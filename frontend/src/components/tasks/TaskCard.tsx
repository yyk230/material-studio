"use client";

import Link from "next/link";
import { useState } from "react";
import { formatDateTime } from "@/lib/format";
import { STAGE_LABELS } from "@/lib/labels";
import type { TrackedTask } from "@/lib/tasks/taskTracker";
import { ImagePreviewDialog } from "./ImagePreviewDialog";
import { StageProgress } from "./StageProgress";
import { StatusBadge } from "./StatusBadge";
import styles from "./tasks.module.css";

interface Props {
  tracked: TrackedTask;
  showTemplate: boolean;
  onResume: () => void;
}

export function TaskCard({ tracked, showTemplate, onResume }: Props) {
  const { task, sync } = tracked;
  const [previewOpen, setPreviewOpen] = useState(false);

  return (
    <article className={styles.card} aria-label={`${task.templateTitle} 任务`}>
      <div className={styles.media}>
        {task.status === "SUCCEEDED" && task.resultUrl ? (
          <button
            type="button"
            className={styles.resultButton}
            aria-label="放大查看结果图"
            onClick={() => setPreviewOpen(true)}
          >
            <img src={task.resultUrl} alt={`${task.templateTitle} 制作结果`} className={styles.result} />
          </button>
        ) : (
          <div className={`${styles.placeholder} ${task.status === "FAILED" ? styles.placeholderFailed : ""}`}>
            {task.status === "PENDING" && <span>等待处理…</span>}
            {task.status === "PROCESSING" && (
              <span>{task.currentStage ? `${STAGE_LABELS[task.currentStage]}中…` : "处理中…"}</span>
            )}
            {task.status === "FAILED" && <span>{task.errorMessage ?? "制作失败"}</span>}
          </div>
        )}
      </div>

      <div className={styles.body}>
        <div className={styles.row}>
          <StatusBadge status={task.status} />
          <time className={styles.time} dateTime={task.createdAt}>
            {formatDateTime(task.createdAt)}
          </time>
        </div>
        {showTemplate && (
          <Link href={`/templates/${encodeURIComponent(task.templateId)}`} className={styles.templateLink}>
            {task.templateTitle}
          </Link>
        )}
        <StageProgress task={task} />
        {task.status === "FAILED" && task.errorMessage && (
          <p className={styles.failReason}>
            失败原因{task.currentStage ? `（${STAGE_LABELS[task.currentStage]}）` : ""}：{task.errorMessage}
          </p>
        )}
        {sync === "retrying" && (
          <p className={styles.syncWarning} role="status">
            状态同步失败，正在重试…
          </p>
        )}
        {sync === "paused" && (
          <div className={styles.syncWarning} role="alert">
            <span>状态同步失败</span>
            <button type="button" className="btn" onClick={onResume}>
              重新查询
            </button>
          </div>
        )}
        {sync === "gone" && (
          <p className={styles.syncWarning} role="alert">
            任务不存在或无权访问
          </p>
        )}
      </div>

      {task.resultUrl && (
        <ImagePreviewDialog
          open={previewOpen}
          src={task.resultUrl}
          alt={`${task.templateTitle} 制作结果`}
          onClose={() => setPreviewOpen(false)}
        />
      )}
    </article>
  );
}
