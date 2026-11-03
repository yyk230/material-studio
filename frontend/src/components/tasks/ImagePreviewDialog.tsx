"use client";

import { useEffect, useRef } from "react";
import styles from "./tasks.module.css";

interface Props {
  open: boolean;
  src: string;
  alt: string;
  onClose: () => void;
}

export function ImagePreviewDialog({ open, src, alt, onClose }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={dialogRef}
      className={styles.previewDialog}
      aria-label="结果预览"
      onClose={onClose}
      onClick={(e) => {
        if (e.target === dialogRef.current) onClose();
      }}
    >
      <button type="button" className={styles.previewClose} aria-label="关闭预览" onClick={onClose}>
        ×
      </button>
      {open && <img src={src} alt={alt} className={styles.previewImage} />}
    </dialog>
  );
}
