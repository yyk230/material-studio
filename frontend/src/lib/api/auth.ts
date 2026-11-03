import { jsonBody, request } from "./client";
import type { UserDto } from "./types";

export function fetchMe(): Promise<UserDto> {
  return request<UserDto>("/api/auth/me");
}

export function login(email: string, password: string): Promise<UserDto> {
  return request<UserDto>("/api/auth/login", { method: "POST", body: jsonBody({ email, password }) });
}

export function register(email: string, password: string, displayName: string): Promise<UserDto> {
  return request<UserDto>("/api/auth/register", {
    method: "POST",
    body: jsonBody({ email, password, displayName }),
  });
}

export function logout(): Promise<void> {
  return request<void>("/api/auth/logout", { method: "POST" });
}
