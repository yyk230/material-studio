"use client";

import { useRef } from "react";
import type { UploadDto } from "@/lib/api/types";
import styles from "./upload.module.css";

export type SlotState =
  | { kind: "empty" }
  | { kind: "uploading"; fileName: string }
  | { kind: "uploaded"; upload: UploadDto }
  | { kind: "error"; message: string };

interface Props {
  index: number;
  state: SlotState;
  accept: string;
  disabled: boolean;
  onSelect: (file: File) => void;
  onRemove: () => void;
}

export function UploadSlot({ index, state, accept, disabled, onSelect, onRemove }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const n = index + 1;
  const openPicker = () => inputRef.current?.click();

  return (
    <div className={`${styles.slot} ${state.kind === "error" ? styles.slotError : ""}`}>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        className="visuallyHidden"
        tabIndex={-1}
        aria-hidden="true"
        onChange={(e) => {
          const file = e.target.files?.[0];
          // 重置 value，保证再次选择同一个文件也能触发 change
          e.target.value = "";
          if (file) onSelect(file);
        }}
      />

      {state.kind === "uploaded" ? (
        <>
          <img src={state.upload.url} alt={`第 ${n} 张图片：${state.upload.originalName}`} className={styles.preview} />
          <div className={styles.slotActions}>
            <button type="button" className="btn" aria-label={`替换第 ${n} 张图片`} onClick={openPicker} disabled={disabled}>
              替换
            </button>
            <button type="button" className="btn" aria-label={`移除第 ${n} 张图片`} onClick={onRemove} disabled={disabled}>
              移除
            </button>
          </div>
        </>
      ) : (
        <button
          type="button"
          className={styles.picker}
          aria-label={`上传第 ${n} 张图片`}
          onClick={openPicker}
          disabled={disabled || state.kind === "uploading"}
        >
          <span className={styles.pickerIcon} aria-hidden="true">
            ＋
          </span>
          {state.kind === "uploading" ? (
            <span role="status">上传中…</span>
          ) : (
            <span>{state.kind === "error" ? "重新选择图片" : "点击上传图片"}</span>
          )}
          <span className={styles.slotIndex}>第 {n} 张</span>
        </button>
      )}

      {state.kind === "error" && (
        <p className={styles.slotMessage} role="alert">
          {state.message}
        </p>
      )}
    </div>
  );
}
