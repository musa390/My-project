import { readFileSync, existsSync } from "node:fs";

const requiredFiles = [
  "src/app/page.tsx",
  "src/app/login/page.tsx",
  "src/app/[section]/page.tsx",
  "src/app/loading.tsx",
  "src/app/error.tsx",
  "src/app/not-found.tsx",
  "src/components/admin-shell.tsx",
  "src/components/management-workspace.tsx"
];

const missingFiles = requiredFiles.filter(
  path => !existsSync(new URL("../" + path, import.meta.url))
);

const workspace = readFileSync(
  new URL("../src/components/management-workspace.tsx", import.meta.url),
  "utf8"
);

const titleBlock = workspace.match(
  /const titles\s*:\s*Record<string,\s*string>\s*=\s*\{([\s\S]*?)\};/
)?.[1] ?? "";

const declaredSections = new Set(
  [...titleBlock.matchAll(/(?:^|,)\s*["']?([a-z-]+)["']?\s*:/g)]
    .map(match => match[1])
);

// "settings" is intentionally not included: the current backend does not
// expose a store-settings API, and the UI explicitly marks it unavailable.
const requiredSections = [
  "products", "categories", "brands", "collections", "orders",
  "customers", "payments", "inventory", "contact-messages", "returns",
  "coupons", "banners", "reviews", "newsletter", "analytics", "staff"
];

const missingSections = requiredSections.filter(
  section => !declaredSections.has(section)
);

if (missingFiles.length || missingSections.length) {
  console.error("Admin workspace static verification failed.");
  if (missingFiles.length) {
    console.error("Missing files:", missingFiles.join(", "));
  }
  if (missingSections.length) {
    console.error("Missing management sections:", missingSections.join(", "));
  }
  process.exit(1);
}

console.log("Admin workspace static verification: PASS");
console.log(`Required files: ${requiredFiles.length}/${requiredFiles.length}`);
console.log(`Management sections: ${requiredSections.length}/${requiredSections.length}`);
console.log("Unsupported store settings remain explicitly unimplemented.");
