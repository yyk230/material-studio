"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { StateView } from "@/components/ui/StateView";
import { isAbortError } from "@/lib/api/client";
import { fetchCategories, fetchTemplates } from "@/lib/api/templates";
import type { TemplateDto } from "@/lib/api/types";
import { CategoryFilter } from "./CategoryFilter";
import { TemplateCard } from "./TemplateCard";
import styles from "./templates.module.css";

type Load<T> = { state: "loading" } | { state: "ready"; data: T } | { state: "error"; message: string };

export function TemplateBrowser() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const category = searchParams.get("category");

  const [categories, setCategories] = useState<string[]>([]);
  const [templates, setTemplates] = useState<Load<TemplateDto[]>>({ state: "loading" });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    fetchCategories(controller.signal)
      .then(setCategories)
      .catch(() => {
        // 分类加载失败时只显示“全部”，列表区域仍可独立重试
      });
    return () => controller.abort();
  }, [attempt]);

  useEffect(() => {
    const controller = new AbortController();
    setTemplates({ state: "loading" });
    fetchTemplates(category, controller.signal)
      .then((data) => setTemplates({ state: "ready", data }))
      .catch((error: unknown) => {
        if (isAbortError(error)) return;
        setTemplates({ state: "error", message: error instanceof Error ? error.message : "加载失败" });
      });
    return () => controller.abort();
  }, [category, attempt]);

  function selectCategory(next: string | null) {
    const params = new URLSearchParams(searchParams.toString());
    if (next) params.set("category", next);
    else params.delete("category");
    const query = params.toString();
    router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
  }

  return (
    <div>
      <h1 className={styles.pageTitle}>选择模板</h1>
      <CategoryFilter categories={categories} selected={category} onSelect={selectCategory} />
      {templates.state === "loading" && (
        <div className={styles.grid} aria-busy="true" aria-label="模板加载中">
          {Array.from({ length: 8 }, (_, i) => (
            <div key={i} className={styles.skeleton} />
          ))}
        </div>
      )}
      {templates.state === "error" && (
        <StateView kind="error" message={templates.message} onRetry={() => setAttempt((n) => n + 1)} />
      )}
      {templates.state === "ready" && templates.data.length === 0 && (
        <StateView kind="empty" message="该分类暂无模板" />
      )}
      {templates.state === "ready" && templates.data.length > 0 && (
        <ul className={styles.grid}>
          {templates.data.map((t) => (
            <li key={t.id}>
              <TemplateCard template={t} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
