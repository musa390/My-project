"use client";

import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { Search, Plus, Pencil, Trash2, X, Check, RefreshCw } from "lucide-react";
import { adminApi, type AnalyticsOverview } from "@/lib/management";
import { getStaffCurrentUser } from "@/lib/auth";

type Row = Record<string, unknown> & { id: number | string };
type Resource = "products" | "categories" | "brands" | "collections" | "orders" | "customers" | "payments" | "inventory" | "contacts" | "returns" | "coupons" | "banners" | "reviews" | "newsletter" | "analytics" | "staff";
const titles:Record<string,string>={products:"Products",categories:"Categories",brands:"Brands",collections:"Collections",orders:"Orders",customers:"Customers",payments:"Payments",inventory:"Inventory","contact-messages":"Contact Messages",returns:"Return Requests",coupons:"Coupons & Discounts",banners:"Banners",reviews:"Reviews",newsletter:"Newsletter",analytics:"Analytics",staff:"Staff & Roles"};
const aliases:Record<string,string>={"contact-messages":"contacts","returns":"returns"};
const unsupported:Record<string,string>={
 staff:"Staff and role administration endpoints are not available in the current backend.",
 settings:"Store settings endpoints are not available in the current backend."
};

export function ManagementWorkspace({section}:{section:string}) {
 const key=aliases[section]||section;
 const resource=key as Resource;
 const supported=["products","categories","brands","collections","orders","customers","payments","inventory","contacts","returns","coupons","banners","reviews","newsletter","analytics","staff"].includes(resource);
 const [rows,setRows]=useState<Row[]>([]);
 const [analytics,setAnalytics]=useState<AnalyticsOverview|null>(null);
 const [productImages,setProductImages]=useState<Array<Record<string,unknown>>>([]);
 const [canManageCustomers,setCanManageCustomers]=useState(false);
 const [query,setQuery]=useState("");
 const [loading,setLoading]=useState(true);
 const [saving,setSaving]=useState(false);
 const [error,setError]=useState("");
 const [notice,setNotice]=useState("");
 const [formOpen,setFormOpen]=useState(false);
 const [editing,setEditing]=useState<number|null>(null);
 const [draft,setDraft]=useState({name:"",slug:"",description:"",categoryId:"",brandId:"",sku:"",variantName:"",price:"",initialStock:"0",quantityChange:"",note:"",discountType:"PERCENTAGE",discountValue:"10",minimumOrderAmount:"0",maximumDiscountAmount:"",usageLimit:"",perUserLimit:"",startsAt:"",endsAt:"",imageUrl:"",targetUrl:"",placement:"HOME_HERO",sortOrder:"0",roleCodes:"",altText:"",primaryImage:"true"});
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
   if(resource==="customers")result=(await adminApi.customers(query)).items;
   if(resource==="payments")result=(await adminApi.payments(query)).items;
   if(resource==="inventory")result=await adminApi.lowStock();
   if(resource==="contacts")result=(await adminApi.contacts()).items;
   if(resource==="returns")result=await adminApi.returns();
   if(resource==="coupons")result=(await adminApi.coupons(query)).items;
   if(resource==="banners")result=(await adminApi.banners(query)).items;
   if(resource==="reviews")result=(await adminApi.reviews(query)).items;
   if(resource==="newsletter")result=(await adminApi.newsletterSubscribers(query)).items;
   if(resource==="analytics"){setAnalytics(await adminApi.analytics());result=[];}
   if(resource==="staff")result=await adminApi.staffUsers();
   setRows(result as Row[]);
  }catch(reason){setError(reason instanceof Error?reason.message:"Unable to load records from the backend.");setRows([]);}
  finally{setLoading(false);}
 },[query,resource,supported]);

 useEffect(()=>{void load();},[load]);
 useEffect(()=>{getStaffCurrentUser().then(user=>setCanManageCustomers(user.roles.includes("ADMIN"))).catch(()=>setCanManageCustomers(false));},[]);
 const filtered=useMemo(()=>rows.filter(row=>Object.values(row).join(" ").toLowerCase().includes(query.toLowerCase())),[rows,query]);

 function resetForm(){setProductImages([]);setDraft({name:"",slug:"",description:"",categoryId:"",brandId:"",sku:"",variantName:"",price:"",initialStock:"0",quantityChange:"",note:"",discountType:"PERCENTAGE",discountValue:"10",minimumOrderAmount:"0",maximumDiscountAmount:"",usageLimit:"",perUserLimit:"",startsAt:"",endsAt:"",imageUrl:"",targetUrl:"",placement:"HOME_HERO",sortOrder:"0",roleCodes:"",altText:"",primaryImage:"true"});setEditing(null);setFormOpen(false);}
 function startEdit(row:Row){
  setEditing(Number(row.id));
  if(resource==="products"){setProductImages([]);adminApi.productImages(Number(row.id)).then(setProductImages).catch(reason=>setError(reason instanceof Error?reason.message:"Could not load product images."));}
  setDraft({name:String(row.name||row.code||row.title||""),slug:String(row.slug||""),description:String(row.description||row.subtitle||""),categoryId:String(row.category_id||""),brandId:String(row.brand_id||""),sku:"",variantName:"",price:"",initialStock:"0",quantityChange:"",note:"",discountType:String(row.discount_type||"PERCENTAGE"),discountValue:String(row.discount_value||"10"),minimumOrderAmount:String(row.minimum_order_amount||"0"),maximumDiscountAmount:String(row.maximum_discount_amount||""),usageLimit:String(row.usage_limit||""),perUserLimit:String(row.per_user_limit||""),startsAt:String(row.starts_at||""),endsAt:String(row.ends_at||""),imageUrl:"",altText:"",primaryImage:"true",targetUrl:String(row.target_url||""),placement:String(row.placement||"HOME_HERO"),sortOrder:String(row.sort_order||"0"),roleCodes:String(row.role_codes||"")});
  setFormOpen(true);
 }
 async function submit(event:FormEvent<HTMLFormElement>){
  event.preventDefault();setSaving(true);setError("");setNotice("");
  try{
   if(["categories","brands","collections"].includes(resource)){
    const body={name:draft.name.trim(),slug:draft.slug.trim(),description:draft.description.trim(),active:true};
    if(!body.name)throw new Error("Name is required.");
    if(resource==="categories"){
     if(editing) await adminApi.updateCategory(editing,body);
     else await adminApi.createCategory(body);
    }
    if(resource==="brands"){
     if(editing) await adminApi.updateBrand(editing,body);
     else await adminApi.createBrand(body);
    }
    if(resource==="collections"){
     if(editing) await adminApi.updateCollection(editing,body);
     else await adminApi.createCollection(body);
    }
   }else if(resource==="products"){
    if(editing){
     const body:Record<string,unknown>={name:draft.name.trim(),slug:draft.slug.trim(),description:draft.description.trim()};
     if(draft.categoryId)body.categoryId=Number(draft.categoryId);
     if(draft.brandId)body.brandId=Number(draft.brandId);
     await adminApi.updateProduct(editing,body);
     if(draft.imageUrl.trim())await adminApi.addProductImage(editing,{imageUrl:draft.imageUrl.trim(),altText:draft.altText.trim(),sortOrder:productImages.length,primary:draft.primaryImage==="true"});
    }else{
     if(!draft.name.trim()||!draft.categoryId)throw new Error("Product name and category ID are required.");
     const product=await adminApi.createProduct({name:draft.name.trim(),slug:draft.slug.trim(),categoryId:Number(draft.categoryId),brandId:draft.brandId?Number(draft.brandId):null,shortDescription:draft.description.trim(),description:draft.description.trim(),productType:"STANDARD",status:"DRAFT",featured:false});
     if(draft.imageUrl.trim())await adminApi.addProductImage(Number(product.id),{imageUrl:draft.imageUrl.trim(),altText:draft.altText.trim(),sortOrder:0,primary:true});
     if(draft.sku.trim()&&draft.variantName.trim()&&draft.price.trim()){
      await adminApi.createVariant(Number(product.id),{sku:draft.sku.trim(),variantName:draft.variantName.trim(),price:Number(draft.price),volumeMl:Number(draft.variantName.match(/\d+(?:\.\d+)?/)?.[0]||0)||null,initialStock:Number(draft.initialStock||0),isDefault:true,isActive:true});
     }
    }
   }else if(resource==="coupons"){
    const body={code:draft.name.trim().toUpperCase(),description:draft.description.trim(),discountType:draft.discountType,discountValue:Number(draft.discountValue),minimumOrderAmount:Number(draft.minimumOrderAmount||0),maximumDiscountAmount:draft.maximumDiscountAmount?Number(draft.maximumDiscountAmount):null,usageLimit:draft.usageLimit?Number(draft.usageLimit):null,perUserLimit:draft.perUserLimit?Number(draft.perUserLimit):null,startsAt:draft.startsAt||null,endsAt:draft.endsAt||null,active:true};
    if(editing)await adminApi.updateCoupon(editing,body);else await adminApi.createCoupon(body);
   }else if(resource==="banners"){
    const body={title:draft.name.trim(),subtitle:draft.description.trim(),imageUrl:draft.imageUrl.trim(),targetUrl:draft.targetUrl.trim()||null,placement:draft.placement,startsAt:draft.startsAt||null,endsAt:draft.endsAt||null,sortOrder:Number(draft.sortOrder||0),active:true};
    if(editing)await adminApi.updateBanner(editing,body);else await adminApi.createBanner(body);
   }else if(resource==="staff"){
    const roleCodes=draft.roleCodes.split(",").map(value=>value.trim().toUpperCase()).filter(Boolean);
    if(!editing||!roleCodes.length)throw new Error("Select at least one staff role.");
    await adminApi.updateStaffRoles(editing,roleCodes);
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
   else if(resource==="coupons")await adminApi.deactivateCoupon(Number(row.id));
   else if(resource==="banners")await adminApi.deactivateBanner(Number(row.id));
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
 async function updateCustomer(row:Row,status:string){
  try{await adminApi.updateCustomerStatus(Number(row.id),status);setNotice("Customer account status saved.");await load();}
  catch(reason){setError(reason instanceof Error?reason.message:"Customer status update failed. This action may require Admin permission.");}
 }
 async function requestRefund(row:Row){
  const amountText=window.prompt("Refund amount in "+String(row.currency||"BDT"),String(row.amount||""));
  if(amountText===null)return;
  const amount=Number(amountText);
  if(!Number.isFinite(amount)||amount<=0){setError("Enter a valid positive refund amount.");return;}
  const reason=window.prompt("Reason for manual refund review","");
  if(reason===null||!reason.trim()){setError("A refund reason is required.");return;}
  try{
   const key=typeof crypto!=="undefined"&&"randomUUID" in crypto?crypto.randomUUID():String(row.payment_id||row.id)+"-"+Date.now();
   await adminApi.requestRefund(Number(row.payment_id||row.id),amount,String(row.currency||"BDT"),reason.trim(),key);
   setNotice("Refund request queued for manual staff review. No money has been transferred.");await load();
  }catch(reason){setError(reason instanceof Error?reason.message:"Refund request could not be queued.");}
 }
 async function confirmPayment(row:Row){
  try{await adminApi.confirmCodPayment(Number(row.payment_id||row.id));setNotice("COD collection confirmation saved.");await load();}
  catch(reason){setError(reason instanceof Error?reason.message:"COD collection could not be confirmed.");}
 }
 async function decideReturn(row:Row,decision:"APPROVED"|"REJECTED"){
  try{await adminApi.decideReturn(Number(row.id),decision,"Reviewed from Admin Website");setNotice("Return request decision saved.");await load();}
  catch(reason){setError(reason instanceof Error?reason.message:"Return decision could not be saved.");}
 }
 async function moderateReview(row:Row,status:"APPROVED"|"REJECTED"){
  try{await adminApi.moderateReview(Number(row.id),status);setNotice("Review moderation saved.");await load();}
  catch(reason){setError(reason instanceof Error?reason.message:"Review moderation failed.");}
 }
 async function unsubscribeSubscriber(row:Row){
  try{await adminApi.unsubscribeNewsletter(Number(row.id));setNotice("Subscriber marked as unsubscribed.");await load();}
  catch(reason){setError(reason instanceof Error?reason.message:"Subscriber update failed.");}
 }
 async function updateContact(row:Row,status:string){
  try{await adminApi.updateContactStatus(Number(row.id),status);setNotice("Contact message status saved.");await load();}
  catch(reason){setError(reason instanceof Error?reason.message:"Contact update failed.");}
 }

 if(resource==="analytics")return <section className="space-y-6"><div><p className="text-xs uppercase tracking-[.2em] text-[var(--gold)]">Live analytics · database aggregates</p><h1 className="mt-2 text-3xl font-semibold">Store Analytics</h1><p className="mt-2 text-sm text-[var(--muted)]">Metrics are calculated by the backend from current store records.</p></div>{loading?<p className="muted">Loading analytics…</p>:error?<p className="form-error" role="alert">{error}</p>:analytics?<div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{Object.entries({"Active products":analytics.activeProducts,"Total orders":analytics.totalOrders,"Pending orders":analytics.pendingOrders,"Active customers":analytics.activeCustomers,"Paid revenue (BDT)":Number(analytics.paidRevenue).toLocaleString("en-BD"),"Low-stock variants":analytics.lowStockVariants,"New contact messages":analytics.newContactMessages,"Requested returns":analytics.requestedReturns,"Subscribed newsletter":analytics.subscribedNewsletter,"Pending payments":analytics.pendingPayments}).map(([label,value])=><article key={label} className="rounded-xl border border-[var(--line)] bg-[var(--panel)] p-5"><p className="text-xs text-[var(--muted)]">{label}</p><p className="mt-3 text-2xl font-semibold">{value}</p></article>)}</div>:<p className="muted">No analytics data returned.</p>}</section>;
 if(!supported)return <section className="rounded-2xl border border-[var(--line)] bg-[var(--panel)] p-8"><p className="text-xs uppercase tracking-[.2em] text-[var(--gold)]">Backend integration status</p><h1 className="mt-3 text-2xl font-semibold">{title}</h1><p className="mt-3 max-w-2xl text-sm leading-7 text-[var(--muted)]">{unsupported[section]||"This section has no corresponding backend endpoint yet."}</p><p className="mt-4 text-xs text-[#d2bd95]">No demo records are shown or changed here. This module remains pending until its secure backend API is implemented.</p></section>;

 return <div className="space-y-6">
  <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs uppercase tracking-[.22em] text-[var(--gold)]">Live database · {resource}</p><h1 className="mt-2 text-3xl font-semibold">{title}</h1><p className="mt-2 max-w-2xl text-sm leading-6 text-[var(--muted)]">Records below are retrieved from the authenticated LUXE backend API.</p></div><div className="flex gap-2"><button onClick={()=>void load()} disabled={loading} className="inline-flex items-center gap-2 rounded-lg border border-[var(--line)] px-3 py-2.5 text-sm"><RefreshCw size={15}/>Refresh</button>{["products","categories","brands","collections","inventory","coupons","banners"].includes(resource)&&<button onClick={()=>{setEditing(null);setFormOpen(v=>!v);}} className="inline-flex items-center gap-2 rounded-lg bg-[var(--gold)] px-4 py-2.5 text-sm font-semibold text-[#17130d]"><Plus size={16}/>{formOpen?"Close":"Add new"}</button>}</div></div>
  <div className="rounded-xl border border-[#365b43] bg-[#122017] px-4 py-3 text-xs leading-5 text-[#b9d8c0]">LIVE API MODE — changes are sent to the backend. Server-side permissions, validation and business rules remain authoritative.</div>
  {error&&<div className="rounded-lg border border-red-900 bg-red-950/40 px-4 py-3 text-sm text-red-200" role="alert">{error}</div>}
  {notice&&<div className="rounded-lg border border-[var(--line)] bg-[var(--panel)] px-4 py-3 text-sm text-[#c7d9c7]" role="status">{notice}</div>}
  {formOpen&&<form onSubmit={submit} className="rounded-2xl border border-[var(--line)] bg-[var(--panel)] p-5"><div className="mb-4 flex items-center justify-between"><h2 className="font-semibold">{editing?"Update database record":"Create database record"}</h2><button type="button" onClick={resetForm} aria-label="Close form"><X size={18}/></button></div>
   {["categories","brands","collections","products","coupons","banners"].includes(resource)&&<div className="grid gap-3 md:grid-cols-2">
    <label className="text-xs text-[var(--muted)]">Name *<input required value={draft.name} onChange={e=>setDraft(d=>({...d,name:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)]">Slug<input value={draft.slug} onChange={e=>setDraft(d=>({...d,slug:e.target.value}))} placeholder="Auto-generated when blank" className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)] md:col-span-2">Description<textarea value={draft.description} onChange={e=>setDraft(d=>({...d,description:e.target.value}))} rows={3} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    {resource==="products"&&<><label className="text-xs text-[var(--muted)] md:col-span-2">Product image URL (HTTPS)<input type="url" value={draft.imageUrl} onChange={e=>setDraft(d=>({...d,imageUrl:e.target.value}))} placeholder="https://res.cloudinary.com/…" className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Image alt text<input value={draft.altText} onChange={e=>setDraft(d=>({...d,altText:e.target.value}))} maxLength={255} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Set as primary image<select value={draft.primaryImage} onChange={e=>setDraft(d=>({...d,primaryImage:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"><option value="true">Yes</option><option value="false">No</option></select></label></>}
    {resource==="products"&&<><label className="text-xs text-[var(--muted)]">Category ID *<input required type="number" min="1" value={draft.categoryId} onChange={e=>setDraft(d=>({...d,categoryId:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Brand ID (optional)<input type="number" min="1" value={draft.brandId} onChange={e=>setDraft(d=>({...d,brandId:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>{!editing&&<><label className="text-xs text-[var(--muted)]">Initial variant SKU<input value={draft.sku} onChange={e=>setDraft(d=>({...d,sku:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Variant name / size<input value={draft.variantName} onChange={e=>setDraft(d=>({...d,variantName:e.target.value}))} placeholder="50ml" className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Variant price (BDT)<input type="number" min="0" step="0.01" value={draft.price} onChange={e=>setDraft(d=>({...d,price:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Initial stock<input type="number" min="0" step="1" value={draft.initialStock} onChange={e=>setDraft(d=>({...d,initialStock:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label></>}</>}
   </div>}
   {resource==="coupons"&&<div className="mt-4 grid gap-3 md:grid-cols-2">
    <label className="text-xs text-[var(--muted)]">Discount type<select value={draft.discountType} onChange={e=>setDraft(d=>({...d,discountType:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"><option value="PERCENTAGE">Percentage</option><option value="FIXED">Fixed amount (BDT)</option></select></label>
    <label className="text-xs text-[var(--muted)]">Discount value *<input required type="number" min="0.01" step="0.01" value={draft.discountValue} onChange={e=>setDraft(d=>({...d,discountValue:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)]">Minimum order (BDT)<input type="number" min="0" step="0.01" value={draft.minimumOrderAmount} onChange={e=>setDraft(d=>({...d,minimumOrderAmount:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)]">Maximum discount (optional)<input type="number" min="0.01" step="0.01" value={draft.maximumDiscountAmount} onChange={e=>setDraft(d=>({...d,maximumDiscountAmount:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)]">Total usage limit<input type="number" min="1" value={draft.usageLimit} onChange={e=>setDraft(d=>({...d,usageLimit:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)]">Per-customer limit<input type="number" min="1" value={draft.perUserLimit} onChange={e=>setDraft(d=>({...d,perUserLimit:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)]">Starts at<input type="datetime-local" value={draft.startsAt} onChange={e=>setDraft(d=>({...d,startsAt:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)]">Ends at<input type="datetime-local" value={draft.endsAt} onChange={e=>setDraft(d=>({...d,endsAt:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
   </div>}
   {resource==="banners"&&<div className="mt-4 grid gap-3 md:grid-cols-2">
    <label className="text-xs text-[var(--muted)] md:col-span-2">Image URL (HTTPS) *<input required type="url" value={draft.imageUrl} onChange={e=>setDraft(d=>({...d,imageUrl:e.target.value}))} placeholder="https://res.cloudinary.com/…" className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)]">Target URL<input type="url" value={draft.targetUrl} onChange={e=>setDraft(d=>({...d,targetUrl:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)]">Placement<select value={draft.placement} onChange={e=>setDraft(d=>({...d,placement:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"><option>HOME_HERO</option><option>HOME_PROMO</option><option>SHOP_TOP</option><option>COLLECTION</option></select></label>
    <label className="text-xs text-[var(--muted)]">Sort order<input type="number" value={draft.sortOrder} onChange={e=>setDraft(d=>({...d,sortOrder:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)]">Starts at<input type="datetime-local" value={draft.startsAt} onChange={e=>setDraft(d=>({...d,startsAt:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
    <label className="text-xs text-[var(--muted)]">Ends at<input type="datetime-local" value={draft.endsAt} onChange={e=>setDraft(d=>({...d,endsAt:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label>
   </div>}
   {resource==="staff"&&<div className="mt-4"><label className="text-xs text-[var(--muted)]">Staff roles (comma-separated) *<input required value={draft.roleCodes} onChange={e=>setDraft(d=>({...d,roleCodes:e.target.value}))} placeholder="MANAGER, INVENTORY_STAFF" className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><p className="mt-2 text-xs text-[var(--muted)]">Allowed roles: ADMIN, MANAGER, INVENTORY_STAFF. You cannot change your own roles.</p></div>}
   {resource==="products"&&editing!==null&&productImages.length>0&&<div className="mt-4 rounded-lg border border-[var(--line)] p-3 md:col-span-2"><p className="mb-2 text-xs uppercase tracking-wider text-[var(--muted)]">Existing product images</p>{productImages.map(image=><div key={String(image.id)} className="flex items-center justify-between gap-3 border-t border-[var(--line)] py-2 text-xs"><a href={String(image.image_url)} target="_blank" rel="noreferrer" className="max-w-[75%] truncate text-[var(--gold)]">{String(image.image_url)}</a><button type="button" onClick={async()=>{try{await adminApi.deleteProductImage(Number(image.id));setProductImages(items=>items.filter(item=>item.id!==image.id));}catch(reason){setError(reason instanceof Error?reason.message:"Could not delete product image.");}}} className="text-red-300">REMOVE</button></div>)}</div>}
   {resource==="inventory"&&<div className="grid gap-3 md:grid-cols-3"><label className="text-xs text-[var(--muted)]">Variant ID *<input required type="number" min="1" value={draft.categoryId} onChange={e=>setDraft(d=>({...d,categoryId:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Quantity change (+/-) *<input required type="number" step="1" value={draft.quantityChange} onChange={e=>setDraft(d=>({...d,quantityChange:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label><label className="text-xs text-[var(--muted)]">Reason / note *<input required maxLength={255} value={draft.note} onChange={e=>setDraft(d=>({...d,note:e.target.value}))} className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-2.5 text-sm text-white"/></label></div>}
   <button type="submit" disabled={saving} className="mt-5 inline-flex items-center gap-2 rounded-lg border border-[#80663e] px-4 py-2.5 text-sm text-[var(--gold)] disabled:opacity-50"><Check size={15}/>{saving?"SAVING…":editing?"SAVE CHANGES":"SAVE TO DATABASE"}</button>
   {resource==="products"&&!editing&&<p className="mt-3 text-xs text-[var(--muted)]">Products are created as DRAFT. Add a variant to establish a price and stock, then publish using the product status workflow. A variant is created only when SKU, variant name and price are all supplied.</p>}
  </form>}
  <section className="overflow-hidden rounded-2xl border border-[var(--line)] bg-[var(--panel)]"><div className="flex flex-col gap-3 border-b border-[var(--line)] p-4 sm:flex-row sm:items-center sm:justify-between"><label className="flex-1 text-xs text-[var(--muted)]"><span className="sr-only">Search records</span><div className="flex items-center gap-2 rounded-lg border border-[var(--line)] bg-[#100f0d] px-3"><Search size={15}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder={"Search "+title.toLowerCase()+"…"} className="w-full bg-transparent py-2.5 text-sm text-white outline-none"/></div></label><span className="text-xs text-[var(--muted)]">{filtered.length} loaded record(s)</span></div>
  {loading?<div className="px-6 py-16 text-center text-sm text-[var(--muted)]">Loading live records…</div>:filtered.length===0?<div className="px-6 py-16 text-center"><p className="font-medium">No live records found</p><p className="mt-2 text-sm text-[var(--muted)]">Try refreshing or changing the search.</p></div>:<div className="overflow-x-auto"><table className="w-full min-w-[720px] text-left text-sm"><thead className="bg-[#201d18] text-xs uppercase tracking-wider text-[var(--muted)]"><tr><th className="px-5 py-3">Record</th><th className="px-5 py-3">Details</th><th className="px-5 py-3">Status</th><th className="px-5 py-3">Value</th><th className="px-5 py-3">Actions</th></tr></thead><tbody>{filtered.map((row,index)=>{
   const id=String(row.id??row.variant_id??index);
   const name=String(row.name||row.product_name||row.orderNumber||row.full_name||row.code||row.title||row.product_name||row.email||((row.first_name||row.last_name)?String(row.first_name||"")+" "+String(row.last_name||""):"")||row.variant_name||row.subject||"Record");
   const detail=String(row.slug??row.category_name??row.brand_name??row.customerEmail??row.email??row.sku??row.order_number??"");
   const status=String(row.status??row.orderStatus??row.order_status??row.returnStatus??row.return_status??row.account_status??row.paymentStatus??row.payment_status??row.moderation_status??row.subscription_status??(row.is_active===false?"INACTIVE":"ACTIVE"));
   const value=String(row.price_min??row.discount_value??row.grandTotal??row.grand_total??row.amount??row.lifetime_value??row.available_quantity??row.quantity_on_hand??row.message??row.image_url??"—");
   return <tr key={id} className="border-t border-[var(--line)] hover:bg-white/[.02]"><td className="px-5 py-4"><div className="font-medium">{name}</div><div className="mt-1 text-[10px] text-[var(--muted)]">#{id}</div></td><td className="max-w-[280px] truncate px-5 py-4 text-[#c9c0b3]">{detail}</td><td className="px-5 py-4">{resource==="orders"?<select value={status} onChange={e=>void updateOrder(row,e.target.value)} className="rounded-lg border border-[var(--line)] bg-[#100f0d] px-2 py-1.5 text-xs text-white"><option value={status}>{status}</option>{["CONFIRMED","PROCESSING","SHIPPED","DELIVERED","CANCELLED"].filter(s=>s!==status).map(s=><option key={s}>{s}</option>)}</select>:resource==="contacts"?<select value={status} onChange={e=>void updateContact(row,e.target.value)} className="rounded-lg border border-[var(--line)] bg-[#100f0d] px-2 py-1.5 text-xs text-white">{["NEW","IN_PROGRESS","RESOLVED","SPAM"].map(s=><option key={s}>{s}</option>)}</select>:resource==="customers"&&canManageCustomers?<select value={status} onChange={e=>void updateCustomer(row,e.target.value)} className="rounded-lg border border-[var(--line)] bg-[#100f0d] px-2 py-1.5 text-xs text-white">{["ACTIVE","SUSPENDED"].map(s=><option key={s}>{s}</option>)}</select>:status}</td><td className="max-w-[240px] truncate px-5 py-4 text-[#e1d5c2]">{value}</td><td className="px-5 py-4"><div className="flex items-center gap-2">{["products","categories","brands","collections","coupons","banners","staff"].includes(resource)&&<><button title="Edit" aria-label={"Edit "+name} onClick={()=>startEdit(row)} className="rounded-md border border-[var(--line)] p-2 text-[var(--muted)] hover:text-white"><Pencil size={14}/></button><button title="Archive" aria-label={"Archive "+name} onClick={()=>void archive(row)} className="rounded-md border border-[var(--line)] p-2 text-[var(--muted)] hover:text-red-300"><Trash2 size={14}/></button>{resource==="products"&&status==="DRAFT"&&<button title="Publish" onClick={()=>void publishProduct(row)} className="rounded-md border border-[#655333] px-2 py-1 text-xs text-[var(--gold)]">PUBLISH</button>}</>}{resource==="payments"&&String(row.payment_method)==="CASH_ON_DELIVERY"&&status!=="PAID"&&<button onClick={()=>void confirmPayment(row)} className="rounded-md border border-[#655333] px-2 py-1 text-xs text-[var(--gold)]">CONFIRM COD</button>}{resource==="payments"&&(status==="PAID"||status==="PARTIALLY_REFUNDED")&&<button onClick={()=>void requestRefund(row)} className="rounded-md border border-[#655333] px-2 py-1 text-xs text-[var(--gold)]">REQUEST REFUND</button>}{resource==="returns"&&status==="REQUESTED"&&<><button onClick={()=>void decideReturn(row,"APPROVED")} className="rounded-md border border-[#655333] px-2 py-1 text-xs text-[var(--gold)]">APPROVE</button><button onClick={()=>void decideReturn(row,"REJECTED")} className="rounded-md border border-[var(--line)] px-2 py-1 text-xs text-red-300">REJECT</button></>}{resource==="reviews"&&status==="PENDING"&&<><button onClick={()=>void moderateReview(row,"APPROVED")} className="rounded-md border border-[#655333] px-2 py-1 text-xs text-[var(--gold)]">APPROVE</button><button onClick={()=>void moderateReview(row,"REJECTED")} className="rounded-md border border-[var(--line)] px-2 py-1 text-xs text-red-300">REJECT</button></>}{resource==="newsletter"&&status!=="UNSUBSCRIBED"&&<button onClick={()=>void unsubscribeSubscriber(row)} className="rounded-md border border-[var(--line)] px-2 py-1 text-xs text-red-300">UNSUBSCRIBE</button>}</div></td></tr>;
  })}</tbody></table></div>}
  <div className="flex items-center justify-between border-t border-[var(--line)] px-5 py-3 text-xs text-[var(--muted)]"><span>Source: LUXE backend REST API</span><span>Server is authoritative</span></div></section>
 </div>;
}
