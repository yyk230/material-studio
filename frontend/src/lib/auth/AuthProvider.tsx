"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import * as authApi from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";
import type { UserDto } from "@/lib/api/types";

export type AuthStatus = "loading" | "authenticated" | "anonymous" | "error";

interface AuthContextValue {
  status: AuthStatus;
  user: UserDto | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => void;
  handleUnauthorized: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<UserDto | null>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let cancelled = false;
    authApi
      .fetchMe()
      .then((me) => {
        if (cancelled) return;
        setUser(me);
        setStatus("authenticated");
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        setUser(null);
        // 后端暂时不可用不等于未登录，避免把已登录用户踢到登录页
        setStatus(error instanceof ApiError && error.kind === "unauthorized" ? "anonymous" : "error");
      });
    return () => {
      cancelled = true;
    };
  }, [attempt]);

  const refresh = useCallback(() => {
    setStatus("loading");
    setAttempt((n) => n + 1);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const me = await authApi.login(email, password);
    setUser(me);
    setStatus("authenticated");
  }, []);

  const register = useCallback(async (email: string, password: string, displayName: string) => {
    const me = await authApi.register(email, password, displayName);
    setUser(me);
    setStatus("authenticated");
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // 无论请求是否成功都在本地退出
    }
    setUser(null);
    setStatus("anonymous");
  }, []);

  const handleUnauthorized = useCallback(() => {
    setUser(null);
    setStatus("anonymous");
  }, []);

  const value = useMemo(
    () => ({ status, user, login, register, logout, refresh, handleUnauthorized }),
    [status, user, login, register, logout, refresh, handleUnauthorized],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
