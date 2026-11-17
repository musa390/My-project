import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";
import { StoreProvider, Shell } from "@/components/store";
const siteUrl = process.env.NEXT_PUBLIC_SITE_URL;
const siteMetadataBase = siteUrl ? new URL(siteUrl) : undefined;
export const metadata: Metadata = {
 title: { default: "LUXE Parfums | Timeless Luxury", template: "%s | LUXE Parfums" },
 description: "Discover refined fragrances, explore scent families and shop the LUXE Parfums demo collection.",
 applicationName: "LUXE Parfums",
 ...(siteMetadataBase ? { metadataBase: siteMetadataBase } : {}),
 openGraph: {
  title: "LUXE Parfums | Timeless Luxury",
  description: "Discover refined fragrances crafted for every essence.",
  type: "website",
  siteName: "LUXE Parfums",
  locale: "en_US",
 },
 twitter: {
  card: "summary_large_image",
  title: "LUXE Parfums | Timeless Luxury",
  description: "Discover refined fragrances crafted for every essence.",
 },
};
export default function RootLayout({ children }: { children: ReactNode }) {
 return <html lang="en"><body><StoreProvider><a className="skip-link" href="#main-content">Skip to content</a><Shell>{children}</Shell></StoreProvider></body></html>;
}
