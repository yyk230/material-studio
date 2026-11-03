"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { TaskList } from "@/components/tasks/TaskList";
import { StateView } from "@/components/ui/StateView";
import { useAuth } from "@/lib/auth/AuthProvider";
import { useTaskFeed } from "@/lib/tasks/useTaskFeed";
import styles from "./history.module.css";

export default function HistoryPage() {
  const { status, refresh } = useAuth();
  const router = useRouter();
  const feed = useTaskFeed();

  useEffect(() => {
    if (status === "anonymous") router.replace("/login?next=/history");
  }, [status, router]);

  return (
    <div>
      <div className={styles.head}>
        <h1>我的记录</h1>
        {status === "authenticated" && (
          <button type="button" className="btn btnGhost" aria-label="刷新记录" onClick={feed.reload}>
            ↻ 刷新
          </button>
        )}
      </div>
      {status === "authenticated" && (
        <TaskList
          feed={feed}
          showTemplate
          emptyAction={
            <Link href="/" className="btn">
              去选模板
            </Link>
          }
        />
      )}
      {(status === "loading" || status === "anonymous") && <StateView kind="loading" />}
      {status === "error" && <StateView kind="error" message="无法连接服务器" onRetry={refresh} />}
    </div>
  );
}
