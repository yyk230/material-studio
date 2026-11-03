import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, isAbortError, request } from "./client";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

async function capture(promise: Promise<unknown>): Promise<unknown> {
  try {
    await promise;
  } catch (error) {
    return error;
  }
  throw new Error("expected rejection");
}

describe("request", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    fetchMock.mockReset();
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it("returns parsed JSON on success and undefined on 204", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, { id: 1 }));
    await expect(request("/api/x")).resolves.toEqual({ id: 1 });

    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(request("/api/x")).resolves.toBeUndefined();
  });

  it.each([
    [401, "unauthorized"],
    [404, "notFound"],
    [400, "validation"],
    [409, "validation"],
    [413, "validation"],
    [500, "server"],
    [502, "server"],
  ])("classifies HTTP %i as %s", async (status, kind) => {
    fetchMock.mockResolvedValueOnce(jsonResponse(status, { code: "X", message: "服务端说明" }));
    const error = (await capture(request("/api/x"))) as ApiError;
    expect(error).toBeInstanceOf(ApiError);
    expect(error.kind).toBe(kind);
    expect(error.status).toBe(status);
    expect(error.message).toBe("服务端说明");
    expect(error.code).toBe("X");
  });

  it("falls back to a default message for non-JSON error bodies", async () => {
    fetchMock.mockResolvedValueOnce(new Response("<html>Bad Gateway</html>", { status: 502 }));
    const error = (await capture(request("/api/x"))) as ApiError;
    expect(error.kind).toBe("server");
    expect(error.message).toBe("服务暂时不可用，请稍后重试");
  });

  it("classifies fetch rejections as network errors", async () => {
    fetchMock.mockRejectedValueOnce(new TypeError("Failed to fetch"));
    const error = (await capture(request("/api/x"))) as ApiError;
    expect(error.kind).toBe("network");
  });

  it("turns a timeout into a network error", async () => {
    vi.useFakeTimers();
    fetchMock.mockImplementationOnce(
      (_input, init) =>
        new Promise((_resolve, reject) => {
          init?.signal?.addEventListener("abort", () => reject(new DOMException("Aborted", "AbortError")));
        }),
    );
    const pending = capture(request("/api/x", { timeoutMs: 1000 }));
    await vi.advanceTimersByTimeAsync(1000);
    const error = (await pending) as ApiError;
    expect(error).toBeInstanceOf(ApiError);
    expect(error.kind).toBe("network");
    expect(error.message).toBe("请求超时");
  });

  it("rethrows AbortError when the caller aborts", async () => {
    fetchMock.mockImplementationOnce(
      (_input, init) =>
        new Promise((_resolve, reject) => {
          init?.signal?.addEventListener("abort", () => reject(new DOMException("Aborted", "AbortError")));
        }),
    );
    const controller = new AbortController();
    const pending = capture(request("/api/x", { signal: controller.signal }));
    controller.abort();
    const error = await pending;
    expect(error).not.toBeInstanceOf(ApiError);
    expect(isAbortError(error)).toBe(true);
  });

  it("sends JSON content type only for non-FormData bodies", async () => {
    fetchMock.mockImplementation(async () => jsonResponse(200, {}));
    await request("/api/x", { method: "POST", body: JSON.stringify({ a: 1 }) });
    await request("/api/x", { method: "POST", body: new FormData() });

    const jsonHeaders = fetchMock.mock.calls[0][1]?.headers as Headers;
    const formHeaders = fetchMock.mock.calls[1][1]?.headers as Headers;
    expect(jsonHeaders.get("Content-Type")).toBe("application/json");
    expect(formHeaders.has("Content-Type")).toBe(false);
    expect(fetchMock.mock.calls[0][1]?.credentials).toBe("same-origin");
  });
});
