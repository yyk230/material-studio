export type ApiErrorKind = "network" | "server" | "unauthorized" | "notFound" | "validation";

export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status: number;
  readonly code: string | null;

  constructor(kind: ApiErrorKind, message: string, status = 0, code: string | null = null) {
    super(message);
    this.name = "ApiError";
    this.kind = kind;
    this.status = status;
    this.code = code;
  }
}

export interface RequestOptions extends Omit<RequestInit, "signal"> {
  signal?: AbortSignal;
  timeoutMs?: number;
}

export const DEFAULT_TIMEOUT_MS = 10_000;

const DEFAULT_MESSAGES: Record<ApiErrorKind, string> = {
  network: "网络异常，请检查连接",
  server: "服务暂时不可用，请稍后重试",
  unauthorized: "登录已失效，请重新登录",
  notFound: "请求的内容不存在",
  validation: "请求参数不合法",
};

export function classifyStatus(status: number): ApiErrorKind {
  if (status === 401) return "unauthorized";
  if (status === 404) return "notFound";
  if (status === 400 || status === 409 || status === 413) return "validation";
  return "server";
}

export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}

/**
 * fetch 封装：同源 Cookie、超时、错误分类。
 * 外部 signal 主动取消时原样抛出 AbortError，调用方据此静默忽略；超时则作为 network 错误抛出。
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { signal: outerSignal, timeoutMs = DEFAULT_TIMEOUT_MS, headers, body, ...init } = options;

  if (outerSignal?.aborted) {
    throw new DOMException("Aborted", "AbortError");
  }

  const controller = new AbortController();
  let timedOut = false;
  const timer = setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, timeoutMs);
  const onOuterAbort = () => controller.abort();
  outerSignal?.addEventListener("abort", onOuterAbort);

  const finalHeaders = new Headers(headers);
  if (body !== undefined && body !== null && !(body instanceof FormData) && !finalHeaders.has("Content-Type")) {
    finalHeaders.set("Content-Type", "application/json");
  }

  let response: Response;
  try {
    response = await fetch(path, {
      ...init,
      body,
      headers: finalHeaders,
      credentials: "same-origin",
      signal: controller.signal,
    });
  } catch {
    if (timedOut) {
      throw new ApiError("network", "请求超时");
    }
    if (outerSignal?.aborted) {
      throw new DOMException("Aborted", "AbortError");
    }
    throw new ApiError("network", DEFAULT_MESSAGES.network);
  } finally {
    clearTimeout(timer);
    outerSignal?.removeEventListener("abort", onOuterAbort);
  }

  if (response.ok) {
    if (response.status === 204) {
      return undefined as T;
    }
    return (await response.json()) as T;
  }

  const kind = classifyStatus(response.status);
  let message = DEFAULT_MESSAGES[kind];
  let code: string | null = null;
  try {
    const data = (await response.json()) as { code?: string; message?: string };
    if (data?.message) message = data.message;
    if (data?.code) code = data.code;
  } catch {
    // 非 JSON 错误体（例如代理返回的 502 页面），使用默认文案
  }
  throw new ApiError(kind, message, response.status, code);
}

export function jsonBody(value: unknown): string {
  return JSON.stringify(value);
}
