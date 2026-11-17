import { readFileSync, existsSync } from "node:fs";
const required = [
  "src/app/page.tsx",
  "src/app/login/page.tsx",
  "src/app/[section]/page.tsx",
  "src/app/loading.tsx",
  "src/app/error.tsx",
  "src/app/not-found.tsx",
  "src/components/admin-shell.tsx",
  "src/components/management-workspace.tsx"
];
const missing = required.filter((path) => !existsSync(new URL("../" + path, import.meta.url)));
const workspace = readFileSync(new URL("../src/components/management-workspace.tsx", import.meta.url), "utf8");
const sections = ["products","categories","brands","collections","orders","customers","inventory","payments","coupons","reviews","banners","newsletter","analytics","staff","settings"];
const absent = sections.filter((section) => !workspace.includes(section + ":{"));
if (missing.length || absent.length) {
  console.error("Phase 3 verification failed.");
  if (missing.length) console.error("Missing files:", missing.join(", "));
  if (absent.length) console.error("Missing modules:", absent.join(", "));
  process.exit(1);
}
console.log("Phase 3 static verification: PASS");
console.log(`Required files: ${required.length}/${required.length}`);
console.log(`Management modules: ${sections.length}/${sections.length}`);
