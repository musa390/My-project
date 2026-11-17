import type { Metadata } from "next";
import { RegisterPage } from "@/components/store";

export const metadata: Metadata = {
  title: "Create Account",
  description: "Registration interface for the LUXE Parfums frontend demo.",
  robots: { index: false, follow: false },
};

export default function Page() {
  return <RegisterPage />;
}
