import type { MetadataRoute } from "next";

export default function robots(): MetadataRoute.Robots {
  const configuredUrl = process.env.NEXT_PUBLIC_SITE_URL;
  let sitemapUrl: string | undefined;

  if (configuredUrl) {
    try {
      const baseUrl = new URL(configuredUrl);
      if (baseUrl.protocol === "https:" || baseUrl.protocol === "http:") {
        sitemapUrl = new URL("/sitemap.xml", baseUrl).toString();
      }
    } catch {
      // Keep robots.txt valid while the owner configures the canonical public URL.
    }
  }

  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: [
        "/account", "/cart", "/checkout", "/login", "/my-orders",
        "/orders", "/order-success", "/track-order", "/wishlist", "/search",
        "/api/",
      ],
    },
    ...(sitemapUrl ? { sitemap: sitemapUrl } : {}),
  };
}
