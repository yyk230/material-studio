import { request } from "./client";
import type { UploadDto } from "./types";

export const UPLOAD_TIMEOUT_MS = 30_000;

export function uploadImage(templateId: string, file: File): Promise<UploadDto> {
  const form = new FormData();
  form.append("templateId", templateId);
  form.append("file", file);
  return request<UploadDto>("/api/uploads", { method: "POST", body: form, timeoutMs: UPLOAD_TIMEOUT_MS });
}
