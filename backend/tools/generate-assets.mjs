// 生成模板封面与模拟结果图（自绘 SVG，无外部素材）。
// 用法：node backend/tools/generate-assets.mjs
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..", "src", "main", "resources", "assets");

const templates = [
  { id: "travel-postcard", title: "旅行明信片", category: "创意图片", colors: ["#2b5876", "#4e4376"], shape: "mountain" },
  { id: "pixel-avatar", title: "像素头像", category: "创意图片", colors: ["#11998e", "#38ef7d"], shape: "pixel" },
  { id: "comic-strip", title: "三格漫画", category: "创意图片", colors: ["#f7971e", "#ffd200"], shape: "panels" },
  { id: "spring-festival", title: "新春贺卡", category: "节日祝福", colors: ["#b31217", "#e52d27"], shape: "lantern" },
  { id: "christmas-hug", title: "圣诞合影", category: "节日祝福", colors: ["#134e5e", "#71b280"], shape: "tree" },
  { id: "pet-sticker", title: "萌宠贴纸", category: "萌宠", colors: ["#ee9ca7", "#ffdde1"], shape: "paw" },
  { id: "pet-oil-painting", title: "宠物油画像", category: "萌宠", colors: ["#3e2723", "#a1887f"], shape: "frame" },
  { id: "couple-portrait", title: "情侣写真", category: "写真婚礼", colors: ["#c94b4b", "#4b134f"], shape: "hearts" },
  { id: "wedding-album", title: "婚礼相册", category: "写真婚礼", colors: ["#d7d2cc", "#304352"], shape: "rings" },
];

const shapes = {
  mountain: `<polygon points="40,470 180,260 300,470" fill="#fff" opacity=".35"/><polygon points="160,470 320,220 450,470" fill="#fff" opacity=".5"/><circle cx="370" cy="150" r="42" fill="#ffe082" opacity=".9"/>`,
  pixel: Array.from({ length: 36 }, (_, i) => {
    const x = 120 + (i % 6) * 40;
    const y = 160 + Math.floor(i / 6) * 40;
    const on = [1, 4, 7, 10, 13, 16, 19, 22, 25, 26, 27, 28, 31, 32].includes(i);
    return `<rect x="${x}" y="${y}" width="36" height="36" fill="#fff" opacity="${on ? 0.85 : 0.2}"/>`;
  }).join(""),
  panels: `<rect x="50" y="140" width="380" height="100" rx="8" fill="#fff" opacity=".6"/><rect x="50" y="255" width="380" height="100" rx="8" fill="#fff" opacity=".45"/><rect x="50" y="370" width="380" height="100" rx="8" fill="#fff" opacity=".3"/>`,
  lantern: `<line x1="240" y1="90" x2="240" y2="150" stroke="#ffd54f" stroke-width="6"/><ellipse cx="240" cy="270" rx="120" ry="130" fill="#ffd54f" opacity=".9"/><rect x="200" y="395" width="80" height="20" fill="#ffd54f"/><line x1="240" y1="415" x2="240" y2="480" stroke="#ffd54f" stroke-width="6"/>`,
  tree: `<polygon points="240,110 340,260 140,260" fill="#fff" opacity=".7"/><polygon points="240,190 370,380 110,380" fill="#fff" opacity=".55"/><rect x="215" y="380" width="50" height="70" fill="#fff" opacity=".4"/><circle cx="240" cy="105" r="16" fill="#ffd54f"/>`,
  paw: `<ellipse cx="240" cy="340" rx="90" ry="75" fill="#fff" opacity=".85"/><circle cx="150" cy="230" r="38" fill="#fff" opacity=".85"/><circle cx="210" cy="180" r="38" fill="#fff" opacity=".85"/><circle cx="270" cy="180" r="38" fill="#fff" opacity=".85"/><circle cx="330" cy="230" r="38" fill="#fff" opacity=".85"/>`,
  frame: `<rect x="80" y="120" width="320" height="360" fill="none" stroke="#ffcc80" stroke-width="18"/><ellipse cx="240" cy="300" rx="100" ry="120" fill="#fff" opacity=".35"/>`,
  hearts: `<path d="M170 300 C 110 240, 110 170, 170 170 C 200 170, 215 190, 220 210 C 225 190, 240 170, 270 170 C 330 170, 330 240, 270 300 L 220 350 Z" fill="#fff" opacity=".7"/><path d="M260 380 C 220 340, 220 290, 260 290 C 280 290, 290 302, 293 315 C 296 302, 306 290, 326 290 C 366 290, 366 340, 326 380 L 293 412 Z" fill="#fff" opacity=".5"/>`,
  rings: `<circle cx="200" cy="300" r="85" fill="none" stroke="#fff" stroke-width="18" opacity=".85"/><circle cx="290" cy="300" r="85" fill="none" stroke="#ffe082" stroke-width="18" opacity=".85"/>`,
};

function svg(t, variant) {
  const [c1, c2] = variant === "result" ? [t.colors[1], t.colors[0]] : t.colors;
  const badge =
    variant === "result"
      ? `<rect x="150" y="36" width="180" height="40" rx="20" fill="#000" opacity=".45"/><text x="240" y="63" text-anchor="middle" font-size="20" fill="#fff">模拟结果</text>`
      : `<rect x="24" y="24" width="${t.category.length * 20 + 28}" height="36" rx="18" fill="#000" opacity=".35"/><text x="38" y="48" font-size="18" fill="#fff">${t.category}</text>`;
  const border = variant === "result" ? `<rect x="12" y="12" width="456" height="616" rx="20" fill="none" stroke="#fff" stroke-width="4" opacity=".7"/>` : "";
  // 封面标题由前端卡片叠加显示，SVG 内只在结果图上绘制标题
  const title =
    variant === "result"
      ? `<text x="240" y="560" text-anchor="middle" font-size="44" font-weight="700" fill="#fff" font-family="sans-serif">${t.title}</text>`
      : "";
  return `<svg xmlns="http://www.w3.org/2000/svg" width="480" height="640" viewBox="0 0 480 640">
<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${c1}"/><stop offset="1" stop-color="${c2}"/></linearGradient></defs>
<rect width="480" height="640" fill="url(#g)"/>
${shapes[t.shape]}
${border}
${badge}
${title}
</svg>
`;
}

for (const variant of ["covers", "results"]) {
  for (const t of templates) {
    const file = join(root, variant, `${t.id}.svg`);
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, svg(t, variant === "results" ? "result" : "cover"));
  }
}
console.log(`generated ${templates.length * 2} svg files under ${root}`);
