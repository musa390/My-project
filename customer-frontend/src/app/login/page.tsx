import type { Metadata } from "next";
import { LoginPage } from "@/components/store";

export const metadata: Metadata = {
  title: "Sign In",
  description: "Sign-in interface for the LUXE Parfums frontend demo.",
  robots: { index: false, follow: false },
};

export default function Page() {
  return <LoginPage />;
}
