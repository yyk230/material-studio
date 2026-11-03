import type { NextConfig } from "next";

// standalone 模式下 rewrites 在构建时固化：Docker 镜像通过 build arg BACKEND_URL 指定后端地址
const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  output: "standalone",
  eslint: {
    // CI 单独执行 npm run lint
    ignoreDuringBuilds: true,
  },
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backendUrl}/api/:path*` }];
  },
};

export default nextConfig;
