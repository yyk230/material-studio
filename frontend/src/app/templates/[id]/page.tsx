"use client";

import { useParams } from "next/navigation";
import { TemplateDetail } from "@/components/templates/TemplateDetail";

export default function TemplatePage() {
  const params = useParams<{ id: string }>();
  return <TemplateDetail templateId={params.id} />;
}
