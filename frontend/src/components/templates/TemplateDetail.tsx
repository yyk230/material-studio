"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { TaskList } from "@/components/tasks/TaskList";
import { StateView } from "@/components/ui/StateView";
import { UploadPanel } from "@/components/upload/UploadPanel";
import { ApiError, isAbortError } from "@/lib/api/client";
import { fetchTemplate } from "@/lib/api/templates";
import type { TemplateDto } from "@/lib/api/types";
import { useAuth } from "@/lib/auth/AuthProvider";
import { formatMb } from "@/lib/format";
import { mimeLabel } from "@/lib/labels";
import { useTaskFeed } from "@/lib/tasks/useTaskFeed";
import styles from "./templates.module.css";

type Load =
  | { state: "loading" }
  | { state: "ready"; template: TemplateDto }
  | { state: "notFound" }
  | { state: "error"; message: string };

export function TemplateDetail({ templateId }: { templateId: string }) {
  const [load, setLoad] = useState<Load>({ state: "loading" });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoad({ state: "loading" });
    fetchTemplate(templateId, controller.signal)
      .then((template) => setLoad({ state: "ready", template }))
      .catch((error: unknown) => {
        if (isAbortError(error)) return;
        if (error instanceof ApiError && error.kind === "notFound") setLoad({ state: "notFound" });
        else setLoad({ state: "error", message: error instanceof Error ? error.message : "加载失败" });
      });
    return () => controller.abort();
  }, [templateId, attempt]);

  if (load.state === "loading") {
    return (
      <div className={styles.detail} aria-busy="true">
        <div className={`${styles.skeleton} ${styles.detailSkeleton}`} />
        <div className={`${styles.skeleton} ${styles.detailSkeleton}`} />
      </div>
    );
  }
  if (load.state === "notFound") {
    return (
      <StateView
        kind="empty"
        message="模板不存在或已下线"
        action={
          <Link href="/" className="btn">
            返回模板列表
          </Link>
        }
      />
    );
  }
  if (load.state === "error") {
    return <StateView kind="error" message={load.message} onRetry={() => setAttempt((n) => n + 1)} />;
  }
  return <DetailContent template={load.template} />;
}

function DetailContent({ template }: { template: TemplateDto }) {
  const { status, user, refresh } = useAuth();
  const pathname = usePathname();
  const feed = useTaskFeed({ templateId: template.id });
  const loginHref = `/login?next=${encodeURIComponent(pathname)}`;
  const requirement = [
    `需要 ${template.requiredImageCount} 张图片`,
    `支持 ${template.acceptedMimeTypes.map(mimeLabel).join("/")}`,
    `单张不超过 ${formatMb(template.maxFileSizeBytes)} MB`,
  ].join(" · ");

  return (
    <div className={styles.detail}>
      <section className={styles.left} aria-labelledby="template-title">
        <Link href="/" className={styles.back}>
          ‹ 返回
        </Link>
        <h1 id="template-title" className={styles.detailTitle}>
          {template.title}
        </h1>
        <img src={template.coverUrl} alt={`${template.title} 示例封面`} className={styles.detailCover} />
        <p className={styles.description}>{template.description}</p>
        <p className={styles.requirement}>{requirement}</p>

        {status === "authenticated" && user && (
          <UploadPanel key={`${template.id}:${user.id}`} template={template} onCreated={feed.add} />
        )}
        {status === "anonymous" && (
          <Link href={loginHref} className={`btn btnPrimary ${styles.fullWidth}`}>
            登录后开始制作
          </Link>
        )}
        {status === "loading" && <StateView kind="loading" />}
        {status === "error" && <StateView kind="error" message="无法连接服务器" onRetry={refresh} />}
      </section>

      <section className={styles.right} aria-labelledby="history-title">
        <div className={styles.rightHead}>
          <h2 id="history-title">制作记录</h2>
          {status === "authenticated" && (
            <button type="button" className="btn btnGhost" aria-label="刷新记录" onClick={feed.reload}>
              ↻
            </button>
          )}
        </div>
        {status === "authenticated" && <TaskList feed={feed} />}
        {status === "anonymous" && (
          <StateView
            kind="empty"
            message="登录后查看制作记录"
            action={
              <Link href={loginHref} className="btn">
                去登录
              </Link>
            }
          />
        )}
        {status === "loading" && <StateView kind="loading" />}
        {status === "error" && <StateView kind="error" message="无法连接服务器" onRetry={refresh} />}
      </section>
    </div>
  );
}
