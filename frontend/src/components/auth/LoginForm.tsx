"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/lib/auth/AuthProvider";
import { safeNextPath } from "@/lib/format";
import styles from "./LoginForm.module.css";

type Mode = "login" | "register";

export function LoginForm() {
  const { status, login, register } = useAuth();
  const router = useRouter();
  const next = safeNextPath(useSearchParams().get("next"));

  const [mode, setMode] = useState<Mode>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (status === "authenticated") router.replace(next);
  }, [status, next, router]);

  function validate(): string | null {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) return "请输入正确的邮箱地址";
    if (mode === "register" && (password.length < 8 || password.length > 64)) return "密码长度需为 8~64 位";
    if (mode === "login" && password.length === 0) return "请输入密码";
    if (mode === "register" && displayName.trim().length > 32) return "昵称不能超过 32 个字符";
    return null;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const invalid = validate();
    if (invalid) {
      setError(invalid);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      if (mode === "login") await login(email.trim(), password);
      else await register(email.trim(), password, displayName.trim());
      router.replace(next);
    } catch (err) {
      setError(err instanceof Error ? err.message : "操作失败，请稍后重试");
    } finally {
      setSubmitting(false);
    }
  }

  function switchMode(target: Mode) {
    setMode(target);
    setError(null);
  }

  return (
    <div className={styles.wrap}>
      <form className={styles.form} onSubmit={handleSubmit} noValidate>
        <div className={styles.tabs} role="group" aria-label="登录或注册">
          <button type="button" aria-pressed={mode === "login"} onClick={() => switchMode("login")}>
            登录
          </button>
          <button type="button" aria-pressed={mode === "register"} onClick={() => switchMode("register")}>
            注册
          </button>
        </div>

        <label className={styles.field}>
          <span>邮箱</span>
          <input
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </label>
        <label className={styles.field}>
          <span>密码</span>
          <input
            type="password"
            autoComplete={mode === "login" ? "current-password" : "new-password"}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        {mode === "register" && (
          <label className={styles.field}>
            <span>昵称（可选）</span>
            <input
              type="text"
              autoComplete="nickname"
              maxLength={32}
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
          </label>
        )}

        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}

        <button type="submit" className="btn btnPrimary" disabled={submitting}>
          {submitting ? "提交中…" : mode === "login" ? "登录" : "注册并登录"}
        </button>

        <p className={styles.demo}>
          演示账号：demo1@example.com / demo2@example.com，密码均为 Demo@123456
        </p>
      </form>
    </div>
  );
}
