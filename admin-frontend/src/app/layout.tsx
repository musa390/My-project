import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: { default: "LUXE PERFUME | Admin", template: "%s | LUXE Admin" },
  description: "LUXE PERFUME administration workspace. Demo interface; backend not connected.",
  robots: { index: false, follow: false }
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}