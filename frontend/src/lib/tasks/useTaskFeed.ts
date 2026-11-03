"use client";

import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";
import { fetchTask, fetchTasks } from "@/lib/api/tasks";
import type { TaskDto } from "@/lib/api/types";
import { useAuth } from "@/lib/auth/AuthProvider";
import { TaskTracker, type TrackedTask } from "./taskTracker";

export type FeedLoadState = "idle" | "loading" | "ready" | "error";

const EMPTY: readonly TrackedTask[] = [];
const noopSubscribe = () => () => {};
const emptySnapshot = () => EMPTY;

/**
 * 页面级任务列表：加载当前用户（可按模板过滤）的任务并跟踪未完成任务。
 * 用户或模板变化、离开页面时销毁跟踪器，停止全部轮询；已销毁跟踪器对应的列表响应一律丢弃。
 */
export function useTaskFeed({ templateId }: { templateId?: string } = {}) {
  const { user, handleUnauthorized } = useAuth();
  const userId = user?.id ?? null;

  const [tracker, setTracker] = useState<TaskTracker | null>(null);
  const [loadState, setLoadState] = useState<FeedLoadState>("idle");
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [loadMoreError, setLoadMoreError] = useState<string | null>(null);
  const pageRef = useRef(0);

  /** 拉取一页并合并进跟踪器；跟踪器已销毁时返回 false 且不改任何状态。 */
  const loadPage = useCallback(
    async (target: TaskTracker, page: number) => {
      const result = await fetchTasks({ templateId, page });
      if (target.isDisposed()) return false;
      target.upsert(result.items);
      setHasMore(result.hasMore);
      pageRef.current = page;
      return true;
    },
    [templateId],
  );

  const handleLoadError = useCallback(
    (target: TaskTracker, error: unknown, onError: () => void) => {
      if (target.isDisposed()) return;
      if (isUnauthorized(error)) handleUnauthorized();
      onError();
    },
    [handleUnauthorized],
  );

  useEffect(() => {
    if (userId === null) {
      setTracker(null);
      setLoadState("idle");
      setHasMore(false);
      return;
    }
    const next = new TaskTracker({
      fetchTask: (id, signal) => fetchTask(id, signal),
      onUnauthorized: handleUnauthorized,
    });
    setTracker(next);
    setLoadState("loading");
    setLoadMoreError(null);
    pageRef.current = 0;
    loadPage(next, 0)
      .then((applied) => {
        if (applied) setLoadState("ready");
      })
      .catch((error: unknown) => handleLoadError(next, error, () => setLoadState("error")));
    return () => next.dispose();
  }, [userId, loadPage, handleLoadError, handleUnauthorized]);

  const tasks = useSyncExternalStore(
    tracker?.subscribe ?? noopSubscribe,
    tracker?.getSnapshot ?? emptySnapshot,
    emptySnapshot,
  );

  /** 重新拉取第一页并合并（不销毁跟踪器）；已有数据时失败不清空列表。 */
  const reload = useCallback(() => {
    if (!tracker) return;
    const hadData = tracker.getSnapshot().length > 0;
    if (!hadData) setLoadState("loading");
    loadPage(tracker, 0)
      .then((applied) => {
        if (applied) setLoadState("ready");
      })
      .catch((error: unknown) =>
        handleLoadError(tracker, error, () => {
          if (!hadData) setLoadState("error");
        }),
      );
  }, [tracker, loadPage, handleLoadError]);

  const loadMore = useCallback(() => {
    if (!tracker || loadingMore) return;
    setLoadingMore(true);
    setLoadMoreError(null);
    loadPage(tracker, pageRef.current + 1)
      .catch((error: unknown) =>
        handleLoadError(tracker, error, () => setLoadMoreError(error instanceof Error ? error.message : "加载失败")),
      )
      .finally(() => {
        if (!tracker.isDisposed()) setLoadingMore(false);
      });
  }, [tracker, loadingMore, loadPage, handleLoadError]);

  const add = useCallback((task: TaskDto) => tracker?.upsert([task]), [tracker]);
  const resume = useCallback((id: string) => tracker?.resume(id), [tracker]);

  return { tasks, loadState, hasMore, loadingMore, loadMoreError, reload, loadMore, add, resume };
}

function isUnauthorized(error: unknown): boolean {
  return typeof error === "object" && error !== null && (error as { kind?: string }).kind === "unauthorized";
}
