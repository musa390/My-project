"use client";

import { useEffect, useState } from "react";
import { CreditCard, ShoppingCart, Package, Mail, RefreshCw } from "lucide-react";
import { adminApi, type StaffOrder } from "@/lib/management";

export default function DashboardPage(){
 const [products,setProducts]=useState(0);const [orders,setOrders]=useState<StaffOrder[]>([]);const [contacts,setContacts]=useState(0);
 const [loading,setLoading]=useState(true);const [error,setError]=useState("");
 async function load(){
  setLoading(true);setError("");
  try{
   const [productPage,orderPage,contactPage]=await Promise.all([adminApi.products(),adminApi.orders(),adminApi.contacts()]);
   setProducts(productPage.totalElements);setOrders(orderPage.items.slice(0,6));setContacts(contactPage.items.filter(item=>item.status==="NEW").length);
  }catch(reason){setError(reason instanceof Error?reason.message:"Dashboard data could not be loaded.");}
  finally{setLoading(false);}
 }
 useEffect(()=>{void load();},[]);
 const sales=orders.reduce((sum,order)=>sum+Number(order.grandTotal||0),0);
 const metrics=[{label:"Products",value:String(products),note:"Live catalogue records",icon:Package},{label:"Orders",value:String(orders.length),note:"Recent records loaded",icon:ShoppingCart},{label:"New contact messages",value:String(contacts),note:"Awaiting staff review",icon:Mail},{label:"Recent order value",value:"৳ "+sales.toLocaleString("en-BD"),note:"Visible recent orders only",icon:CreditCard}];
 return <div className="space-y-8"><section className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs uppercase tracking-[.24em] text-[var(--gold)]">Live overview</p><h1 className="mt-2 text-3xl font-semibold">Dashboard</h1><p className="mt-2 text-sm text-[var(--muted)]">A snapshot of your store database.</p></div><button onClick={()=>void load()} disabled={loading} className="inline-flex items-center gap-2 rounded-lg border border-[var(--line)] px-3 py-2 text-sm"><RefreshCw size={15}/>Refresh</button></section>
 {error&&<p className="rounded-lg border border-red-900 bg-red-950/40 p-3 text-sm text-red-200" role="alert">{error}</p>}
 <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{metrics.map(({label,value,note,icon:Icon})=><article key={label} className="rounded-2xl border border-[var(--line)] bg-[var(--panel)] p-5"><div className="flex items-center justify-between text-sm text-[var(--muted)]"><span>{label}</span><Icon size={18} className="text-[var(--gold)]"/></div><div className="mt-5 text-2xl font-semibold">{loading?"—":value}</div><div className="mt-2 text-xs text-[#8e8578]">{note}</div></article>)}</section>
 <section className="overflow-hidden rounded-2xl border border-[var(--line)] bg-[var(--panel)]"><div className="border-b border-[var(--line)] p-5"><h2 className="font-medium">Recent orders</h2><p className="mt-1 text-xs text-[var(--muted)]">Live records returned by the staff orders API.</p></div>{loading?<p className="p-6 text-sm text-[var(--muted)]">Loading…</p>:orders.length?<div className="overflow-x-auto"><table className="w-full min-w-[600px] text-left text-sm"><thead className="text-xs text-[var(--muted)]"><tr><th className="px-5 py-3">Order</th><th className="px-5 py-3">Customer</th><th className="px-5 py-3">Total</th><th className="px-5 py-3">Status</th></tr></thead><tbody>{orders.map(order=><tr key={order.orderNumber} className="border-t border-[var(--line)]"><td className="px-5 py-4">{order.orderNumber}</td><td className="px-5 py-4 text-[#c9c0b3]">{order.customerEmail}</td><td className="px-5 py-4">৳ {Number(order.grandTotal||0).toLocaleString("en-BD")}</td><td className="px-5 py-4"><span className="rounded-full border border-[#655333] px-2.5 py-1 text-xs text-[var(--gold)]">{order.orderStatus}</span></td></tr>)}</tbody></table></div>:<p className="p-6 text-sm text-[var(--muted)]">No orders have been placed yet.</p>}</section>
 </div>;
}
