import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import type { TaskDto, TaskStatus } from "@/lib/api/types";
import { TaskTracker } from "./taskTracker";

function task(id: string, status: TaskStatus, extra: Partial<TaskDto> = {}): TaskDto {
  return {
    id,
    templateId: "tpl",
    templateTitle: "模板",
    status,
    currentStage: status === "PENDING" ? null : "GENERATION",
    errorMessage: null,
    resultUrl: null,
    steps: [],
    createdAt: `2026-11-02T10:00:0${id.length % 10}.000Z`,
    startedAt: null,
    finishedAt: null,
    ...extra,
  };
}

/** 按任务 ID 排队的响应：每次查询依次取出一个结果（TaskDto 或要抛出的错误）。 */
function scriptedFetch(script: Record<string, Array<TaskDto | Error>>) {
  const calls: string[] = [];
  const fetchTask = vi.fn(async (id: string) => {
    calls.push(id);
    const next = script[id]?.shift();
    if (!next) throw new Error(`no scripted response for ${id}`);
    if (next instanceof Error) throw next;
    return next;
  });
  return { fetchTask, calls };
}

const find = (tracker: TaskTracker, id: string) => tracker.getSnapshot().find((t) => t.task.id === id)!;

describe("TaskTracker", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("tracks multiple tasks independently and stops polling at terminal states", async () => {
    const { fetchTask, calls } = scriptedFetch({
      a: [task("a", "PROCESSING"), task("a", "SUCCEEDED", { resultUrl: "/api/files/results/a" })],
      bb: [
        task("bb", "PROCESSING"),
        task("bb", "FAILED", { errorMessage: "生成服务暂时不可用（模拟失败）", currentStage: "GENERATION" }),
      ],
    });
    const tracker = new TaskTracker({ fetchTask, onUnauthorized: vi.fn(), pollMs: 1000 });
    tracker.upsert([task("a", "PENDING"), task("bb", "PENDING")]);

    await vi.advanceTimersByTimeAsync(1000);
    expect(find(tracker, "a").task.status).toBe("PROCESSING");
    expect(find(tracker, "bb").task.status).toBe("PROCESSING");

    await vi.advanceTimersByTimeAsync(1000);
    const a = find(tracker, "a");
    const b = find(tracker, "bb");
    expect(a.task.status).toBe("SUCCEEDED");
    expect(a.task.resultUrl).toBe("/api/files/results/a");
    expect(a.task.errorMessage).toBeNull();
    expect(b.task.status).toBe("FAILED");
    expect(b.task.errorMessage).toBe("生成服务暂时不可用（模拟失败）");
    expect(b.task.resultUrl).toBeNull();

    const callCount = calls.length;
    await vi.advanceTimersByTimeAsync(10_000);
    expect(calls.length).toBe(callCount);
    expect(tracker.activeCount()).toBe(0);
    expect(vi.getTimerCount()).toBe(0);
  });

  it("treats query failures as sync errors, never as task failures, and resumes on demand", async () => {
    const networkError = () => new ApiError("network", "网络异常");
    const { fetchTask, calls } = scriptedFetch({
      a: [networkError(), new ApiError("server", "服务暂时不可用", 502), networkError(), task("a", "SUCCEEDED")],
    });
    const tracker = new TaskTracker({ fetchTask, onUnauthorized: vi.fn(), pollMs: 1000, retryMs: 2000, maxFailures: 3 });
    tracker.upsert([task("a", "PROCESSING")]);

    await vi.advanceTimersByTimeAsync(1000);
    expect(find(tracker, "a").sync).toBe("retrying");
    expect(find(tracker, "a").task.status).toBe("PROCESSING");

    await vi.advanceTimersByTimeAsync(2000);
    expect(find(tracker, "a").sync).toBe("retrying");

    await vi.advanceTimersByTimeAsync(2000);
    expect(find(tracker, "a").sync).toBe("paused");
    expect(find(tracker, "a").task.status).toBe("PROCESSING");
    expect(calls).toHaveLength(3);

    await vi.advanceTimersByTimeAsync(30_000);
    expect(calls).toHaveLength(3);
    expect(vi.getTimerCount()).toBe(0);

    tracker.resume("a");
    await vi.advanceTimersByTimeAsync(0);
    expect(find(tracker, "a").task.status).toBe("SUCCEEDED");
    expect(find(tracker, "a").sync).toBe("ok");
    expect(calls).toHaveLength(4);
  });

  it("marks a missing task as gone without affecting others", async () => {
    const { fetchTask } = scriptedFetch({
      a: [new ApiError("notFound", "任务不存在", 404)],
      bb: [task("bb", "SUCCEEDED")],
    });
    const tracker = new TaskTracker({ fetchTask, onUnauthorized: vi.fn(), pollMs: 1000 });
    tracker.upsert([task("a", "PROCESSING"), task("bb", "PROCESSING")]);

    await vi.advanceTimersByTimeAsync(1000);
    expect(find(tracker, "a").sync).toBe("gone");
    expect(find(tracker, "a").task.status).toBe("PROCESSING");
    expect(find(tracker, "bb").task.status).toBe("SUCCEEDED");
    expect(vi.getTimerCount()).toBe(0);
  });

  it("notifies on unauthorized responses", async () => {
    const onUnauthorized = vi.fn();
    const { fetchTask } = scriptedFetch({ a: [new ApiError("unauthorized", "请先登录", 401)] });
    const tracker = new TaskTracker({ fetchTask, onUnauthorized, pollMs: 1000 });
    tracker.upsert([task("a", "PROCESSING")]);

    await vi.advanceTimersByTimeAsync(1000);
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
    expect(vi.getTimerCount()).toBe(0);
  });

  it("does not let stale list data overwrite a terminal state", async () => {
    const { fetchTask } = scriptedFetch({});
    const tracker = new TaskTracker({ fetchTask, onUnauthorized: vi.fn(), pollMs: 1000 });
    tracker.upsert([task("a", "SUCCEEDED", { resultUrl: "/r" })]);
    tracker.upsert([task("a", "PROCESSING")]);

    expect(find(tracker, "a").task.status).toBe("SUCCEEDED");
    expect(tracker.activeCount()).toBe(0);
    expect(vi.getTimerCount()).toBe(0);
    expect(fetchTask).not.toHaveBeenCalled();
  });

  it("ignores responses that arrive after dispose", async () => {
    let resolve!: (value: TaskDto) => void;
    const fetchTask = vi.fn(
      () =>
        new Promise<TaskDto>((r) => {
          resolve = r;
        }),
    );
    const tracker = new TaskTracker({ fetchTask, onUnauthorized: vi.fn(), pollMs: 1000 });
    const listener = vi.fn();
    tracker.upsert([task("a", "PROCESSING")]);
    tracker.subscribe(listener);

    await vi.advanceTimersByTimeAsync(1000);
    expect(fetchTask).toHaveBeenCalledTimes(1);
    const before = tracker.getSnapshot();

    tracker.dispose();
    resolve(task("a", "SUCCEEDED"));
    await vi.advanceTimersByTimeAsync(5000);

    expect(tracker.getSnapshot()).toBe(before);
    expect(listener).not.toHaveBeenCalled();
    expect(vi.getTimerCount()).toBe(0);
  });

  it("aborts the in-flight request on dispose", async () => {
    let signal: AbortSignal | undefined;
    const fetchTask = vi.fn((_id: string, s: AbortSignal) => {
      signal = s;
      return new Promise<TaskDto>(() => {});
    });
    const tracker = new TaskTracker({ fetchTask, onUnauthorized: vi.fn(), pollMs: 1000 });
    tracker.upsert([task("a", "PENDING")]);
    await vi.advanceTimersByTimeAsync(1000);

    tracker.dispose();
    expect(signal?.aborted).toBe(true);
  });

  it("creates no timers when only terminal tasks are tracked", () => {
    const { fetchTask } = scriptedFetch({});
    const tracker = new TaskTracker({ fetchTask, onUnauthorized: vi.fn() });
    tracker.upsert([task("a", "SUCCEEDED"), task("bb", "FAILED")]);

    expect(tracker.getSnapshot()).toHaveLength(2);
    expect(vi.getTimerCount()).toBe(0);
  });
});
