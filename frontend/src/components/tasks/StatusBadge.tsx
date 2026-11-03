import type { TaskStatus } from "@/lib/api/types";
import { STATUS_LABELS } from "@/lib/labels";
import styles from "./tasks.module.css";

export function StatusBadge({ status }: { status: TaskStatus }) {
  return <span className={`${styles.badge} ${styles[`badge_${status}`]}`}>{STATUS_LABELS[status]}</span>;
}
