import { jsonBody, request } from "./client";
import type { TaskDto, TaskPage } from "./types";

export const TASK_PAGE_SIZE = 20;

export function createTask(templateId: string, uploadIds: string[]): Promise<TaskDto> {
  return request<TaskDto>("/api/tasks", { method: "POST", body: jsonBody({ templateId, uploadIds }) });
}

export function fetchTask(id: string, signal?: AbortSignal): Promise<TaskDto> {
  return request<TaskDto>(`/api/tasks/${encodeURIComponent(id)}`, { signal });
}

export function fetchTasks(
  params: { templateId?: string; page: number; size?: number },
  signal?: AbortSignal,
): Promise<TaskPage> {
  const query = new URLSearchParams({ page: String(params.page), size: String(params.size ?? TASK_PAGE_SIZE) });
  if (params.templateId) query.set("templateId", params.templateId);
  return request<TaskPage>(`/api/tasks?${query.toString()}`, { signal });
}
