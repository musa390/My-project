import { chromium } from "playwright";

const baseUrl = process.env.BASE_URL ?? "http://127.0.0.1:3001";
const routes = ["/login","/","/products","/categories","/brands","/collections","/orders","/customers","/inventory","/payments","/returns","/contact-messages","/coupons","/banners","/reviews","/newsletter","/analytics","/staff"];
const viewports = [
  { name: "mobile", width: 390, height: 844 },
  { name: "tablet", width: 768, height: 1024 },
  { name: "desktop", width: 1440, height: 900 },
];

const browser = await chromium.launch({ headless: true });
const failures = [];
try {
  const page = await browser.newPage({ viewport: viewports[0] });
  page.on("pageerror", error => failures.push(`pageerror: ${error.message}`));

  for (const route of routes) {
    const response = await page.goto(new URL(route, baseUrl).toString(), {
      waitUntil: "domcontentloaded",
      timeout: 30000,
    });
    if (!response || response.status() >= 500 || response.status() === 404) {
      failures.push(`route ${route}: HTTP ${response?.status() ?? "no response"}`);
    }
  }

  for (const viewport of viewports) {
    await page.setViewportSize({ width: viewport.width, height: viewport.height });
    await page.goto(baseUrl, { waitUntil: "domcontentloaded", timeout: 30000 });
    const dimensions = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      document: document.documentElement.scrollWidth,
    }));
    if (dimensions.document > dimensions.viewport + 1) {
      failures.push(`${viewport.name}: horizontal overflow ${dimensions.document}px > ${dimensions.viewport}px`);
      await page.screenshot({ path: `phase14-${viewport.name}-overflow.png`, fullPage: true });
    }
  }

  if (failures.length) {
    console.error("PHASE 14 BROWSER QA: FAIL");
    for (const failure of failures) console.error(`- ${failure}`);
    process.exitCode = 1;
  } else {
    console.log(`PHASE 14 BROWSER QA: PASS — ${routes.length} routes; ${viewports.length} viewport sizes`);
  }
} finally {
  await browser.close();
}
