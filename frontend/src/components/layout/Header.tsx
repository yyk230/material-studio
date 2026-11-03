"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useRef, useState } from "react";
import { useAuth } from "@/lib/auth/AuthProvider";
import { firstChar } from "@/lib/format";
import { AccountDialog } from "./AccountDialog";
import styles from "./Header.module.css";

export function Header() {
  const { status, user, refresh } = useAuth();
  const pathname = usePathname();
  const [accountOpen, setAccountOpen] = useState(false);
  const avatarRef = useRef<HTMLButtonElement>(null);

  const navItems = [
    { href: "/", label: "模板", active: pathname === "/" || pathname.startsWith("/templates") },
    { href: "/history", label: "我的记录", active: pathname.startsWith("/history") },
  ];

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <Link href="/" className={styles.logo}>
          <span className={styles.logoMark} aria-hidden="true" />
          素材工坊
        </Link>
        <nav className={styles.nav} aria-label="主导航">
          {navItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={`${styles.navLink} ${item.active ? styles.navActive : ""}`}
              aria-current={item.active ? "page" : undefined}
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <div className={styles.right}>
          {status === "error" && (
            <span className={styles.offline} role="alert">
              无法连接服务器
              <button type="button" className="btn btnGhost" onClick={refresh}>
                重试
              </button>
            </span>
          )}
          {status === "authenticated" && user && (
            <button
              ref={avatarRef}
              type="button"
              className={styles.avatar}
              aria-label="账户设置"
              aria-haspopup="dialog"
              onClick={() => setAccountOpen(true)}
            >
              {firstChar(user.displayName)}
            </button>
          )}
          {status === "anonymous" && (
            <Link href={`/login?next=${encodeURIComponent(pathname)}`} className="btn btnPrimary">
              登录
            </Link>
          )}
        </div>
      </div>
      {user && (
        <AccountDialog
          open={accountOpen}
          user={user}
          onClose={() => {
            setAccountOpen(false);
            avatarRef.current?.focus();
          }}
        />
      )}
    </header>
  );
}