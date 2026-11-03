import { Suspense } from "react";
import { LoginForm } from "@/components/auth/LoginForm";
import { StateView } from "@/components/ui/StateView";

export default function LoginPage() {
  return (
    <Suspense fallback={<StateView kind="loading" />}>
      <LoginForm />
    </Suspense>
  );
}
