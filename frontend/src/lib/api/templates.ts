import { request } from "./client";
import type { TemplateDto } from "./types";

export function fetchCategories(signal?: AbortSignal): Promise<string[]> {
  return request<string[]>("/api/templates/categories", { signal });
}

export function fetchTemplates(category: string | null, signal?: AbortSignal): Promise<TemplateDto[]> {
  const query = category ? `?category=${encodeURIComponent(category)}` : "";
  return request<TemplateDto[]>(`/api/templates${query}`, { signal });
}

export function fetchTemplate(id: string, signal?: AbortSignal): Promise<TemplateDto> {
  return request<TemplateDto>(`/api/templates/${encodeURIComponent(id)}`, { signal });
}
