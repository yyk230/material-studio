export type TaskStatus = "PENDING" | "PROCESSING" | "SUCCEEDED" | "FAILED";
export type TaskStage = "MATERIAL_CHECK" | "GENERATION" | "RESULT_ARRANGE";
export type StepStatus = "RUNNING" | "SUCCEEDED" | "FAILED";

export interface UserDto {
  id: number;
  email: string;
  displayName: string;
  createdAt: string;
}

export interface TemplateDto {
  id: string;
  title: string;
  category: string;
  coverUrl: string;
  description: string;
  requiredImageCount: number;
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
}

export interface UploadDto {
  id: string;
  url: string;
  mimeType: string;
  sizeBytes: number;
  originalName: string;
}

export interface TaskStepDto {
  stage: TaskStage;
  status: StepStatus;
  message: string | null;
  startedAt: string;
  finishedAt: string | null;
}

export interface TaskDto {
  id: string;
  templateId: string;
  templateTitle: string;
  status: TaskStatus;
  currentStage: TaskStage | null;
  errorMessage: string | null;
  resultUrl: string | null;
  steps: TaskStepDto[];
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface TaskPage {
  items: TaskDto[];
  hasMore: boolean;
}

export const TERMINAL_STATUSES: ReadonlySet<TaskStatus> = new Set(["SUCCEEDED", "FAILED"]);

export function isTerminal(status: TaskStatus): boolean {
  return TERMINAL_STATUSES.has(status);
}
