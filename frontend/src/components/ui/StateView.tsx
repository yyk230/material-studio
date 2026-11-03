import type { ReactNode } from "react";
import styles from "./StateView.module.css";

type Props =
  | { kind: "loading"; message?: string }
  | { kind: "empty"; message: string; action?: ReactNode }
  | { kind: "error"; message: string; onRetry?: () => void; action?: ReactNode };

/** 统一的加载中 / 空数据 / 请求失败展示。 */
export function StateView(props: Props) {
  if (props.kind === "loading") {
    return (
      <div className={styles.box} role="status" aria-live="polite">
        <span className={styles.spinner} aria-hidden="true" />
        <span>{props.message ?? "加载中…"}</span>
      </div>
    );
  }
  if (props.kind === "empty") {
    return (
      <div className={styles.box}>
        <span className={styles.muted}>{props.message}</span>
        {props.action}
      </div>
    );
  }
  return (
    <div className={`${styles.box} ${styles.error}`} role="alert">
      <span>{props.message}</span>
      <div className={styles.actions}>
        {props.onRetry && (
          <button type="button" className="btn" onClick={props.onRetry}>
            重试
          </button>
        )}
        {props.action}
      </div>
    </div>
  );
}
