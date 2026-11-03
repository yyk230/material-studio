"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import type { UserDto } from "@/lib/api/types";
import { useAuth } from "@/lib/auth/AuthProvider";
import { firstChar, formatDateTime } from "@/lib/format";
import styles from "./AccountDialog.module.css";

interface Props {
  open: boolean;
  user: UserDto;
  onClose: () => void;
}

export function AccountDialog({ open, user, onClose }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const { logout } = useAuth();
  const router = useRouter();
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  async function handleLogout() {
    setLoggingOut(true);
    await logout();
    setLoggingOut(false);
    onClose();
    router.replace("/");
  }

  return (
    <dialog
      ref={dialogRef}
      className={styles.dialog}
      aria-labelledby="account-dialog-title"
      onClose={onClose}
      onClick={(e) => {
        if (e.target === dialogRef.current) onClose();
      }}
    >
      <div className={styles.head}>
        <h2 id="account-dialog-title">账户设置</h2>
        <button type="button" className={styles.close} aria-label="关闭" onClick={onClose}>
          ×
        </button>
      </div>
      <section className={styles.card}>
        <div className={styles.profile}>
          <span className={styles.avatar} aria-hidden="true">
            {firstChar(user.displayName)}
          </span>
          <div>
            <div className={styles.name}>{user.displayName}</div>
            <div className={styles.muted}>用户 ID：{user.id}</div>
          </div>
        </div>
        <dl className={styles.fields}>
          <div>
            <dt>邮箱</dt>
            <dd>{user.email}</dd>
          </div>
          <div>
            <dt>注册时间</dt>
            <dd>{formatDateTime(user.createdAt)}</dd>
          </div>
        </dl>
      </section>
      <button type="button" className={`btn ${styles.logout}`} onClick={handleLogout} disabled={loggingOut}>
        {loggingOut ? "正在退出…" : "退出登录"}
      </button>
    </dialog>
  );
}
