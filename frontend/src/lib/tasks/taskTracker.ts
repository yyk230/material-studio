import { ApiError, isAbortError } from "@/lib/api/client";
import { isTerminal, type TaskDto } from "@/lib/api/types";

/**
 * ok：最近一次查询正常；retrying：查询失败，稍后自动重试；
 * paused：连续失败已停止自动重试，等待用户“重新查询”；gone：任务不存在或无权访问。
 * 这里的同步状态只描述“查询请求”是否正常，与任务本身的成功/失败无关。
 */
export type SyncState = "ok" | "retrying" | "paused" | "gone";

export interface TrackedTask {
  task: TaskDto;
  sync: SyncState;
  failures: number;
}

export interface Timers {
  setTimeout: (fn: () => void, ms: number) => unknown;
  clearTimeout: (handle: unknown) => void;
}

export interface TaskTrackerOptions {
  fetchTask: (id: string, signal: AbortSignal) => Promise<TaskDto>;
  onUnauthorized: () => void;
  pollMs?: number;
  retryMs?: number;
  maxFailures?: number;
  timers?: Timers;
}

interface Entry extends TrackedTask {
  timer: unknown | null;
  controller: AbortController | null;
}

const defaultTimers: Timers = {
  setTimeout: (fn, ms) => globalThis.setTimeout(fn, ms),
  clearTimeout: (handle) => globalThis.clearTimeout(handle as ReturnType<typeof setTimeout>),
};

/**
 * 跟踪一组任务的状态：每个未完成任务各自独立轮询（互不影响），终态即停止；
 * 查询失败只改变同步状态、保留已知任务数据，绝不把任务标记为失败。
 */
export class TaskTracker {
  private readonly entries = new Map<string, Entry>();
  private readonly listeners = new Set<() => void>();
  private snapshot: readonly TrackedTask[] = [];
  private disposed = false;

  private readonly fetchTask: TaskTrackerOptions["fetchTask"];
  private readonly onUnauthorized: () => void;
  private readonly pollMs: number;
  private readonly retryMs: number;
  private readonly maxFailures: number;
  private readonly timers: Timers;

  constructor(options: TaskTrackerOptions) {
    this.fetchTask = options.fetchTask;
    this.onUnauthorized = options.onUnauthorized;
    this.pollMs = options.pollMs ?? 1500;
    this.retryMs = options.retryMs ?? 5000;
    this.maxFailures = options.maxFailures ?? 3;
    this.timers = options.timers ?? defaultTimers;
  }

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  getSnapshot = (): readonly TrackedTask[] => this.snapshot;

  upsert(tasks: TaskDto[]): void {
    if (this.disposed || tasks.length === 0) return;
    for (const task of tasks) {
      const existing = this.entries.get(task.id);
      if (existing) {
        // 列表接口返回的旧数据不能覆盖已经拿到的终态
        if (isTerminal(existing.task.status) && !isTerminal(task.status)) continue;
        existing.task = task;
      } else {
        this.entries.set(task.id, { task, sync: "ok", failures: 0, timer: null, controller: null });
      }
      const entry = this.entries.get(task.id)!;
      if (isTerminal(task.status)) {
        this.stop(entry);
      } else if (!entry.timer && !entry.controller && entry.sync !== "paused" && entry.sync !== "gone") {
        this.schedule(task.id, this.pollMs);
      }
    }
    this.emit();
  }

  /** 用户点击“重新查询”：清零失败次数并立即查询一次。 */
  resume(id: string): void {
    const entry = this.entries.get(id);
    if (this.disposed || !entry || isTerminal(entry.task.status)) return;
    this.stop(entry);
    entry.failures = 0;
    entry.sync = "ok";
    this.emit();
    void this.poll(id);
  }

  isDisposed(): boolean {
    return this.disposed;
  }

  dispose(): void {
    this.disposed = true;
    for (const entry of this.entries.values()) this.stop(entry);
    this.listeners.clear();
  }

  /** 供测试断言：当前是否存在计时器或进行中的请求。 */
  activeCount(): number {
    let count = 0;
    for (const entry of this.entries.values()) {
      if (entry.timer || entry.controller) count++;
    }
    return count;
  }

  private schedule(id: string, delay: number): void {
    const entry = this.entries.get(id);
    if (!entry) return;
    entry.timer = this.timers.setTimeout(() => {
      entry.timer = null;
      void this.poll(id);
    }, delay);
  }

  private async poll(id: string): Promise<void> {
    const entry = this.entries.get(id);
    if (this.disposed || !entry) return;
    const controller = new AbortController();
    entry.controller = controller;
    try {
      const task = await this.fetchTask(id, controller.signal);
      if (this.disposed || entry.controller !== controller) return;
      entry.controller = null;
      entry.task = task;
      entry.sync = "ok";
      entry.failures = 0;
      if (!isTerminal(task.status)) this.schedule(id, this.pollMs);
    } catch (error) {
      if (this.disposed || entry.controller !== controller || isAbortError(error)) return;
      entry.controller = null;
      if (error instanceof ApiError && error.kind === "notFound") {
        entry.sync = "gone";
      } else if (error instanceof ApiError && error.kind === "unauthorized") {
        this.onUnauthorized();
      } else {
        entry.failures += 1;
        if (entry.failures < this.maxFailures) {
          entry.sync = "retrying";
          this.schedule(id, this.retryMs);
        } else {
          entry.sync = "paused";
        }
      }
    }
    this.emit();
  }

  private stop(entry: Entry): void {
    if (entry.timer) {
      this.timers.clearTimeout(entry.timer);
      entry.timer = null;
    }
    if (entry.controller) {
      entry.controller.abort();
      entry.controller = null;
    }
  }

  private emit(): void {
    if (this.disposed) return;
    this.snapshot = [...this.entries.values()]
      .map(({ task, sync, failures }) => ({ task, sync, failures }))
      .sort((a, b) => (a.task.createdAt < b.task.createdAt ? 1 : a.task.createdAt > b.task.createdAt ? -1 : 0));
    for (const listener of this.listeners) listener();
  }
}
