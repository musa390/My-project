import type { Metadata } from "next";
import "./globals.css";
import { AdminShell } from "@/components/admin-shell";
export const metadata:Metadata={title:{default:"LUXE PERFUME Admin",template:"%s | LUXE PERFUME Admin"},description:"LUXE PERFUME administration demo interface. Backend not connected.",robots:{index:false,follow:false}};
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="en"><body><AdminShell>{children}</AdminShell></body></html>}
