"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, Package, ShoppingBag, Users, Boxes, Tags, Star, ChartNoAxesCombined, Settings, ShieldCheck, Layers3, Store, CreditCard, Image, Mail, UserCog, Shapes, Menu } from "lucide-react";

const nav=[
 {href:"/",label:"Overview",icon:LayoutDashboard},
 {href:"/products",label:"Products",icon:Package},
 {href:"/categories",label:"Categories",icon:Shapes},
 {href:"/brands",label:"Brands",icon:Store},
 {href:"/collections",label:"Collections",icon:Layers3},
 {href:"/orders",label:"Orders",icon:ShoppingBag},
 {href:"/customers",label:"Customers",icon:Users},
 {href:"/inventory",label:"Inventory",icon:Boxes},
 {href:"/payments",label:"Payments",icon:CreditCard},
 {href:"/coupons",label:"Coupons & Discounts",icon:Tags},
 {href:"/reviews",label:"Reviews",icon:Star},
 {href:"/banners",label:"Banners",icon:Image},
 {href:"/newsletter",label:"Newsletter",icon:Mail},
 {href:"/analytics",label:"Analytics",icon:ChartNoAxesCombined},
 {href:"/staff",label:"Staff & Roles",icon:UserCog},
 {href:"/settings",label:"Settings",icon:Settings}
];

export function AdminShell({children}:{children:React.ReactNode}){
 const pathname=usePathname();
 const isLogin=pathname==="/login";
 return <div className="min-h-screen md:grid md:grid-cols-[250px_minmax(0,1fr)]">
  {!isLogin&&<aside className="border-b border-[var(--line)] bg-[#15130f] p-4 md:sticky md:top-0 md:h-screen md:overflow-y-auto md:border-b-0 md:border-r md:p-5">
   <Link href="/" className="flex items-center gap-3"><span className="grid size-10 place-items-center rounded-xl border border-[#80663e] text-[var(--gold)]"><ShieldCheck size={21}/></span><span><span className="block text-sm font-semibold tracking-[.22em]">LUXE PERFUME</span><span className="text-[10px] uppercase tracking-[.24em] text-[var(--muted)]">Administration</span></span></Link>
   <div className="mb-3 mt-7 hidden text-[10px] font-semibold uppercase tracking-[.22em] text-[#857b6d] md:block">Workspace</div>
   <nav aria-label="Admin navigation" className="mt-5 flex gap-1 overflow-x-auto pb-1 md:mt-0 md:grid md:grid-cols-1 md:overflow-visible">{nav.map(({href,label,icon:Icon})=>{const active=pathname===href||(href!=="/"&&pathname.startsWith(href+"/"));return <Link key={href} href={href} aria-current={active?"page":undefined} className={`flex shrink-0 items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition-colors ${active?"bg-[#302719] text-[var(--gold)]":"text-[#c7beb1] hover:bg-[#29231b] hover:text-white"}`}><Icon size={17}/><span>{label}</span></Link>;})}</nav>
   <div className="mt-7 hidden rounded-xl border border-[var(--line)] bg-[#1b1813] p-3 md:block"><div className="text-xs font-medium text-[var(--gold)]">DEMO WORKSPACE</div><p className="mt-1 text-xs leading-5 text-[var(--muted)]">Sample interface and data only. No live store operations are connected.</p></div>
  </aside>}
  <div className="min-w-0"><header className="sticky top-0 z-10 flex h-[64px] items-center justify-between border-b border-[var(--line)] bg-[#100f0d]/95 px-4 backdrop-blur md:px-8"><div className="flex items-center gap-2 text-xs uppercase tracking-[.18em] text-[var(--muted)]"><Menu size={15}/><span>Store management</span></div><div className="flex items-center gap-3"><span className="rounded-full border border-[#665331] px-3 py-1 text-[10px] uppercase tracking-wider text-[var(--gold)]">Demo mode</span>{!isLogin&&<Link href="/login" className="text-sm text-[#d7c8ad] hover:text-white">Sign in</Link>}</div></header><main className="mx-auto w-full max-w-[1440px] p-4 sm:p-5 md:p-8">{children}</main></div>
 </div>;
}
