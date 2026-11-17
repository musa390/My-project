import type { Metadata } from "next";
import { OrdersPage } from "@/components/store";

export const metadata: Metadata = {
  title: "Order History",
  description: "View demo orders saved in this browser.",
  robots: { index: false, follow: false },
};

export default function Page() {
  return <OrdersPage />;
}
