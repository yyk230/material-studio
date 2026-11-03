import Link from "next/link";
import type { TemplateDto } from "@/lib/api/types";
import styles from "./templates.module.css";

export function TemplateCard({ template }: { template: TemplateDto }) {
  return (
    <Link href={`/templates/${encodeURIComponent(template.id)}`} className={styles.card}>
      <img src={template.coverUrl} alt={template.title} className={styles.cover} loading="lazy" />
      <div className={styles.cardOverlay}>
        <span className={styles.cardTitle}>{template.title}</span>
        <span className={styles.cardMeta}>需 {template.requiredImageCount} 张图</span>
      </div>
    </Link>
  );
}
