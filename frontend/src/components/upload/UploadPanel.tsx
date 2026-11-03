"use client";

import { useState } from "react";
import { ApiError } from "@/lib/api/client";
import { createTask } from "@/lib/api/tasks";
import type { TaskDto, TemplateDto } from "@/lib/api/types";
import { uploadImage } from "@/lib/api/uploads";
import { useAuth } from "@/lib/auth/AuthProvider";
import { formatMb } from "@/lib/format";
import { mimeLabel } from "@/lib/labels";
import { UploadSlot, type SlotState } from "./UploadSlot";
import styles from "./upload.module.css";

interface Props {
  template: TemplateDto;
  onCreated: (task: TaskDto) => void;
}

export function validateFile(file: File, template: TemplateDto): string | null {
  // 部分系统识别不出 webp 等类型（file.type 为空），交给服务端按文件头判断
  if (file.type && !template.acceptedMimeTypes.includes(file.type)) {
    return `仅支持 ${template.acceptedMimeTypes.map(mimeLabel).join("/")} 格式的图片`;
  }
  if (file.size > template.maxFileSizeBytes) {
    return `图片不能超过 ${formatMb(template.maxFileSizeBytes)} MB`;
  }
  return null;
}

export function UploadPanel({ template, onCreated }: Props) {
  const { handleUnauthorized } = useAuth();
  const [slots, setSlots] = useState<SlotState[]>(() =>
    Array.from({ length: template.requiredImageCount }, () => ({ kind: "empty" }) as SlotState),
  );
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const setSlot = (index: number, state: SlotState) =>
    setSlots((prev) => prev.map((s, i) => (i === index ? state : s)));

  async function handleSelect(index: number, file: File) {
    const invalid = validateFile(file, template);
    if (invalid) {
      setSlot(index, { kind: "error", message: invalid });
      return;
    }
    setSlot(index, { kind: "uploading", fileName: file.name });
    try {
      const upload = await uploadImage(template.id, file);
      setSlot(index, { kind: "uploaded", upload });
    } catch (error) {
      if (error instanceof ApiError && error.kind === "unauthorized") handleUnauthorized();
      const message =
        error instanceof ApiError && error.kind === "network" ? "网络异常，上传失败" : errorMessage(error);
      setSlot(index, { kind: "error", message });
    }
  }

  async function handleSubmit() {
    const uploadIds = slots.flatMap((s) => (s.kind === "uploaded" ? [s.upload.id] : []));
    if (uploadIds.length !== template.requiredImageCount || submitting) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const task = await createTask(template.id, uploadIds);
      onCreated(task);
    } catch (error) {
      if (error instanceof ApiError && error.kind === "unauthorized") handleUnauthorized();
      setSubmitError(errorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  const uploading = slots.some((s) => s.kind === "uploading");
  const missing = slots.filter((s) => s.kind !== "uploaded").length;
  const ready = missing === 0 && !submitting;
  const hint = submitting ? null : uploading ? "图片上传中…" : missing > 0 ? `还需上传 ${missing} 张图片` : null;

  return (
    <div className={styles.panel}>
      <h2 className={styles.panelTitle}>上传图片</h2>
      <div className={styles.slots}>
        {slots.map((slot, index) => (
          <UploadSlot
            key={index}
            index={index}
            state={slot}
            accept={template.acceptedMimeTypes.join(",")}
            disabled={submitting}
            onSelect={(file) => void handleSelect(index, file)}
            onRemove={() => setSlot(index, { kind: "empty" })}
          />
        ))}
      </div>
      <button
        type="button"
        className={`btn btnPrimary ${styles.submit}`}
        disabled={!ready}
        aria-describedby="submit-hint"
        onClick={() => void handleSubmit()}
      >
        {submitting ? "提交中…" : "立即制作"}
      </button>
      <p id="submit-hint" className={styles.hint} aria-live="polite">
        {hint}
      </p>
      {submitError && (
        <p className={styles.submitError} role="alert">
          {submitError}
        </p>
      )}
    </div>
  );
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "操作失败，请稍后重试";
}
