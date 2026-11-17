import type { MetadataRoute } from "next";
import { products } from "@/lib/products";

const staticRoutes = [
  "", "/shop", "/collections", "/best-sellers", "/new-arrivals",
  "/about", "/contact", "/faq", "/policies", "/shipping-policy",
  "/return-refund-policy", "/privacy-policy", "/terms-conditions",
];

export default function sitemap(): MetadataRoute.Sitemap {
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
  const collectionSlugs = [...new Set(products.map((product) => product.collection))]
    .map((collection) => `/collections/${encodeURIComponent(collection.toLowerCase().replace(/\s+/g, "-"))}`);
  const productRoutes = products.map((product) => `/products/${encodeURIComponent(product.slug)}`);
  const uniqueRoutes = [...new Set([...staticRoutes, ...collectionSlugs, ...productRoutes])];

  return uniqueRoutes.map((route) => ({
    url: `${base}${route}`,
    changeFrequency: route.startsWith("/products/") ? "monthly" : "weekly",
    priority: route === "" ? 1 : route.startsWith("/products/") ? 0.7 : 0.8,
  }));
}
