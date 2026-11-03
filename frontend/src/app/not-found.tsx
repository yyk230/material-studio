import Link from "next/link";
import { StateView } from "@/components/ui/StateView";

export default function NotFound() {
  return (
    <StateView
      kind="empty"
      message="页面不存在"
      action={
        <Link href="/" className="btn">
          返回首页
        </Link>
      }
    />
  );
}
