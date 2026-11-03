const dateTimeFormat = new Intl.DateTimeFormat("zh-CN", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hour12: false,
});

export function formatDateTime(iso: string): string {
  return dateTimeFormat.format(new Date(iso));
}

export function formatMb(bytes: number): string {
  const mb = bytes / 1024 / 1024;
  return Number.isInteger(mb) ? String(mb) : mb.toFixed(1);
}

export function firstChar(name: string): string {
  return Array.from(name.trim())[0]?.toUpperCase() ?? "?";
}

/** 只允许站内相对路径，防止登录后跳转到外部站点。 */
export function safeNextPath(next: string | null): string {
  if (!next || !next.startsWith("/") || next.startsWith("//")) return "/";
  return next;
}
