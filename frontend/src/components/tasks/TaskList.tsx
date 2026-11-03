"use client";

import type { ReactNode } from "react";
import { StateView } from "@/components/ui/StateView";
import type { useTaskFeed } from "@/lib/tasks/useTaskFeed";
import { TaskCard } from "./TaskCard";
import styles from "./tasks.module.css";

interface Props {
  feed: ReturnType<typeof useTaskFeed>;
  showTemplate?: boolean;
  emptyAction?: ReactNode;
}

/** 列表本身的加载/失败/空状态，与单个任务的同步状态分开展示。 */
export function TaskList({ feed, showTemplate = false, emptyAction }: Props) {
  const { tasks, loadState } = feed;

  if (loadState === "loading" || loadState === "idle") {
    return <StateView kind="loading" message="正在加载记录…" />;
  }
  if (loadState === "error" && tasks.length === 0) {
    return <StateView kind="error" message="记录加载失败" onRetry={feed.reload} />;
  }
  if (tasks.length === 0) {
    return <StateView kind="empty" message="还没有制作记录" action={emptyAction} />;
  }

  return (
    <div>
      <ul className={styles.grid}>
        {tasks.map((tracked) => (
          <li key={tracked.task.id}>
            <TaskCard tracked={tracked} showTemplate={showTemplate} onResume={() => feed.resume(tracked.task.id)} />
          </li>
        ))}
      </ul>
      {feed.hasMore && (
        <div className={styles.more}>
          <button type="button" className="btn" onClick={feed.loadMore} disabled={feed.loadingMore}>
            {feed.loadingMore ? "加载中…" : "加载更多"}
          </button>
          {feed.loadMoreError && (
            <span className={styles.moreError} role="alert">
              {feed.loadMoreError}
            </span>
          )}
        </div>
      )}
    </div>
  );
}
