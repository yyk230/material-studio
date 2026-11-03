import type { TaskStage, TaskStatus } from "@/lib/api/types";

export const STATUS_LABELS: Record<TaskStatus, string> = {
  PENDING: "等待处理",
  PROCESSING: "处理中",
  SUCCEEDED: "已完成",
  FAILED: "失败",
};

export const STAGES: readonly TaskStage[] = ["MATERIAL_CHECK", "GENERATION", "RESULT_ARRANGE"];

export const STAGE_LABELS: Record<TaskStage, string> = {
  MATERIAL_CHECK: "素材检查",
  GENERATION: "模拟生成",
  RESULT_ARRANGE: "结果整理",
};

const MIME_LABELS: Record<string, string> = {
  "image/jpeg": "JPG",
  "image/png": "PNG",
  "image/webp": "WEBP",
};

export function mimeLabel(mime: string): string {
  return MIME_LABELS[mime] ?? mime;
}
