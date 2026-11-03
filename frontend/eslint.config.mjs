import { dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { FlatCompat } from "@eslint/eslintrc";

const compat = new FlatCompat({ baseDirectory: dirname(fileURLToPath(import.meta.url)) });

const config = [
  { ignores: ["node_modules/**", ".next/**", "next-env.d.ts"] },
  ...compat.extends("next/core-web-vitals", "next/typescript"),
  {
    rules: {
      // 用户图片需携带 Cookie 鉴权，next/image 的服务端优化器不带 Cookie 会 401，因此使用原生 <img>
      "@next/next/no-img-element": "off",
    },
  },
];

export default config;
