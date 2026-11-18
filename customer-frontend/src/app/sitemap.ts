import type { MetadataRoute } from "next";
import { getCatalog, getCollections } from "@/lib/commerce";

export const dynamic = "force-dynamic";

const staticRoutes = [
  "", "/shop", "/collections", "/best-sellers", "/new-arrivals",
  "/about", "/contact", "/faq", "/policies", "/shipping-policy",
  "/return-refund-policy", "/privacy-policy", "/terms-conditions",
];

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const configuredUrl = process.env.NEXT_PUBLIC_SITE_URL;
  if (!configuredUrl) return [];

  let baseUrl: URL;
  try {
    baseUrl = new URL(configuredUrl);
    if (baseUrl.protocol !== "https:" && baseUrl.protocol !== "http:") return [];
  } catch {
    return [];
  }

  const base = baseUrl.toString().replace(/\/$/, "");
  const [catalog, collections] = await Promise.all([
    getCatalog({ page: 0, size: 100, sort: "newest" }).catch(() => null),
    getCollections().catch(() => []),
  ]);

  const collectionRoutes = collections
    .map((collection) => String(collection.slug || ""))
    .filter(Boolean)
    .map((slug) => `/collections/${encodeURIComponent(slug)}`);
  const productRoutes = (catalog?.items || [])
    .map((product) => String(product.slug || ""))
    .filter(Boolean)
    .map((slug) => `/products/${encodeURIComponent(slug)}`);
  const uniqueRoutes = [...new Set([...staticRoutes, ...collectionRoutes, ...productRoutes])];

  return uniqueRoutes.map((route) => ({
    url: `${base}${route}`,
    changeFrequency: route.startsWith("/products/") ? "monthly" : "weekly",
    priority: route === "" ? 1 : route.startsWith("/products/") ? 0.7 : 0.8,
  }));
}
