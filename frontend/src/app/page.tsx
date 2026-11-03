import { Suspense } from "react";
import { TemplateBrowser } from "@/components/templates/TemplateBrowser";
import { StateView } from "@/components/ui/StateView";

export default function HomePage() {
  return (
    <Suspense fallback={<StateView kind="loading" />}>
      <TemplateBrowser />
    </Suspense>
  );
}
