"use client";

import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { Search, Plus, Pencil, Trash2, X, Check, RefreshCw } from "lucide-react";
import { adminApi, type CatalogRecord, type TaxonomyRecord, type StaffOrder, type InventoryRecord, type ContactMessage } from "@/lib/management";

type Row = Record<string, unknown> & { id: number | string };
type Resource = "products" | "categories" | "brands" | "collections" | "orders" | "inventory" | "contacts" | "returns";
const titles:Record<string,string>={products:"Products",categories:"Categories",brands:"Brands",collections:"Collections",orders:"Orders",inventory:"Inventory","contact-messages":"Contact Messages",returns:"Return Requests"};
const aliases:Record<string,string>={"contact-messages":"contacts","returns":"returns"};
const unsupported:Record<string,string>={
 customers:"The backend does not yet expose a staff customer-directory endpoint.",
 payments:"The backend currently exposes COD collection and refund actions, but no staff payment-list endpoint.",
 coupons:"Coupon validation exists in checkout, but staff coupon CRUD endpoints are not available yet.",
 reviews:"Review moderation endpoints are not available in the current backend.",
 banners:"Banner management endpoints are not available in the current backend.",
 newsletter:"Public newsletter double opt-in exists; staff subscriber-list management is not available yet.",
 analytics:"Live analytics/reporting endpoints are not available yet.",
 staff:"Staff and role administration endpoints are not available in the current backend.",
 settings:"Store settings endpoints are not available in the current backend."
};

export function ManagementWorkspace({section}:{section:string}) {
 const key=aliases[section]||section;
 const resource=key as Resource;
 const supported=["products","categories","brands","collections","orders","inventory","contacts","returns"].includes(resource);
 const [rows,setRows]=useState<Row[]>([]);
 const [query,setQuery]=useState("");
 const [loading,setLoading]=useState(true);
 const [saving,setSaving]=useState(false);
 const [error,setError]=useState("");
 const [notice,setNotice]=useState("");
 const [formOpen,setFormOpen]=useState(false);
 const [editing,setEditing]=useState<number|null>(null);
 const [draft,setDraft]=useState({name:"",slug:"",description:"",categoryId:"",brandId:"",sku:"",variantName:"",price:"",initialStock:"0",quantityChange:"",note:""});
 const title=titles[section]||titles[key]||"Management";

 const load=useCallback(async()=>{
  if(!supported){setLoading(false);return;}
  setLoading(true);setError("");
  try{
   let result:unknown[]=[];
   if(resource==="products")result=(await adminApi.products(query)).items;
   if(resource==="categories")result=await adminApi.categories();
   if(resource==="brands")result=await adminApi.brands();
   if(resource==="collections")result=await adminApi.collections();
   if(resource==="orders")result=(await adminApi.orders(query)).items;
   if(resource==="inventory")result=await adminApi.lowStock();
   if(resource==="contacts")result=(await adminApi.contacts()).items;
   if(resource==="returns")result=await adminApi.returns();
   setRows(result as Row[]);
  }catch(reason){setError(reason instanceof Error?reason.message:"Unable to load records from the backend.");setRows([]);}
  finally{setLoading(false);}
 },[query,resource,supported]);

 useEffect(()=>{void load();},[load]);
 const filtered=useMemo(()=>rows.filter(row=>Object.values(row).join(" ").toLowerCase().includes(query.toLowerCase())),[rows,query]);

 function resetForm(){setDraft({name:"",slug:"",description:"",categoryId:"",brandId:"",sku:"",variantName:"",price:"",initialStock:"0",quantityChange:"",note:""});setEditing(null);setFormOpen(false);}
 function startEdit(row:Row){
  setEditing(Number(row.id));
  setDraft({name:String(row.name||""),slug:String(row.slug||""),description:String(row.description||""),categoryId:String(row.category_id||""),brandId:String(row.brand_id||""),sku:"",variantName:"",price:"",initialStock:"0",quantityChange:"",note:""});
  setFormOpen(true);
 }
 async function submit(event:FormEvent<HTMLFormElement>){
  event.preventDefault();setSaving(true);setError("");setNotice("");
  try{
   if(["categories","brands","collections"].includes(resource)){
    const body={name:draft.name.trim(),slug:draft.slug.trim(),description:draft.description.trim(),active:true};
    if(!body.name)throw new Error("Name is required.");
    if(resource==="categories")editing?await adminApi.updateCategory(editing,body):await adminApi.createCategory(body);
    if(resource==="brands")editing?await adminApi.updateBrand(editing,body):await adminApi.createBrand(body);
    if(resource==="collections")editing?await adminApi.updateCollection(editing,body):await adminApi.createCollection(body);
   }else if(resource==="products"){
    if(editing){
     const body:Record<string,unknown>={name:draft.name.trim(),slug:draft.slug.trim(),description:draft.description.trim()};
     if(draft.categoryId)body.categoryId=Number(draft.categoryId);
     if(draft.brandId)body.brandId=Number(draft.brandId);
     await adminApi.updateProduct(editing,body);
    }else{
     if(!draft.name.trim()||!draft.categoryId)throw new Error("Product name and category ID are required.");
     const product=await adminApi.createProduct({name:draft.name.trim(),slug:draft.slug.trim(),categoryId:Number(draft.categoryId),brandId:draft.brandId?Number(draft.brandId):null,shortDescription:draft.description.trim(),description:draft.description.trim(),productType:"STANDARD",status:"DRAFT",featured:false});
     if(draft.sku.trim()&&draft.variantName.trim()&&draft.price.trim()){
      await adminApi.createVariant(Number(product.id),{sku:draft.sku.trim(),variantName:draft.variantName.trim(),price:Number(draft.price),volumeMl:Number(draft.variantName.match(/\d+(?:\.\d+)?/)?.[0]||0)||null,initialStock:Number(draft.initialStock||0),isDefault:true,isActive:true});
     }
    }
   }else if(resource==="inventory"){
    const variantId=Number(draft.categoryId);
    const delta=Number(draft.quantityChange);
    if(!variantId||!Number.isInteger(delta)||delta===0||!draft.note.trim())throw new Error("Enter variant ID, a non-zero whole-number adjustment and a note.");
    await adminApi.adjustInventory(variantId,delta,draft.note.trim());
   }else{
    throw new Error("This section is read-only in the current backend integration.");
   }
   setNotice(editing?"Record updated in the database.":"Record created in the database.");
   resetForm();await load();
  }catch(reason){setError(reason instanceof Error?reason.message:"The operation failed.");}
  finally{setSaving(false);}
 }
 async function archive(row:Row){
  if(!window.confirm("Archive/deactivate this record in the database?"))return;
  setError("");setNotice("");
  try{
   if(resource==="products")await adminApi.archiveProduct(Number(row.id));
   else if(resource==="categories")await adminApi.archiveCategory(Number(row.id));
   else if(resource==="brands")await adminApi.archiveBrand(Number(row.id));
   else if(resource==="collections")await adminApi.archiveCollection(Number(row.id));
   else throw new Error("Archive is not supported for this resource.");
   setNotice("Record archived/deactivated in the database.");await load();
  }catch(reason){setError(reason instanceof Error?reason.message:"Archive failed.");}
 }
 async function updateOrder(row:Row,status:string){
  try{await adminApi.updateOrderStatus(String(row.orderNumber),status,"Updated from Admin Website");setNotice("Order status saved to the database.");await load();}
  catch(reason){setError(reason instanceof Error?reason.message:"Order update failed.");}
 }
 async function publishProduct(row:Row){
  try{await adminApi.updateProduct(Number(row.id),{status:"ACTIVE"});setNotice("Product published to the live catalogue.");await load();}
  catch(reason){setError(reason instanceof Error?reason.message:"Product cannot be published until its required variant is configured.");}
 }
 async function updateContact(row:Row,status:string){
  try{await adminApi.updateContactStatus(Number(row.id),status);setNotice("Contact message status saved.");await load();}
  catch(reason){setError(reason instanceof Error?reason.message:"Contact update failed.");}
 }

 if(!supported)return <section className="rounded-2xl border border-[var(--line)] bg-[var(--panel)] p-8"><p className="text-xs uppercase tracking-[.2em] text-[var(--gold)]">Backend integration status</p><h1 className="mt-3 text-2xl font-semibold">{title}</h1><p className="mt-3 max-w-2xl text-sm leading-7 text-[var(--muted)]">{unsupported[section]||"This section has no corresponding backend endpoint yet."}</p><p className="mt-4 text-xs text-[#d2bd95]">No demo records are shown or changed here. This module remains pending until its secure backend API is implemented.</p></section>;

 return <div className="space-y-6">
  <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs uppercase tracking-[.22em] text-[var(--gold)]">Live database · {resource}</p><h1 className="mt-2 text-3xl font-semibold">{title}</h1><p className="mt-2 max-w-2xl text-sm leading-6 text-[var(--muted)]">Records below are retrieved from the authenticated LUXE backend API.</p></div><div className="flex gap-2"><button onClick={()=>void load()} disabled={loading} className="inline-flex items-center gap-2 rounded-lg border border-[var(--line)] px-3 py-2.5 text-sm"><RefreshCw size={15}/>Refresh</button>{["products","categories","brands","collections","inventory"].includes(resource)&&<button onClick={()=>{setEditing(null);setFormOpen(v=>!v);}} className="inline-flex items-center gap-2 rounded-lg bg-[var(--gold)] px-4 py-2.5 text-sm font-semibold text-[#17130d]"><Plus size={16}/>{formOpen?"Close":"Add new"}</button>}</div></div>
  <div className="rounded-xl border border-[#365b43] bg-[#122017] px-4 py-3 text-xs leading-5 text-[#b9d8c0]">LIVE API MODE — changes are sent to the backend. Server-side permissions, validation and business rules remain authoritative.</div>
  {error&&<div className="rounded-lg border border-red-900 bg-red-950/40 px-4 py-3 text-sm text-red-200" role="alert">{error}</div>}
  {notice&&<div className="rounded-lg border border-[var(--line)] bg-[var(--panel)] px-4 py-3 text-sm text-[#c7d9c7]" role="status">{notice}</div>}
  {formOpen&&<form onSubmit={submit} className="rounded-2xl border border-[var(--line)] bg-[var(--panel)] p-5"><div className="mb-4 flex items-center justify-between"><h2 className="font-semibold">{editing?"Update database record":"Create database record"}</h2><button type="button" onClick={resetForm} aria-label="Close form"><X size={18}/></button></div>
   {["categories","brands","collections","products"].includes(resource)&&<div className="grid gap-3 md:grid-cols-2">
    <label className="text-xs text-[var(--muted)]">Name *<input required value={draft.name} onChange={e=>setDraft(d=>({...d,name:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)]">Slug<input value={draft.slug} onChange={e=>setDraft(d=>({...d,slug:e.target.value}))} placeholder="Auto-generated when blank" className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)] md:col-span-2">Description<textarea value={draft.description} onChange={e=>setDraft(d=>({...d,description:e.target.value}))} rows={3} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    {resource==="products"&&<><label className="text-xs text-[var(--muted)]">Category ID *<input required type="number" min="1" value={draft.categoryId} onChange={e=>setDraft(d=>({...d,categoryId:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Brand ID (optional)<input type="number" min="1" value={draft.brandId} onChange={e=>setDraft(d=>({...d,brandId:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>{!editing&&<><label className="text-xs text-[var(--muted)]">Initial variant SKU<input value={draft.sku} onChange={e=>setDraft(d=>({...d,sku:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Variant name / size<input value={draft.variantName} onChange={e=>setDraft(d=>({...d,variantName:e.target.value}))} placeholder="50ml" className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Variant price (BDT)<input type="number" min="0" step="0.01" value={draft.price} onChange={e=>setDraft(d=>({...d,price:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Initial stock<input type="number" min="0" step="1" value={draft.initialStock} onChange={e=>setDraft(d=>({...d,initialStock:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label></>}</>}
   </div>}
   {resource==="inventory"&&<div className="grid gap-3 md:grid-cols-3"><label className="text-xs text-[var(--muted)]">Variant ID *<input required type="number" min="1" value={draft.categoryId} onChange={e=>setDraft(d=>({...d,categoryId:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Quantity change (+/-) *<input required type="number" step="1" value={draft.quantityChange} onChange={e=>setDraft(d=>({...d,quantityChange:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Reason / note *<input required maxLength={255} value={draft.note} onChange={e=>setDraft(d=>({...d,note:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label></div>}
   <button type="submit" disabled={saving} className="mt-5 inline-flex items-center gap-2 rounded-lg border border-[#80663e] px-4 py-2.5 text-sm text-[var(--gold)] disabled:opacity-50"><Check size={15}/>{saving?"SAVING…":editing?"SAVE CHANGES":"SAVE TO DATABASE"}</button>
   {resource==="products"&&!editing&&<p className="mt-3 text-xs text-[var(--muted)]">Products are created as DRAFT. Add a variant to establish a price and stock, then publish using the product status workflow. A variant is created only when SKU, variant name and price are all supplied.</p>}
  </form>}
  <section className="overflow-hidden rounded-2xl border border-[var(--line)] bg-[var(--panel)]"><div className="flex flex-col gap-3 border-b border-[var(--line)] p-4 sm:flex-row sm:items-center sm:justify-between"><label className="flex-1 text-xs text-[var(--muted)]"><span className="sr-only">Search records</span><div className="flex items-center gap-2 rounded-lg border border-[var(--line)] bg-[#100f0d] px-3"><Search size={15}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder={"Search "+title.toLowerCase()+"…"} className="w-full bg-transparent py-2.5 text-sm text-white outline-none"/></div></label><span className="text-xs text-[var(--muted)]">{filtered.length} loaded record(s)</span></div>
  {loading?<div className="px-6 py-16 text-center text-sm text-[var(--muted)]">Loading live records…</div>:filtered.length===0?<div className="px-6 py-16 text-center"><p className="font-medium">No live records found</p><p className="mt-2 text-sm text-[var(--muted)]">Try refreshing or changing the search.</p></div>:<div className="overflow-x-auto"><table className="w-full min-w-[720px] text-left text-sm"><thead className="bg-[#201d18] text-xs uppercase tracking-wider text-[var(--muted)]"><tr><th className="px-5 py-3">Record</th><th className="px-5 py-3">Details</th><th className="px-5 py-3">Status</th><th className="px-5 py-3">Value</th><th className="px-5 py-3">Actions</th></tr></thead><tbody>{filtered.map((row,index)=>{
   const id=String(row.id??row.variant_id??index);
   const name=String(row.name??row.product_name??row.orderNumber??row.full_name??row.variant_name??row.subject??"Record");
   const detail=String(row.slug??row.category_name??row.brand_name??row.customerEmail??row.email??row.sku??row.order_number??"");
   const status=String(row.status??row.orderStatus??row.order_status??row.paymentStatus??row.payment_status??"ACTIVE");
   const value=String(row.price_min??row.grandTotal??row.grand_total??row.available_quantity??row.quantity_on_hand??row.message??"—");
   return <tr key={id} className="border-t border-[var(--line)] hover:bg-white/[.02]"><td className="px-5 py-4"><div className="font-medium">{name}</div><div className="mt-1 text-[10px] text-[var(--muted)]">#{id}</div></td><td className="max-w-[280px] truncate px-5 py-4 text-[#c9c0b3]">{detail}</td><td className="px-5 py-4">{resource==="orders"?<select value={status} onChange={e=>void updateOrder(row,e.target.value)} className="rounded-lg border border-[var(--line)] bg-[#100f0d] px-2 py-1.5 text-xs text-white"><option value={status}>{status}</option>{["CONFIRMED","PROCESSING","SHIPPED","DELIVERED","CANCELLED"].filter(s=>s!==status).map(s=><option key={s}>{s}</option>)}</select>:resource==="contacts"?<select value={status} onChange={e=>void updateContact(row,e.target.value)} className="rounded-lg border border-[var(--line)] bg-[#100f0d] px-2 py-1.5 text-xs text-white">{["NEW","IN_PROGRESS","RESOLVED","SPAM"].map(s=><option key={s}>{s}</option>)}</select>:status}</td><td className="max-w-[240px] truncate px-5 py-4 text-[#e1d5c2]">{value}</td><td className="px-5 py-4"><div className="flex items-center gap-2">{["products","categories","brands","collections"].includes(resource)&&<><button title="Edit" aria-label={"Edit "+name} onClick={()=>startEdit(row)} className="rounded-md border border-[var(--line)] p-2 text-[var(--muted)] hover:text-white"><Pencil size={14}/></button><button title="Archive" aria-label={"Archive "+name} onClick={()=>void archive(row)} className="rounded-md border border-[var(--line)] p-2 text-[var(--muted)] hover:text-red-300"><Trash2 size={14}/></button>{resource==="products"&&status==="DRAFT"&&<button title="Publish" onClick={()=>void publishProduct(row)} className="rounded-md border border-[#655333] px-2 py-1 text-xs text-[var(--gold)]">PUBLISH</button>}</>}</div></td></tr>;
  })}</tbody></table></div>}
  <div className="flex items-center justify-between border-t border-[var(--line)] px-5 py-3 text-xs text-[var(--muted)]"><span>Source: LUXE backend REST API</span><span>Server is authoritative</span></div></section>
 </div>;
}
