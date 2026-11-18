"use client";
import Link from "next/link";
import Image from "next/image";
import {useRouter,usePathname} from "next/navigation";
import {useEffect,useMemo,useState,createContext,useContext} from "react";
import type {ReactNode,FormEvent} from "react";
import {ShoppingBag,Search,UserRound,Heart,Menu,X,ArrowRight,Instagram,Facebook,MapPin,Minus,Plus,Trash2,CheckCircle2} from "lucide-react";
import type {Product} from "@/lib/products";
import {money,productPrice,productStock} from "@/lib/products";
import {getCatalog,getProduct,getCart,getWishlist,addCartItem,updateCartItem,removeCartItem,addWishlistItem,removeWishlistItem} from "@/lib/commerce";
import {getCurrentUser} from "@/lib/auth";
import {toStoreProduct} from "@/lib/product-adapter";
import {siteContact, contactFormMode} from "@/lib/site-config";
import {brandContent} from "@/lib/brand-content";
import {signIn, registerAccount, getCurrentUser, signOut} from "@/lib/auth";
import {getAddresses,createAddress,deleteAddress,setDefaultAddress,previewCheckout,placeOrder,requestCodPayment,type Address,type CheckoutQuote} from "@/lib/commerce";

type CartLine = { id: string; qty: number; size?: string; cartItemId?: number; variantId?: number };
type ResolvedCartLine = CartLine & { p: Product };
type DemoOrderItem = { id: string; name: string; slug: string; size: string; qty: number; unitPrice: number; lineTotal: number; image: string };
type DemoOrder = {
 id: string;
 customer: { name: string; email: string; phone: string; alternativePhone: string; division: string; district: string; area: string; address: string; postalCode: string; instructions: string };
 items: DemoOrderItem[];
 subtotal: number; discount: number; discountCode: string | null; delivery: number; total: number;
 payment: "bkash" | "nagad" | "cod"; createdAt: string; status: string; statusStep: number;
};
const isDemoOrderItem = (value: unknown): value is DemoOrderItem => {
 if (!value || typeof value !== "object") return false;
 const item = value as Record<string, unknown>;
 return typeof item.id === "string" && typeof item.name === "string" && typeof item.slug === "string" && typeof item.size === "string" && Number.isInteger(item.qty) && Number(item.qty) > 0 && typeof item.unitPrice === "number" && typeof item.lineTotal === "number" && typeof item.image === "string";
};
const isDemoOrder = (value: unknown): value is DemoOrder => {
 if (!value || typeof value !== "object") return false;
 const order = value as Record<string, unknown>;
 const customer = order.customer;
 if (!customer || typeof customer !== "object") return false;
 const customerRecord = customer as Record<string, unknown>;
 const customerFields = ["name", "email", "phone", "alternativePhone", "division", "district", "area", "address", "postalCode", "instructions"];
 return typeof order.id === "string" && customerFields.every((field) => typeof customerRecord[field] === "string") && Array.isArray(order.items) && order.items.every(isDemoOrderItem) && typeof order.subtotal === "number" && typeof order.discount === "number" && (order.discountCode === null || typeof order.discountCode === "string") && typeof order.delivery === "number" && typeof order.total === "number" && ["bkash", "nagad", "cod"].includes(String(order.payment)) && typeof order.createdAt === "string" && typeof order.status === "string" && Number.isInteger(order.statusStep) && Number(order.statusStep) >= 0 && Number(order.statusStep) <= 5;
};
type StoreContextValue = {
 products: Product[];
 catalogLoading: boolean;
 catalogError: string;
 cart: CartLine[];
 wish: string[];
 add: (id: string, size?: string, qty?: number) => void;
 remove: (id: string, size?: string) => void;
 setQty: (id: string, size: string | undefined, qty: number) => void;
 toggleWish: (id: string) => void;
 clearCart: () => void;
 count: number;
 notify: (message: string) => void;
 toast: string | null;
};
const StoreContext = createContext<StoreContextValue | null>(null);
function mapCart(items: Awaited<ReturnType<typeof getCart>>["items"]): CartLine[] {
 return items.map(item => ({
  id:String(item.product_id),qty:Number(item.quantity),
  size:String(item.variant_name||"100ml"),cartItemId:Number(item.item_id),variantId:Number(item.variant_id)
 }));
}
export function StoreProvider({ children }: { children: ReactNode }) {
 const [products,setProducts]=useState<Product[]>([]);
 const [catalogLoading,setCatalogLoading]=useState(true);
 const [catalogError,setCatalogError]=useState("");
 const [cart,setCart]=useState<CartLine[]>([]);
 const [wish,setWish]=useState<string[]>([]);
 const [toast,setToast]=useState<string|null>(null);
 const notify=(message:string)=>setToast(message);
 useEffect(()=>{
  let active=true;
  getCatalog({page:0,size:100,sort:"newest"}).then(result=>{
   if(active)setProducts(result.items.map((item,index)=>toStoreProduct(item,index)));
  }).catch(error=>{
   if(active)setCatalogError(error instanceof Error?error.message:"The live product catalogue could not be loaded.");
  }).finally(()=>{if(active)setCatalogLoading(false);});
  return ()=>{active=false;};
 },[]);
 useEffect(()=>{
  let active=true;
  getCurrentUser().then(async()=>{
   const [serverCart,serverWishlist]=await Promise.all([getCart(),getWishlist()]);
   if(active){setCart(mapCart(serverCart.items));setWish(serverWishlist.items.map(item=>String(item.product_id)));}
  }).catch(()=>{if(active){setCart([]);setWish([]);}});
  return ()=>{active=false;};
 },[]);
 useEffect(()=>{if(!toast)return;const timer=window.setTimeout(()=>setToast(null),2600);return()=>window.clearTimeout(timer);},[toast]);
 const add=(id:string,size="100ml",qty=1)=>{
  void (async()=>{
   const product=products.find(item=>item.id===id);
   if(!product||!Number.isInteger(qty)||qty<1)return;
   try{
    const detail=await getProduct(product.slug);
    const variants=detail.variants||[];
    const target=variants.find(v=>String(v.variant_name||v.variantName||"").toLowerCase().replace(/\\s/g,"")===size.toLowerCase().replace(/\\s/g,""))
      ||variants.find(v=>Number(v.volume_ml)===Number(size.replace(/[^0-9]/g,"")));
    if(!target)throw new Error("The selected bottle size is not available for this product.");
    const updated=await addCartItem(Number(target.id),qty);
    setCart(mapCart(updated.items));
    notify(product.name+" added to your server cart.");
   }catch(error){notify(error instanceof Error?error.message:"Could not add this product to your cart.");}
  })();
 };
 const remove=(id:string,size?:string)=>{
  const target=cart.find(line=>line.id===id&&(!size||line.size===size));
  if(!target?.cartItemId)return;
  void removeCartItem(target.cartItemId).then(updated=>setCart(mapCart(updated.items))).catch(error=>notify(error instanceof Error?error.message:"Could not remove this cart item."));
 };
 const setQty=(id:string,size:string|undefined,qty:number)=>{
  const target=cart.find(line=>line.id===id&&(!size||line.size===size));
  if(!target?.cartItemId)return;
  if(qty<1){remove(id,size);return;}
  void updateCartItem(target.cartItemId,qty).then(updated=>setCart(mapCart(updated.items))).catch(error=>notify(error instanceof Error?error.message:"Could not update cart quantity."));
 };
 const toggleWish=(id:string)=>{
  const exists=wish.includes(id);
  const action=exists?removeWishlistItem:addWishlistItem;
  void action(Number(id)).then(updated=>setWish(updated.items.map(item=>String(item.product_id)))).catch(error=>notify(error instanceof Error?error.message:"Please sign in to manage your wishlist."));
 };
 const clearCart=()=>setCart([]);
 const count=cart.reduce((total,line)=>total+line.qty,0);
 return <StoreContext.Provider value={{products,catalogLoading,catalogError,cart,wish,add,remove,setQty,toggleWish,clearCart,count,notify,toast}}>{children}</StoreContext.Provider>;
}
export function useStore(): StoreContextValue {
 const value = useContext(StoreContext);
 if (!value) throw new Error("useStore must be used inside StoreProvider");
 return value;
}

export function Header(){
 const {count,wish}=useStore(); const [open,setOpen]=useState(false); const pathname=usePathname();
 useEffect(()=>{setOpen(false);},[pathname]);
 useEffect(()=>{if(!open)return;const onKeyDown=(event:KeyboardEvent)=>{if(event.key==="Escape")setOpen(false);};window.addEventListener("keydown",onKeyDown);return()=>window.removeEventListener("keydown",onKeyDown);},[open]);
 const navItems=[{label:"Home",href:"/"},{label:"Shop",href:"/shop"},{label:"Collections",href:"/collections"},{label:"Best Sellers",href:"/best-sellers"},{label:"New Arrivals",href:"/new-arrivals"},{label:"Our Story",href:"/about"},{label:"Contact",href:"/contact"}];
 return <header className="header"><div className="announcement">{siteContact.announcement}</div><div className="nav wrap">
  <button type="button" className="icon-btn mobile-menu" onClick={()=>setOpen(!open)} aria-label={open?"Close navigation menu":"Open navigation menu"} aria-expanded={open} aria-controls="primary-navigation">{open?<X/>:<Menu/>}</button>
  <Link href="/" className="brand"><span className="brand-mark">✳</span><span>LUXE</span><small>PARFUMS</small></Link>
  <nav id="primary-navigation" aria-label="Primary navigation" className={open?"nav-links show":"nav-links"}>{navItems.map(item=>{const active=item.href==="/"?pathname==="/":pathname===item.href||pathname.startsWith(item.href+"/");return <Link key={item.href} onClick={()=>setOpen(false)} href={item.href} className={active?"active":undefined} aria-current={active?"page":undefined}>{item.label}</Link>})}</nav>
  <div className="nav-actions"><Link className="icon-btn" href="/search" aria-label="Search"><Search/></Link><Link className="icon-btn" href="/wishlist" aria-label={`Wishlist, ${wish.length} saved items`}><Heart/><i>{wish.length}</i></Link><Link className="icon-btn" href="/account" aria-label="Account"><UserRound/></Link><Link className="icon-btn bag" href="/cart" aria-label="Cart"><ShoppingBag/><i>{count}</i></Link></div>
 </div></header>
}
export function Footer(){
 const [subscribed,setSubscribed]=useState(false);
 const socialLinks=[{label:"Instagram",url:siteContact.social.instagram,icon:<Instagram/>},{label:"Facebook",url:siteContact.social.facebook,icon:<Facebook/>},{label:"TikTok",url:siteContact.social.tiktok},{label:"YouTube",url:siteContact.social.youtube}].filter(item=>item.url);
 return <footer className="footer"><div className="wrap footer-grid">
  <div><Link href="/" className="brand footer-brand"><span className="brand-mark">✳</span><span>LUXE</span><small>PERFUME</small></Link><p className="muted">Fragrance is the art of leaving a beautiful memory.</p><div className="social" aria-label="Official social media links">{socialLinks.length?socialLinks.map(item=><a key={item.label} href={item.url} target="_blank" rel="noreferrer" aria-label={item.label}>{item.icon||item.label}</a>):<span className="muted small">Official social links will appear here when configured.</span>}</div><form className="footer-newsletter" onSubmit={event=>{event.preventDefault();setSubscribed(true);event.currentTarget.reset();}}><label htmlFor="footer-newsletter-email">THE LUXE LETTER</label><div><input id="footer-newsletter-email" type="email" required placeholder="Email address"/><button type="submit" aria-label="Subscribe to newsletter"><ArrowRight size={16}/></button></div>{subscribed&&<small role="status">Demo subscription noted on this page only. No email was sent or stored.</small>}</form></div>
  <div><h4>EXPLORE</h4><Link href="/shop">All perfumes</Link><Link href="/collections">Collections</Link><Link href="/new-arrivals">New arrivals</Link><Link href="/best-sellers">Best sellers</Link><Link href="/wishlist">Wishlist</Link></div>
  <div><h4>INFORMATION</h4><Link href="/about">Our story</Link><Link href="/contact">Contact</Link><Link href="/faq">FAQs</Link><Link href="/shipping-policy">Shipping policy</Link><Link href="/return-refund-policy">Returns & refunds</Link><Link href="/privacy-policy">Privacy policy</Link><Link href="/terms-conditions">Terms & conditions</Link></div>
  <div><h4>GET IN TOUCH</h4><p>{siteContact.businessName}</p><p>{siteContact.businessAddress || "Demo address — add your verified business address before launch."}</p>{siteContact.phone&&<a href={`tel:${siteContact.phone}`}>{siteContact.phone}</a>}{siteContact.supportEmail&&<a href={`mailto:${siteContact.supportEmail}`}>{siteContact.supportEmail}</a>}<Link href="/contact">Contact information <ArrowRight size={13}/></Link></div>
 </div><div className="wrap copyright"><span>© 2026 LUXE PERFUME. All rights reserved.</span><span><Link href="/terms-conditions">Terms</Link> · <Link href="/privacy-policy">Privacy</Link> · <Link href="/shipping-policy">Shipping</Link> · Payment demos: bKash · Nagad · COD</span></div></footer>
}
const routeNames:Record<string,string>={shop:'Shop',collections:'Collections','best-sellers':'Best Sellers','new-arrivals':'New Arrivals',products:'Shop',search:'Search',cart:'Shopping Bag',checkout:'Checkout','order-success':'Order Confirmation',account:'My Account',login:'Sign In','my-orders':'My Orders',orders:'Orders','track-order':'Track Order',wishlist:'Wishlist',about:'Our Story',contact:'Contact',faq:'FAQ','shipping-policy':'Shipping Policy','return-refund-policy':'Return & Refund Policy','privacy-policy':'Privacy Policy','terms-conditions':'Terms & Conditions',policies:'Policies'};
export function Breadcrumb(){const {products}=useStore();const pathname=usePathname();const parts=pathname.split('/').filter(Boolean);if(!parts.length)return null;const crumbs=parts.map((part,index)=>{const decoded=decodeURIComponent(part);let label=routeNames[part]||decoded.replace(/-/g,' ').replace(/\b\w/g,c=>c.toUpperCase());if(index===1&&parts[0]==='products'){label=products.find(product=>product.slug===decoded)?.name||label;}if(index===1&&parts[0]==='collections'){label=products.find(product=>product.collection.toLowerCase()===decoded.toLowerCase())?.collection||label;}return {label,href:part==='products'&&index===0?'/shop':'/'+parts.slice(0,index+1).join('/')};});return <nav aria-label="Breadcrumb" className="breadcrumb wrap"><Link href="/">Home</Link>{crumbs.map((crumb,index)=><span className="breadcrumb-part" key={crumb.href}><span aria-hidden="true">/</span>{index===crumbs.length-1?<span aria-current="page">{crumb.label}</span>:<Link href={crumb.href}>{crumb.label}</Link>}</span>)}</nav>}
export function Shell({children}:{children:ReactNode}){const {toast}=useStore();return <><Header/><Breadcrumb/><div id="main-content" tabIndex={-1}>{children}</div><Footer/>{toast&&<div className="store-toast" role="status" aria-live="polite" aria-atomic="true">{toast}</div>}</>}
export function ProductCard({p}:{p:Product}){
 const {add,wish,toggleWish,cart,notify}=useStore(); const [quickOpen,setQuickOpen]=useState(false); const [size,setSize]=useState("50ml"); const inCart=cart.filter(line=>line.id===p.id&&(line.size||"100ml")===size).reduce((total,line)=>total+line.qty,0); const available= Math.max(0,productStock(p,size)-inCart); const cardAvailable=Math.max(0,productStock(p,"50ml")-cart.filter(line=>line.id===p.id&&(line.size||"100ml")==="50ml").reduce((total,line)=>total+line.qty,0));
 return <article className="product-card"><div className="product-image"><Link href={`/products/${p.slug}`}><Image src={p.image} alt={p.name} fill sizes="(max-width: 650px) 50vw, (max-width: 900px) 50vw, 25vw"/></Link>{p.badge&&<span className="product-badge">{p.badge}</span>}<button className={"wish-btn "+(wish.includes(p.id)?"active":"")} onClick={()=>{const wasSaved=wish.includes(p.id);toggleWish(p.id);notify(wasSaved?"Removed from wishlist":"Added to wishlist");}} aria-label={wish.includes(p.id)?"Remove from wishlist":"Add to wishlist"} aria-pressed={wish.includes(p.id)}><Heart/></button><button className="quick-add" onClick={()=>setQuickOpen(true)}>QUICK VIEW <Plus size={14}/></button></div><div className="product-info"><span className="eyebrow">{p.brand} · {p.collection}</span><Link href={`/products/${p.slug}`} className="product-name">{p.name}</Link><span className="notes">{p.notes}</span><div className="rating product-rating">★★★★★ <span>{p.rating.toFixed(1)} ({p.reviewCount})</span></div><div className="price">100ml · {money(p.price)} {p.oldPrice&&<del>{money(p.oldPrice)}</del>}</div><button type="button" className="card-add-btn" disabled={cardAvailable<1} onClick={()=>{add(p.id,"50ml");notify(`${p.name} · 50ml added to bag`);}}>{cardAvailable>0?"ADD TO BAG · 50ML":"OUT OF STOCK"}</button></div>{quickOpen&&<div className="quick-modal-backdrop" role="presentation" onMouseDown={e=>{if(e.target===e.currentTarget)setQuickOpen(false)}}><section className="quick-modal" role="dialog" aria-modal="true" aria-label={`${p.name} quick view`} tabIndex={-1} onKeyDown={event=>{if(event.key==="Escape")setQuickOpen(false);}}><button className="quick-close" autoFocus onClick={()=>setQuickOpen(false)} aria-label="Close quick view"><X/></button><Image src={p.image} alt={p.name} width={600} height={700}/><div className="quick-modal-copy"><span className="eyebrow gold">{p.collection} COLLECTION</span><h2>{p.name}</h2><div className="rating">★★★★★ <span>{p.rating.toFixed(1)} / 5 · {p.reviewCount} demo reviews</span></div><p>{p.description}</p><div className="size-picker"><span className="eyebrow gold">CHOOSE SIZE</span><div className="size-options">{["30ml","50ml","100ml"].map(v=><button key={v} className={size===v?"size-option selected":"size-option"} onClick={()=>setSize(v)}>{v}</button>)}</div></div><strong className="detail-price">{money(productPrice(p,size))} {p.oldPrice&&<del>{money(productPrice({...p,price:p.oldPrice},size))}</del>}</strong><p className="muted small">{available>0?`${available} more available in ${size} demo stock`:"No additional stock available for this size"}</p><button className="gold-btn wide" disabled={available<1} onClick={()=>{add(p.id,size);notify(`${p.name} · ${size} added to bag`);setQuickOpen(false)}}>ADD TO BAG</button><Link className="outline-btn wide" href={`/products/${p.slug}`} onClick={()=>setQuickOpen(false)}>VIEW FULL DETAILS</Link></div></section></div>}</article>
}
export function HomePage(){
 const {products,catalogLoading,catalogError}=useStore();
 const [newsletterSent,setNewsletterSent]=useState(false);
 const trendingProducts=[...products].sort((a,b)=>b.rating-a.rating || b.reviewCount-a.reviewCount).slice(0,4);
 return <>
 <section className="hero"><div className="hero-bg"><Image src="https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?auto=format&fit=crop&w=2200&q=90" alt="" aria-hidden="true" fill priority sizes="100vw"/></div><div className="wrap hero-inner"><div className="hero-copy"><span className="eyebrow gold">BEYOND SCENT, AN EXPERIENCE</span><h1>Timeless Luxury,<br/>Uniquely <em>Yours</em></h1><p>Explore a curated fragrance edit with clear scent notes, bottle sizes and a premium shopping experience.</p><Link className="outline-btn" href="/collections">DISCOVER COLLECTION <ArrowRight/></Link><div className="scent-tags" aria-label="Shop by fragrance family"><Link href="/collections/floral">FLORAL</Link><Link href="/collections/woody">WOODY</Link><Link href="/collections/amber">AMBER</Link><Link href="/collections/citrus">CITRUS</Link></div></div><div className="hero-product"><Image src={products[0].image} alt="Featured LUXE perfume bottle" fill priority sizes="(max-width: 650px) 65vw, 35vw"/></div><div className="hero-note"><span>FEATURED FRAGRANCE</span><h3>{products[0].name}</h3><p>Distinctive. Elegant. Unforgettable.</p><Link href={`/products/${products[0].slug}`}>EXPLORE <ArrowRight size={15}/></Link></div></div></section>
 <section className="section wrap"><div className="section-intro"><span className="eyebrow gold">EXPLORE THE WORLD OF LUXE</span><h2>Curated Collections,<br/><em>Timeless Scents</em></h2><p>Discover Signature, Lumière, Oud and Fresh edits across the LUXE fragrance catalogue.</p></div><div className="collection-grid">{["Signature","Lumière","Oud","Fresh"].map((name,index)=><Link href={`/collections/${encodeURIComponent(name.toLowerCase())}`} className="collection-tile" key={name}><Image src={products[index%products.length].image} alt={`${name} perfume collection`} fill sizes="(max-width: 650px) 50vw, 25vw"/><div><h3>{name}</h3><span>COLLECTION <ArrowRight size={14}/></span></div></Link>)}</div></section>
 <section className="craft-band"><div className="wrap craft-inner"><div><span className="eyebrow gold">THE LUXE PROMISE</span><h2>Thoughtfully Chosen,<br/><em>Personally Yours</em></h2></div><p>Explore fragrance notes, sizes and collection details to find a scent that feels right for you. Brand sourcing and craftsmanship claims will be added when verified.</p><Link className="outline-btn" href="/about">LEARN MORE <ArrowRight/></Link></div></section>
 <section className="section wrap"><div className="center-heading"><span className="eyebrow gold">THE LUXE COLLECTION</span><h2>Crafted for Every <em>Essence</em></h2><p>Explore the fragrances in our curated demo catalogue.</p></div><div className="product-grid">{products.slice(0,4).map(product=><ProductCard key={product.id} p={product}/>)}</div><div className="center"><Link className="outline-btn" href="/shop">VIEW ALL PERFUMES <ArrowRight/></Link></div></section>
 <section className="section wrap"><div className="center-heading"><span className="eyebrow gold">RECENTLY DISCOVERED</span><h2>New <em>Arrivals</em></h2><p>Meet the latest additions to the LUXE fragrance edit.</p></div><div className="product-grid">{products.filter(product=>product.badge?.toLowerCase()==="new").slice(0,4).map(product=><ProductCard key={product.id} p={product}/>)}</div><div className="center"><Link className="outline-btn" href="/new-arrivals">EXPLORE NEW ARRIVALS <ArrowRight/></Link></div></section>
 <section className="section wrap"><div className="center-heading"><span className="eyebrow gold">MOST LOVED</span><h2>Best <em>Sellers</em></h2><p>Featured favourites from the current demo catalogue.</p></div><div className="product-grid">{products.filter(product=>product.badge?.toLowerCase().includes("best")||product.rating>=4.8).slice(0,4).map(product=><ProductCard key={product.id} p={product}/>)}</div><div className="center"><Link className="outline-btn" href="/best-sellers">VIEW BEST SELLERS <ArrowRight/></Link></div></section>
 <section className="section wrap"><div className="center-heading"><span className="eyebrow gold">POPULAR IN THE COLLECTION</span><h2>Trending <em>Fragrances</em></h2><p>Explore highly rated items in this sample catalogue. Ratings are demo catalogue data, not verified customer testimonials.</p></div><div className="product-grid">{trendingProducts.map(product=><ProductCard key={product.id} p={product}/>)}</div><div className="center"><Link className="outline-btn" href="/shop">DISCOVER ALL FRAGRANCES <ArrowRight/></Link></div></section>
 <section className="promo-strip"><div className="wrap"><span className="eyebrow gold">A LITTLE EXTRA LUXURY · DEMO OFFER</span><h2>Complimentary delivery on orders over <em>৳8,000</em></h2><Link className="outline-btn" href="/shop">SHOP THE COLLECTION <ArrowRight/></Link></div></section>
 <section className="section wrap"><div className="center-heading"><span className="eyebrow gold">NOTES FROM THE LUXE COMMUNITY</span><h2>Customer <em>Impressions</em></h2><p>Demo testimonial slots — publish only genuine customer feedback with permission.</p></div><div className="testimonial-grid">{brandContent.testimonials.map((item)=><article key={item.slot}><span className="eyebrow gold">{item.slot}</span><p>{item.quote}</p><small>{item.attribution}</small></article>)}</div></section>
 <section className="story-banner"><div className="story-image"><Image src={brandContent.campaignImageUrl} alt={brandContent.campaignImageAlt} fill sizes="100vw"/></div><div className="story-copy"><span className="eyebrow gold">THE LUXE CAMPAIGN</span><h2>Timeless Elegance,<br/>Unmistakably <em>You.</em></h2><p>A world of fragrance made personal.</p><Link className="outline-btn" href="/about">DISCOVER OUR STORY <ArrowRight/></Link></div></section>
 <section className="newsletter"><div className="wrap newsletter-inner"><div><span className="eyebrow gold">THE LUXE LETTER</span><h2>A world of scent, <em>in your inbox.</em></h2><p>Be first to discover new fragrances and store announcements.</p></div><form onSubmit={event=>{event.preventDefault();setNewsletterSent(true);event.currentTarget.reset();}}><input type="email" placeholder="Your email address" aria-label="Your email address" required/><button className="gold-btn" type="submit">SUBSCRIBE <ArrowRight/></button>{newsletterSent&&<small className="success-text" role="status">Demo subscription noted on this page only. No email was sent or stored.</small>}</form></div></section>
 </>;
}
export function ShopPage(){
 const {products,catalogLoading,catalogError}=useStore();
 const [q,setQ]=useState("");const [category,setCategory]=useState("All");const [brand,setBrand]=useState("All");const [gender,setGender]=useState("All");const [family,setFamily]=useState("All");const [availability,setAvailability]=useState("All");const [priceRange,setPriceRange]=useState("All");const [sort,setSort]=useState("featured");const [limit,setLimit]=useState(12);
 const reset=()=>{setQ("");setCategory("All");setBrand("All");setGender("All");setFamily("All");setAvailability("All");setPriceRange("All");setSort("featured");setLimit(12)};
 const list=useMemo(()=>{const a=products.filter(p=>{const text=(p.name+" "+p.brand+" "+p.gender+" "+p.notes+" "+p.collection+" "+p.fragranceFamily).toLowerCase();const price= p.price;return (category==="All"||p.collection===category)&&(brand==="All"||p.brand===brand)&&(gender==="All"||p.gender===gender)&&(family==="All"||p.fragranceFamily===family)&&(availability==="All"||(availability==="In stock"?p.stock>0:p.stock===0))&&(priceRange==="All"||(priceRange==="Under ৳5,000"?price<5000:priceRange==="৳5,000–৳7,000"?price>=5000&&price<=7000:price>7000))&&text.includes(q.trim().toLowerCase())});
 if(sort==="low")a.sort((x,y)=>x.price-y.price);if(sort==="high")a.sort((x,y)=>y.price-x.price);if(sort==="newest")a.sort((x,y)=>Number(y.badge?.toLowerCase()==="new")-Number(x.badge?.toLowerCase()==="new"));if(sort==="best")a.sort((x,y)=>Number(y.badge?.toLowerCase().includes('best')||y.rating>=4.8)-Number(x.badge?.toLowerCase().includes('best')||x.rating>=4.8));if(sort==="rating")a.sort((x,y)=>y.rating-x.rating);return a},[q,category,brand,gender,family,availability,priceRange,sort]);
 return <main className="page wrap"><div className="page-heading"><span className="eyebrow gold">THE FRAGRANCE EDIT</span><h1>Discover Your <em>Signature</em></h1><p>Explore scents that stay with you.</p></div><div className="shop-tools"><input value={q} onChange={e=>{setQ(e.target.value);setLimit(12)}} placeholder="Search name, brand or notes..." aria-label="Search products"/><select aria-label="Filter category" value={category} onChange={e=>setCategory(e.target.value)}><option value="All">All categories</option>{[...new Set(products.map(p=>p.collection))].map(x=><option key={x}>{x}</option>)}</select><select aria-label="Filter brand" value={brand} onChange={e=>setBrand(e.target.value)}><option value="All">All brands</option>{[...new Set(products.map(p=>p.brand))].map(x=><option key={x}>{x}</option>)}</select><select aria-label="Filter gender" value={gender} onChange={e=>setGender(e.target.value)}><option value="All">All genders</option>{[...new Set(products.map(p=>p.gender))].map(x=><option key={x}>{x}</option>)}</select><select aria-label="Filter fragrance family" value={family} onChange={e=>setFamily(e.target.value)}><option value="All">All fragrance families</option>{[...new Set(products.map(p=>p.fragranceFamily))].map(x=><option key={x}>{x}</option>)}</select><select aria-label="Filter price range" value={priceRange} onChange={e=>setPriceRange(e.target.value)}><option value="All">All prices</option><option>Under ৳5,000</option><option>৳5,000–৳7,000</option><option>Above ৳7,000</option></select><select aria-label="Filter stock" value={availability} onChange={e=>setAvailability(e.target.value)}><option value="All">All availability</option><option>In stock</option><option>Out of stock</option></select><select aria-label="Sort products" value={sort} onChange={e=>setSort(e.target.value)}><option value="featured">Featured</option><option value="newest">Newest</option><option value="low">Price: low to high</option><option value="high">Price: high to low</option><option value="best">Best selling</option><option value="rating">Highest rated</option></select><button className="text-btn" type="button" onClick={reset}>CLEAR FILTERS</button></div><p className="muted">{list.length} fragrances</p>{list.length?<><div className="product-grid">{list.slice(0,limit).map(p=><ProductCard key={p.id} p={p}/>)}</div>{limit<list.length&&<div className="center"><button className="outline-btn" type="button" onClick={()=>setLimit(v=>v+12)}>LOAD MORE</button></div>}</>:<div className="empty-state"><Search size={36}/><h2>No fragrances found</h2><p>Try changing your search or filters.</p><button className="outline-btn" type="button" onClick={reset}>CLEAR FILTERS</button></div>}</main>;
}

export function CollectionsPage(){const {products}=useStore();const descriptions:Record<string,string>={Signature:"A considered edit of versatile, distinctive signature scents.",Lumière:"Bright, luminous compositions with an elegant character.",Oud:"Deep woods and warm, richly layered fragrance profiles.",Fresh:"Clean, airy and refreshing notes for everyday wear.",Floral:"Expressive bouquets built around graceful floral notes.",Woody:"Grounded compositions featuring woods, spice and soft warmth.",Amber:"Warm, smooth scents with amber and comforting accords.",Citrus:"Fresh citrus-led fragrances with a bright, uplifting profile."};const collections=Object.keys(descriptions);return <main className="page wrap"><div className="page-heading"><span className="eyebrow gold">SCENT STORIES</span><h1>Our <em>Collections</em></h1><p>Find the fragrance family that feels like you.</p></div><div className="collection-page-grid">{collections.map((c,i)=>{const product=products.find(p=>p.collection===c)||products[i%products.length];return <Link href={`/collections/${encodeURIComponent(c.toLowerCase())}`} className="collection-large" key={c}><Image src={product.image} alt={`${c} perfume collection`} fill sizes="(max-width: 650px) 50vw, 33vw"/><div><span className="eyebrow">THE LUXE EDIT</span><h2>{c}</h2><p>{descriptions[c]}</p><span>EXPLORE COLLECTION <ArrowRight size={15}/></span></div></Link>})}</div></main>}
export function ProductPage({slug}:{slug:string}){
 const {products}=useStore();
 const p=products.find(x=>x.slug===slug); const {add,toggleWish,wish,cart,notify}=useStore(); const router=useRouter(); const [size,setSize]=useState("50ml"); const [qty,setQty]=useState(1); const [activeImage,setActiveImage]=useState(0); const [recent,setRecent,recentReady]=useSaved<string[]>("luxe-recently-viewed",[],isStringList);
 const viewedProductId=p?.id;
 useEffect(()=>{if(viewedProductId&&recentReady)setRecent((old)=>[viewedProductId,...old.filter(id=>id!==viewedProductId)].slice(0,8));},[viewedProductId,recentReady,setRecent]);
 if(!p)return <main className="page wrap"><h1>Fragrance not found</h1><Link href="/shop">Return to shop</Link></main>;
 const images=p.images?.length?p.images:[p.image]; const price=productPrice(p,size); const related=products.filter(x=>x.id!==p.id&&(x.collection===p.collection||x.fragranceFamily===p.fragranceFamily)).slice(0,4); const recentProducts=recent.filter(id=>id!==p.id).map(id=>products.find(x=>x.id===id)).filter(Boolean) as Product[];
 const selectedStock=productStock(p,size); const alreadyInCart=cart.filter(line=>line.id===p.id&&(line.size||"100ml")===size).reduce((total,line)=>total+line.qty,0); const remainingStock=Math.max(0,selectedStock-alreadyInCart); const canAdd=qty<=remainingStock; const buyNow=()=>{if(!canAdd)return;add(p.id,size,qty);notify(`${p.name} · ${size} added to bag`);router.push('/checkout')};
 return <main className="page wrap"><div className="detail-grid"><div className="detail-gallery"><div className="detail-image"><Image src={images[activeImage]||p.image} alt={`${p.name} product view ${activeImage+1}`} fill priority sizes="(max-width: 900px) 100vw, 50vw"/></div><div className="gallery-thumbnails">{images.map((src,i)=><button type="button" key={src} className={activeImage===i?"gallery-thumb active":"gallery-thumb"} onClick={()=>setActiveImage(i)} aria-label={`Show product image ${i+1}`}><Image src={src} alt="" fill sizes="(max-width: 650px) 30vw, 15vw"/></button>)}</div></div><div className="detail-copy"><span className="eyebrow gold">{p.brand} · {p.collection} COLLECTION</span><h1>{p.name}</h1><div className="rating">★★★★★ <span>{p.rating.toFixed(1)} / 5 · {p.reviewCount} demo reviews</span></div><div className="detail-price">{money(price)} {p.oldPrice&&<del>{money(productPrice({...p,price:p.oldPrice},size))}</del>}</div><p>{p.description}</p><div className="size-picker"><span className="eyebrow gold">CHOOSE YOUR SIZE</span><div className="size-options">{["30ml","50ml","100ml"].map(option=><button type="button" key={option} className={size===option?"size-option selected":"size-option"} onClick={()=>{const inCart=cart.filter(line=>line.id===p.id&&(line.size||"100ml")===option).reduce((total,line)=>total+line.qty,0);const availableForSize=Math.max(0,productStock(p,option)-inCart);setSize(option);setQty(v=>Math.min(v,Math.max(1,availableForSize)));}}>{option}</button>)}</div><span className="muted small">Selected: {size} · {money(price)}</span></div><div className="quantity-picker"><span className="eyebrow gold">QUANTITY</span><div className="qty"><button type="button" onClick={()=>setQty(v=>Math.max(1,v-1))} aria-label="Decrease quantity"><Minus size={13}/></button><span>{qty}</span><button type="button" onClick={()=>setQty(v=>Math.min(Math.max(1,remainingStock),v+1))} aria-label="Increase quantity" disabled={qty>=remainingStock}><Plus size={13}/></button></div></div><div className="detail-facts"><div><small>TOP NOTES</small><b>{p.topNotes.join(" · ")}</b></div><div><small>HEART NOTES</small><b>{p.heartNotes.join(" · ")}</b></div><div><small>BASE NOTES</small><b>{p.baseNotes.join(" · ")}</b></div><div><small>BRAND</small><b>{p.brand}</b></div><div><small>GENDER</small><b>{p.gender}</b></div><div><small>FRAGRANCE FAMILY</small><b>{p.fragranceFamily}</b></div><div><small>LONGEVITY</small><b>{p.longevity}</b></div><div><small>SILLAGE</small><b>{p.sillage}</b></div><div><small>STOCK AVAILABILITY</small><b className={remainingStock>0?"stock-in":"stock-out"}>{remainingStock>0?`${remainingStock} available to add in ${size}`:"No additional stock available for this size"}</b></div></div><button className="gold-btn wide" disabled={!canAdd} onClick={()=>{add(p.id,size,qty);notify(`${p.name} · ${size} added to bag`);}}>ADD TO BAG · {size} <ShoppingBag size={17}/></button><button className="outline-btn wide" disabled={!canAdd} onClick={buyNow}>BUY NOW <ArrowRight size={16}/></button><button className="text-btn" onClick={()=>{const wasSaved=wish.includes(p.id);toggleWish(p.id);notify(wasSaved?"Removed from wishlist":"Added to wishlist");}}><Heart size={14}/> {wish.includes(p.id)?"SAVED TO WISHLIST":"ADD TO WISHLIST"}</button><div className="product-policy-links"><Link href="/shipping-policy">Shipping information <ArrowRight size={13}/></Link><Link href="/return-refund-policy">Return & refund policy <ArrowRight size={13}/></Link></div><p className="muted small">Complimentary delivery on orders over ৳8,000. Demo stock and reviews are sample data, not live inventory or verified customer feedback.</p></div></div>
 <section className="section product-notes-section"><div className="section-intro"><span className="eyebrow gold">THE SCENT PROFILE</span><h2>Notes that tell <em>a story</em></h2></div><div className="note-pyramid">{[["TOP NOTES",p.topNotes,"The first impression"],["HEART NOTES",p.heartNotes,"The character of the fragrance"],["BASE NOTES",p.baseNotes,"The lasting signature"]].map(([label,notes,desc])=><div key={String(label)}><span className="eyebrow gold">{label as string}</span><h3>{(notes as string[]).join(" · ")}</h3><p>{desc as string}</p></div>)}</div></section>
 <section className="section"><div className="section-intro"><span className="eyebrow gold">CUSTOMER VOICES</span><h2>Ratings & <em>Reviews</em></h2><p>Illustrative demo reviews for layout preview only.</p></div><div className="review-grid">{p.reviews.map((r,i)=><article key={i}><div className="rating">{"★".repeat(r.rating)}{"☆".repeat(5-r.rating)} <span>{r.rating}/5</span></div><h3>{r.title}</h3><p>{r.comment}</p><small>{r.name} · {r.date}</small></article>)}</div></section>
 {related.length>0&&<section className="section"><div className="section-intro"><span className="eyebrow gold">SIMILAR SCENTS</span><h2>You may also <em>love</em></h2></div><div className="product-grid">{related.map(x=><ProductCard key={x.id} p={x}/>)}</div></section>}
 {recentProducts.length>0&&<section className="section"><div className="section-intro"><span className="eyebrow gold">YOUR BROWSING HISTORY</span><h2>Recently <em>Viewed</em></h2></div><div className="product-grid">{recentProducts.slice(0,4).map(x=><ProductCard key={x.id} p={x}/>)}</div></section>}
 </main>
}
export function CartPage(){
 const {products,cart,remove,setQty,notify}=useStore();
 const lines: ResolvedCartLine[] = cart.map((line)=>({...line,p:products.find((product)=>product.id===line.id)})).filter((line): line is ResolvedCartLine => Boolean(line.p));
 const subtotal=lines.reduce((total,line)=>total+productPrice(line.p,line.size||"100ml")*line.qty,0);
 const delivery=subtotal===0||subtotal>=8000?0:120;
 return <main className="page wrap"><div className="page-heading"><span className="eyebrow gold">YOUR SELECTION</span><h1>Shopping <em>Bag</em></h1></div>
 {!lines.length?<div className="empty-state"><ShoppingBag size={40}/><h2>Your bag is waiting</h2><p>Discover a fragrance that feels like you.</p><Link className="gold-btn" href="/shop">EXPLORE PERFUMES</Link></div>:
 <div className="cart-layout"><div className="cart-lines">{lines.map((l)=><div className="cart-line" key={`${l.id}-${l.size||l.p.size}`}><Image src={l.p.image} alt={l.p.name} width={120} height={160}/><div className="cart-line-info"><Link href={`/products/${l.p.slug}`}>{l.p.name}</Link><span>{l.size||l.p.size} · {l.p.notes}</span><b>{money(productPrice(l.p,l.size||"100ml"))}</b><div className="qty"><button type="button" aria-label="Decrease quantity" disabled={l.qty<=1} onClick={()=>{setQty(l.id,l.size,l.qty-1);notify("Cart quantity updated");}}><Minus size={13}/></button><span>{l.qty}</span><button type="button" aria-label="Increase quantity" disabled={cart.filter((x:CartLine)=>x.id===l.id&&(x.size||"100ml")===(l.size||"100ml")).reduce((n:number,x:CartLine)=>n+x.qty,0)>=productStock(l.p,l.size||"100ml")} onClick={()=>{setQty(l.id,l.size,l.qty+1);notify("Cart quantity updated");}}><Plus size={13}/></button></div><small className="muted">Line total: {money(productPrice(l.p,l.size||"100ml")*l.qty)}</small></div><button type="button" className="remove" onClick={()=>{remove(l.id,l.size);notify(`${l.p.name} removed from bag`);}} aria-label={`Remove ${l.p.name}`}><Trash2/></button></div>)}</div>
 <aside className="summary"><h3>ORDER SUMMARY</h3><div><span>Subtotal</span><b>{money(subtotal)}</b></div><div><span>Discount</span><b>Apply at checkout</b></div><div><span>Delivery</span><b>{delivery?money(delivery):"FREE"}</b></div><hr/><div className="summary-total"><span>Estimated total</span><b>{money(subtotal+delivery)}</b></div><p className="muted small">Delivery is ৳120 below ৳8,000; free from ৳8,000. Discount codes can be applied at checkout.</p><Link className="gold-btn wide" href="/checkout">PROCEED TO CHECKOUT <ArrowRight/></Link><Link className="continue-link" href="/shop">Continue shopping</Link></aside></div>}</main>
}
export function WishlistPage(){const {products,wish,toggleWish,add,notify}=useStore();const saved=products.filter(p=>wish.includes(p.id));return <main className="page wrap"><div className="page-heading"><span className="eyebrow gold">YOUR FAVOURITES</span><h1>Wish <em>List</em></h1></div>{saved.length?<div className="product-grid">{saved.map(p=><div key={p.id}><ProductCard p={p}/><button className="text-btn" onClick={()=>{toggleWish(p.id);notify(`${p.name} removed from wishlist`);}}>REMOVE FROM WISHLIST</button><button className="text-btn" disabled={productStock(p,p.size)<1} onClick={()=>{add(p.id,p.size);notify(`${p.name} added to bag`);}}>{productStock(p,p.size)>0?"ADD TO BAG":"OUT OF STOCK"}</button></div>)}</div>:<div className="empty-state"><Heart size={40}/><h2>No saved fragrances yet</h2><Link className="gold-btn" href="/shop">EXPLORE PERFUMES</Link></div>}</main>}
export function CheckoutPage(){
 const {products,cart}=useStore();
 const lines:ResolvedCartLine[]=cart.map(line=>({...line,p:products.find(product=>product.id===line.id)})).filter((line):line is ResolvedCartLine=>Boolean(line.p));
 const [addresses,setAddresses]=useState<Address[]>([]);
 const [addressId,setAddressId]=useState("");
 const [coupon,setCoupon]=useState("");
 const [quote,setQuote]=useState<CheckoutQuote|null>(null);
 const [loading,setLoading]=useState(true);
 const [previewing,setPreviewing]=useState(false);
 const [placing,setPlacing]=useState(false);
 const [error,setError]=useState("");
 const [notice,setNotice]=useState("");
 const router=useRouter();
 useEffect(()=>{let active=true;getAddresses().then(saved=>{if(active){setAddresses(saved);const preferred=saved.find(item=>item.is_default)||saved[0];if(preferred)setAddressId(String(preferred.id));}}).catch(reason=>{if(active)setError(reason instanceof Error?reason.message:"Sign in and save a delivery address before checkout.");}).finally(()=>{if(active)setLoading(false);});return()=>{active=false;};},[]);
 async function calculateQuote(){
  if(!addressId){setError("Choose a saved delivery address first.");return;}
  setError("");setNotice("");setPreviewing(true);
  try{const result=await previewCheckout(Number(addressId),coupon.trim()||undefined);setQuote(result);if(!result.stockAvailable)setNotice("Some cart items are not currently available.");}
  catch(reason){setQuote(null);setError(reason instanceof Error?reason.message:"Could not calculate the server checkout quote.");}
  finally{setPreviewing(false);}
 }
 async function submitOrder(){
  if(!quote){await calculateQuote();return;}
  if(!quote.stockAvailable){setError("Stock is not available for every cart item. Update your cart before placing the order.");return;}
  setPlacing(true);setError("");setNotice("");
  try{
   const order=await placeOrder(Number(addressId),coupon.trim()||undefined);
   try{await requestCodPayment(order.orderNumber,order.orderNumber+"-"+Date.now());}
   catch(paymentError){setNotice("Your order was created, but the COD payment request needs staff review: "+(paymentError instanceof Error?paymentError.message:"payment request failed"));}
   router.push("/order-success?order="+encodeURIComponent(order.orderNumber));
  }catch(reason){setError(reason instanceof Error?reason.message:"Order could not be created. Please review your cart and try again.");}
  finally{setPlacing(false);}
 }
 const totals=quote?.totals;
 return <main className="page wrap"><div className="page-heading"><span className="eyebrow gold">ALMOST YOURS</span><h1>Secure <em>Checkout</em></h1><p>Choose a saved delivery address and review the server-calculated order total.</p></div>
  {loading?<p className="muted">Loading your saved addresses…</p>:!addresses.length?<div className="empty-state"><h2>Delivery address required</h2><p>Sign in and add a delivery address to your account before checkout.</p><Link href="/account" className="gold-btn">MANAGE ACCOUNT & ADD ADDRESS</Link></div>:!lines.length?<div className="empty-state"><h2>Your bag is empty</h2><p>Add a fragrance before checkout.</p><Link href="/shop" className="gold-btn">SHOP FRAGRANCES</Link></div>:
  <div className="checkout-grid"><div className="checkout-fields"><section className="form-panel"><h2>01. Delivery address</h2><label>Choose saved address<select value={addressId} onChange={e=>{setAddressId(e.target.value);setQuote(null);}} className="w-full" required><option value="">Select an address</option>{addresses.map(item=><option key={item.id} value={item.id}>{item.label} · {item.recipient_name} · {item.phone} · {item.city}</option>)}</select></label>{addresses.filter(item=>String(item.id)===addressId).map(item=><div key={item.id} className="saved-address"><div><b>{item.recipient_name}</b><p>{item.phone}<br/>{item.address_line1}, {item.area}, {item.city}, {item.district} {item.postal_code}</p></div></div>)}<Link href="/account" className="continue-link">Manage saved addresses <ArrowRight size={14}/></Link></section>
   <section className="form-panel"><h2>02. Payment method</h2><div className="payment-option selected"><span className="payment-logo cod">৳</span><span><b>Cash on Delivery</b><small>Pay the courier when your order arrives. No online payment is collected here.</small></span></div><div className="demo-notice"><b>Payment availability</b><p>Only the backend-supported Cash on Delivery workflow is enabled. bKash and Nagad are not presented as successful payment options because live gateway integration is not yet available.</p></div></section>
   <section className="form-panel"><h2>03. Discount code</h2><div className="coupon-row"><input value={coupon} onChange={e=>{setCoupon(e.target.value);setQuote(null);}} placeholder="Enter coupon code"/><button type="button" className="outline-btn" onClick={()=>void calculateQuote()} disabled={previewing}>{previewing?"CHECKING…":"VALIDATE"}</button></div></section>
  </div>
  <aside className="summary"><h3>SERVER CHECKOUT QUOTE</h3>{lines.map(line=><div key={line.id+"-"+line.size}><span>{line.p.name} · {line.size||line.p.size} × {line.qty}</span><b>{money(productPrice(line.p,line.size||"100ml")*line.qty)}</b></div>)}<hr/>{totals?<><div><span>Subtotal</span><b>{money(Number(totals.subtotal))}</b></div><div><span>Discount</span><b>{money(Number(totals.discountTotal))}</b></div><div><span>Delivery</span><b>{money(Number(totals.shippingTotal))}</b></div><div><span>Tax</span><b>{money(Number(totals.taxTotal))}</b></div><div className="summary-total"><span>Grand total</span><b>{money(Number(totals.grandTotal))}</b></div><p className="muted small">{quote?.notice}</p></>:<p className="muted small">Calculate a checkout quote to see authoritative prices, discounts, shipping and stock validation.</p>}
   {error&&<p className="form-error" role="alert">{error}</p>}{notice&&<p className="demo-notice" role="status">{notice}</p>}
   <button className="outline-btn wide" type="button" disabled={previewing||!addressId} onClick={()=>void calculateQuote()}>{previewing?"CALCULATING…":"RECALCULATE QUOTE"}</button>
   <button className="gold-btn wide" type="button" disabled={placing||!quote||!quote.stockAvailable} onClick={()=>void submitOrder()}>{placing?"CREATING ORDER…":<>PLACE ORDER · COD <ArrowRight/></>}</button>
   <p className="muted small">The backend recalculates prices and reserves inventory transactionally when the order is placed.</p>
  </aside></div>}
 </main>;
}
export function AboutPage(){return <main className="page wrap"><div className="page-heading"><span className="eyebrow gold">THE LUXE STORY</span><h1>Our <em>Story</em></h1><p>A fragrance-led storefront concept built around discovery and personal expression.</p></div><div className="about-split"><div className="about-copy"><span className="eyebrow gold">OUR PHILOSOPHY</span><h2>{brandContent.aboutStory.heading}</h2>{brandContent.aboutStory.paragraphs.map((paragraph,index)=><p key={index}>{paragraph}</p>)}</div><div className="about-media">{brandContent.aboutMedia.type==="video"&&brandContent.aboutMedia.videoUrl?<video controls playsInline preload="metadata" poster={brandContent.aboutMedia.imageUrl}><source src={brandContent.aboutMedia.videoUrl}/></video>:<Image src={brandContent.aboutMedia.imageUrl} alt={brandContent.aboutMedia.alt} width={900} height={1100}/>}<div className="media-replacement-note"><span className="eyebrow gold">BRAND FILM / EDITORIAL IMAGE</span><p>Edit src/lib/brand-content.ts to replace this image or set the media type to video and add an approved MP4/WebM URL.</p></div></div></div><section className="section"><div className="center-heading"><span className="eyebrow gold">WHAT GUIDES US</span><h2>Our Mission, <em>Vision & Values</em></h2></div><div className="note-pyramid"><div><span className="eyebrow gold">MISSION</span><h3>{brandContent.mission.title}</h3><p>{brandContent.mission.description}</p></div><div><span className="eyebrow gold">VISION</span><h3>{brandContent.vision.title}</h3><p>{brandContent.vision.description}</p></div><div><span className="eyebrow gold">VALUES</span><h3>{brandContent.values.title}</h3><p>{brandContent.values.description}</p></div></div></section><section className="section"><div className="section-intro"><span className="eyebrow gold">CRAFTSMANSHIP</span><h2>Details make the <em>difference</em></h2><p>{brandContent.craftsmanship}</p></div><div className="craft-band"><div className="wrap craft-inner"><div><span className="eyebrow gold">SUSTAINABILITY</span><h2>Thoughtful choices,<br/><em>honestly shared.</em></h2></div><p>{brandContent.sustainability}</p><Link className="outline-btn" href="/contact">CONTACT US <ArrowRight/></Link></div></div></section><section className="section"><div className="section-intro"><span className="eyebrow gold">BRAND TIMELINE</span><h2>A story still <em>unfolding</em></h2><p>Replace these milestones with the real dates and events from your brand journey.</p></div><ol className="brand-timeline">{brandContent.timeline.map((item)=><li key={item.title}><b>{item.title}</b><span>{item.description}</span></li>)}</ol></section></main>}
export function ContactPage(){
 const [status,setStatus]=useState<"idle"|"sending"|"sent"|"error">("idle");
 async function handleSubmit(event:FormEvent<HTMLFormElement>){event.preventDefault();if(status==="sending")return;const form=event.currentTarget;const data=new FormData(form);const payload=Object.fromEntries(data.entries());
  if(contactFormMode==="demo"){setStatus("sent");form.reset();return;}
  setStatus("sending");try{const response=await fetch("/api/contact",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(payload)});if(!response.ok)throw new Error("Contact request failed");setStatus("sent");form.reset();}catch{setStatus("error");}
 }
 const phone=siteContact.phone.trim();const whatsapp=siteContact.whatsapp.trim().replace(/[^0-9]/g,"");const supportEmail=siteContact.supportEmail.trim();
 return <main className="page wrap"><div className="page-heading"><span className="eyebrow gold">WE WOULD LOVE TO HEAR FROM YOU</span><h1>Get in <em>Touch</em></h1><p>Questions about a fragrance or an order? Send a message to the LUXE team.</p></div><div className="contact-layout"><form className="form-panel contact-form" onSubmit={handleSubmit}><h2>Send us a message</h2><div className="form-grid"><label>Your name *<input required minLength={2} maxLength={80} name="name" autoComplete="name" placeholder="Full name"/></label><label>Email address *<input required type="email" maxLength={120} name="email" autoComplete="email" placeholder="you@example.com"/></label><label>Phone number<input type="tel" maxLength={24} name="phone" autoComplete="tel" placeholder="Optional phone number"/></label><label>Subject *<input required minLength={3} maxLength={120} name="subject" placeholder="How can we help?"/></label><label className="full">Message *<textarea required minLength={10} maxLength={2000} name="message" rows={5} placeholder="Write your message..."/></label></div><button className="gold-btn" type="submit" disabled={status==="sending"}>{status==="sending"?"SENDING…":"SEND MESSAGE"} <ArrowRight/></button>{status==="sent"&&<p className="success-text" role="status">{contactFormMode==="demo"?"Demo message received. No email was sent or stored.":"Thank you. Your message has been sent to our support team."}</p>}{status==="error"&&<p className="form-error" role="alert">We could not send your message right now. Please try again later or use the configured contact details.</p>}<p className="muted small">{contactFormMode==="demo"?"Demo mode is active. Form submissions are not sent or stored. Switch to email mode and configure the mail service before launch.":"Email mode is active. Your message is sent securely through the configured email service."}</p></form><aside className="contact-details"><span className="eyebrow gold">CUSTOMER SUPPORT</span><h2>Here to <em>help.</em></h2><div className="contact-info-list"><p><b>Business name</b><span>{siteContact.businessName}</span></p><p><b>Business address</b><span>{siteContact.businessAddress||"Demo address placeholder — add your verified address."}</span></p><p><b>Phone</b><span>{phone? <a href={`tel:${phone}`}>{phone}</a>:"Demo phone placeholder — add your public business number."}</span></p><p><b>WhatsApp</b><span>{whatsapp? <a href={`https://wa.me/${whatsapp}`} target="_blank" rel="noreferrer">{siteContact.whatsappButtonText}</a>:"Demo WhatsApp placeholder — add your public business number."}</span></p><p><b>Support email</b><span>{supportEmail?<a href={`mailto:${supportEmail}`}>{supportEmail}</a>:"Demo email placeholder — add your support email."}</span></p><p><b>Business hours</b><span>{siteContact.businessHours}</span></p><p><b>Social media</b><span className="contact-social">{Object.entries(siteContact.social).filter(([,url])=>url).length?Object.entries(siteContact.social).filter(([,url])=>url).map(([name,url])=><a key={name} href={url} target="_blank" rel="noreferrer">{name}</a>):"Demo social placeholders — add your official profile URLs."}</span></p></div>{siteContact.googleMapsEmbedUrl?<div className="map-frame"><iframe src={siteContact.googleMapsEmbedUrl} title="LUXE PERFUME business location" loading="lazy" referrerPolicy="no-referrer-when-downgrade" allowFullScreen/></div>:<div className="map-placeholder"><MapPin size={24}/><b>Google Maps placeholder</b><span>Add a verified business map embed URL in src/lib/site-config.ts.</span></div>}<Link className="continue-link" href="/faq">Visit frequently asked questions <ArrowRight size={14}/></Link></aside></div></main>
}

export function FaqPage(){const items=[
 ["Are your perfumes authentic?","This is a demo catalogue. Add your verified sourcing, authenticity and product-origin information before accepting real orders."],
 ["How do I choose a fragrance?","Start with the fragrance family and notes shown on each product page. The top notes are the first impression, followed by heart and base notes."],
 ["How long will a perfume last?","Longevity varies with the formula, skin, climate and application. The hour ranges shown in this catalogue are illustrative demo information, not a guarantee."],
 ["How long does delivery take?","Delivery coverage and estimated timelines must be confirmed by the store owner. No courier service is connected to this demo."],
 ["Which payment methods are available?","This storefront demo offers bKash, Nagad and Cash on Delivery only. bKash and Nagad are selection-only; no payment is collected."],
 ["Can I return or exchange a perfume?","Please review the editable Return & Refund Policy and confirm the final rules with the store before placing a real order."],
 ["How are refunds handled?","No real payment or refund is processed in this demo. Add the verified refund timeline and process to the Return & Refund Policy before launch."],
 ["How can I track an order?","Use the demo order reference and checkout phone number on the Track Order page. The six-stage timeline is illustrative and does not connect to a courier."]
 ];return <main className="page wrap narrow"><div className="page-heading"><span className="eyebrow gold">HELP & SUPPORT</span><h1>Frequently Asked <em>Questions</em></h1><p>Helpful information about the LUXE demo storefront.</p></div>{items.map(([q,a])=><details className="faq-item" key={q}><summary>{q}<Plus size={17}/></summary><p>{a}</p></details>)}</main>}
const OAUTH_READY=false; // Enable only after a secure server-side Google OAuth provider is configured.
export function AuthPage({mode}:{mode:"login"|"register"}){
 const [message,setMessage]=useState("");
 const [messageError,setMessageError]=useState(false);
 const [busy,setBusy]=useState(false);
 const isRegister=mode==="register";
 const router=useRouter();
 async function submit(e:FormEvent<HTMLFormElement>){
  e.preventDefault();
  setMessage("");
  setMessageError(false);
  const form=e.currentTarget;
  if(!form.reportValidity())return;
  const data=new FormData(form);
  const password=String(data.get("password")||"");
  if(isRegister&&password!==String(data.get("confirmPassword")||"")){
   setMessage("The passwords do not match. Please check both fields.");
   setMessageError(true);
   return;
  }
  setBusy(true);
  try{
   if(isRegister){
    const fullName=String(data.get("name")||"").trim();
    const parts=fullName.split(/\\s+/);
    const firstName=parts.shift()||"";
    const lastName=parts.join(" ");
    await registerAccount({email:String(data.get("email")||""),password,firstName,lastName});
   }else{
    await signIn(String(data.get("email")||""),password);
   }
   router.replace("/account");
   router.refresh();
  }catch(error){
   setMessage(error instanceof Error?error.message:"Authentication failed. Please try again.");
   setMessageError(true);
  }finally{
   setBusy(false);
  }
 }
 return <main className="page wrap narrow"><div className="page-heading"><span className="eyebrow gold">YOUR LUXE ACCOUNT</span><h1>{isRegister?<>Create Your <em>Account</em></>:<>Welcome <em>Back</em></>}</h1><p>{isRegister?"Create your secure LUXE customer account.":"Sign in to access your LUXE account."}</p></div><div className="form-panel">
  <div role="group" aria-label="Account access options" style={{display:"flex",gap:10,justifyContent:"center",marginBottom:24,flexWrap:"wrap"}}>
   <Link className={isRegister?"outline-btn":"gold-btn"} href="/login" aria-current={!isRegister?"page":undefined}>SIGN IN</Link>
   <Link className={isRegister?"gold-btn":"outline-btn"} href="/register" aria-current={isRegister?"page":undefined}>CREATE ACCOUNT</Link>
  </div>
  <h2>{isRegister?"Register with email":"Sign in with email"}</h2>
  <form className="form-grid" onSubmit={submit}>
   {isRegister&&<label className="full">Full name *<input name="name" type="text" autoComplete="name" minLength={2} maxLength={80} required placeholder="Your full name"/></label>}
   <label className="full">Email address *<input name="email" type="email" autoComplete="email" maxLength={254} required placeholder="you@example.com"/></label>
   <label className="full">Password *<input name="password" type="password" autoComplete={isRegister?"new-password":"current-password"} minLength={12} maxLength={72} required placeholder="At least 12 characters"/></label>
   {isRegister&&<label className="full">Confirm password *<input name="confirmPassword" type="password" autoComplete="new-password" minLength={12} maxLength={72} required placeholder="Enter your password again"/></label>}
   <div className="full"><button className="gold-btn" type="submit" disabled={busy}>{busy?"PLEASE WAIT…":isRegister?"CREATE ACCOUNT":"SIGN IN"} <ArrowRight size={14}/></button></div>
  </form>
  {message&&<p className={messageError?"form-error":"demo-notice"} role={messageError?"alert":"status"} aria-live="polite">{message}</p>}
  <div className="demo-notice"><b>Secure backend authentication</b><p>Credentials are sent directly to the backend over the configured API origin. Session tokens are stored in HttpOnly cookies, not browser localStorage.</p></div>
  <div style={{marginTop:20,borderTop:"1px solid var(--line)",paddingTop:18}}>
   <h2 style={{fontSize:20,marginBottom:12}}>Or continue with Google</h2>
   <button className="outline-btn wide" type="button" disabled={!OAUTH_READY} aria-disabled={!OAUTH_READY}>G&nbsp; CONTINUE WITH GOOGLE</button>
   <p className="muted small" style={{marginTop:12}}>Google sign-in is available after authorized OAuth credentials and callback settings are configured.</p>
  </div>
  <p className="muted small" style={{marginTop:18}}>Your account and order history are managed by the LUXE backend.</p>
  <Link className="continue-link" href="/account">View account profile UI <ArrowRight size={14}/></Link>
  <Link className="continue-link" href="/my-orders">View order history <ArrowRight size={14}/></Link>
 </div></main>;
}
export function LoginPage(){return <AuthPage mode="login"/>;}
export function RegisterPage(){return <AuthPage mode="register"/>;}
type SavedAddress = { id: string; label: string; name: string; phone: string; division: string; district: string; area: string; fullAddress: string; postalCode: string };
type AccountProfile = { name: string; email: string; phone: string };
const bdPhonePattern = /^(?:\+?880|0)1[3-9]\d{8}$/;
const normalizeBdPhone = (value: string) => {
 const digits = value.replace(/\D/g, "");
 return digits.startsWith("880") && digits.length === 13 ? `0${digits.slice(3)}` : digits;
};
const isSavedAddressList = (value: unknown): value is SavedAddress[] => Array.isArray(value) && value.every((item) => item && typeof item.id === "string" && typeof item.label === "string" && typeof item.name === "string" && typeof item.phone === "string" && typeof item.division === "string" && typeof item.district === "string" && typeof item.area === "string" && typeof item.fullAddress === "string" && typeof item.postalCode === "string");
export function AccountPage(){
 const router=useRouter();
 const [profile,setProfile]=useState<{name:string;email:string}>({name:"",email:""});
 const [addresses,setAddresses]=useState<Address[]>([]);
 const [address,setAddress]=useState({label:"Home",name:"",phone:"",division:"Dhaka",district:"",area:"",fullAddress:"",postalCode:""});
 const [loading,setLoading]=useState(true);const [saving,setSaving]=useState(false);
 const [message,setMessage]=useState("");const [messageError,setMessageError]=useState(false);
 async function reload(){
  const [user,saved]=await Promise.all([getCurrentUser(),getAddresses()]);
  setProfile({name:[user.firstName,user.lastName].filter(Boolean).join(" "),email:user.email});
  setAddresses(saved);
 }
 useEffect(()=>{let active=true;Promise.all([getCurrentUser(),getAddresses()]).then(([user,saved])=>{if(active){setProfile({name:[user.firstName,user.lastName].filter(Boolean).join(" "),email:user.email});setAddresses(saved);}}).catch(()=>{if(active){setMessage("Please sign in to view your account and saved addresses.");setMessageError(true);}}).finally(()=>{if(active)setLoading(false);});return()=>{active=false;};},[]);
 async function saveAddress(event:FormEvent<HTMLFormElement>){
  event.preventDefault();setMessage("");setMessageError(false);
  const phone=address.phone.replace(/[\\s-]/g,"");
  if(!bdPhonePattern.test(phone)){setMessage("Enter a valid Bangladesh mobile number.");setMessageError(true);return;}
  if(address.postalCode&&!/^\\d{4}$/.test(address.postalCode)){setMessage("Postal code must contain four digits.");setMessageError(true);return;}
  setSaving(true);
  try{
   await createAddress({label:address.label,recipient_name:address.name,phone,address_line1:address.fullAddress,address_line2:"",area:address.area,city:address.division,district:address.district,postal_code:address.postalCode,country_code:"BD",makeDefault:addresses.length===0});
   setAddress({label:"Home",name:"",phone:"",division:"Dhaka",district:"",area:"",fullAddress:"",postalCode:""});
   await reload();setMessage("Delivery address saved to your account.");
  }catch(error){setMessage(error instanceof Error?error.message:"Could not save this address.");setMessageError(true);}
  finally{setSaving(false);}
 }
 async function removeAddress(id:number){
  try{await deleteAddress(id);await reload();setMessage("Address deleted from your account.");setMessageError(false);}
  catch(error){setMessage(error instanceof Error?error.message:"Could not delete this address.");setMessageError(true);}
 }
 async function makeDefault(id:number){
  try{await setDefaultAddress(id);await reload();setMessage("Default delivery address updated.");setMessageError(false);}
  catch(error){setMessage(error instanceof Error?error.message:"Could not update the default address.");setMessageError(true);}
 }
 async function logout(){
  try{await signOut();router.replace("/login");router.refresh();}
  catch(error){setMessage(error instanceof Error?error.message:"Could not sign out.");setMessageError(true);}
 }
 if(loading)return <main className="page wrap narrow"><p className="muted">Loading your account…</p></main>;
 return <main className="page wrap narrow"><div className="page-heading"><span className="eyebrow gold">YOUR LUXE ACCOUNT</span><h1>Account <em>Profile</em></h1><p>Your account information and delivery addresses are securely stored by LUXE.</p></div>
  <section className="form-panel account-section"><div className="account-section-head"><div className="account-profile-heading"><span className="account-avatar"><UserRound size={24}/></span><div><h2>{profile.name||"Customer"}</h2><small className="muted">{profile.email}</small></div></div><span className="eyebrow gold">AUTHENTICATED</span></div><p className="muted small">Your identity is read from the authenticated backend session. Profile editing can be enabled when a profile-update API is added.</p></section>
  <section className="form-panel account-section"><div className="account-section-head"><h2>Saved addresses</h2><span className="eyebrow gold">{addresses.length} SAVED</span></div>{addresses.length?addresses.map(item=><article className="saved-address" key={item.id}><div><b>{item.label||"Address"} · {item.recipient_name}</b><p>{item.phone}<br/>{item.address_line1}{item.address_line2?", "+item.address_line2:""}, {item.area}, {item.city}, {item.district} {item.postal_code}</p>{item.is_default&&<small className="success-text">DEFAULT ADDRESS</small>}</div><div className="flex gap-3"><button className="text-btn" type="button" disabled={item.is_default} onClick={()=>void makeDefault(item.id)}>MAKE DEFAULT</button><button className="text-btn" type="button" onClick={()=>void removeAddress(item.id)}>REMOVE</button></div></article>):<p className="muted small">No saved address yet. Add one below.</p>}
   <form onSubmit={saveAddress} className="form-grid address-form"><label>Address label<select value={address.label} onChange={e=>setAddress({...address,label:e.target.value})}><option>Home</option><option>Work</option><option>Other</option></select></label><label>Recipient name<input required minLength={2} value={address.name} onChange={e=>setAddress({...address,name:e.target.value})}/></label><label>Phone<input required type="tel" value={address.phone} onChange={e=>setAddress({...address,phone:e.target.value})} placeholder="01XXXXXXXXX"/></label><label>Division / City<input required value={address.division} onChange={e=>setAddress({...address,division:e.target.value})}/></label><label>District<input required value={address.district} onChange={e=>setAddress({...address,district:e.target.value})}/></label><label>Area / Thana<input required value={address.area} onChange={e=>setAddress({...address,area:e.target.value})}/></label><label className="full">Full address<input required minLength={8} value={address.fullAddress} onChange={e=>setAddress({...address,fullAddress:e.target.value})}/></label><label>Postal code (optional)<input inputMode="numeric" pattern="[0-9]{4}" maxLength={4} value={address.postalCode} onChange={e=>setAddress({...address,postalCode:e.target.value})}/></label><div className="full"><button className="outline-btn" type="submit" disabled={saving}>{saving?"SAVING…":"ADD SAVED ADDRESS"} <Plus size={14}/></button></div></form>
  </section>
  {message&&<p className={messageError?"form-error":"success-text"} role={messageError?"alert":"status"}>{message}</p>}
  <div className="account-links"><Link className="outline-btn" href="/my-orders">MY ORDERS <ArrowRight size={14}/></Link><Link className="outline-btn" href="/wishlist">MY WISHLIST <Heart size={14}/></Link><button className="text-btn" type="button" onClick={()=>void logout()}>SIGN OUT</button></div>
 </main>;
}
export function OrdersPage(){
 const [orders,setOrders]=useState<DemoOrder[]>([]);const [ready,setReady]=useState(false);const [expanded,setExpanded]=useState<string|null>(null);
 useEffect(()=>{try{const parsed:unknown=JSON.parse(localStorage.getItem('luxe-orders')||'[]');setOrders(Array.isArray(parsed)?parsed.filter(isDemoOrder):[]);}catch{setOrders([]);}finally{setReady(true);}},[]);
 return <main className="page wrap"><div className="page-heading"><span className="eyebrow gold">YOUR PURCHASES</span><h1>My Order <em>History</em></h1><p>Orders placed from this browser are listed here. These are demo records, not server-backed purchases.</p></div>{!ready?<p className="muted">Loading orders…</p>:orders.length?<div className="orders-list">{orders.map(o=><article className="order-card" key={o.id}><div className="order-card-head"><div><span className="eyebrow gold">ORDER REFERENCE</span><h2>{o.id}</h2></div><span className="order-status">{o.status||'Demo order received'}</span></div><div className="order-card-meta"><span>{new Date(o.createdAt).toLocaleString()}</span><b>{money(o.total)}</b><span>{o.items?.length||0} item(s)</span><span>{o.payment==='bkash'?'bKash':o.payment==='nagad'?'Nagad':'Cash on Delivery'}</span></div><div className="order-card-actions"><button className="outline-btn" type="button" onClick={()=>setExpanded(expanded===o.id?null:o.id)}>{expanded===o.id?"HIDE DETAILS":"VIEW DETAILS"}</button><Link className="outline-btn" href={`/track-order?order=${o.id}`}>TRACK ORDER <ArrowRight size={14}/></Link></div>{expanded===o.id&&<div className="order-detail-panel"><h3>Items in this demo order</h3>{o.items.map((item)=><p key={`${item.id}-${item.size}`}>{item.name} · {item.size} × {item.qty} — {money(item.lineTotal||item.unitPrice*item.qty)}</p>)}<p><b>Delivery address:</b> {o.customer?.address}, {o.customer?.area}, {o.customer?.district}</p><p><b>Phone:</b> {o.customer?.phone}</p><p><b>Payment:</b> {o.payment==='bkash'?'bKash':o.payment==='nagad'?'Nagad':'Cash on Delivery'}</p></div>}</article>)}</div>:<div className="empty-state"><ShoppingBag size={38}/><h2>No demo orders yet</h2><p>After placing a demo order, it will appear here.</p><Link className="gold-btn" href="/shop">SHOP NOW</Link></div>}<p className="muted small">Order history is stored locally in this browser. Clearing browser storage or using another device will remove or hide these demo records.</p></main>;
}
export function PoliciesPage(){return <main className="page wrap narrow"><div className="page-heading"><span className="eyebrow gold">THE DETAILS</span><h1>Store <em>Policies</em></h1></div><h2>Shipping</h2><p className="body-copy">Standard delivery fee is shown as ৳120 for orders below ৳8,000; complimentary delivery is displayed for orders of ৳8,000 or more. Confirm actual coverage and delivery times before launch.</p><h2>Returns & refunds</h2><p className="body-copy">Publish your final return window, eligible product condition, sealed-item rules and refund process here. For hygiene reasons, opened fragrance products may have restrictions under your local policy.</p><h2>Privacy</h2><p className="body-copy">Customer details should only be used to fulfil orders, provide support and send marketing messages where consent has been given. Connect a complete privacy notice before launch.</p><h2>Terms</h2><p className="body-copy">Product availability, prices, delivery charges and order acceptance should be confirmed by the store. These sample terms are placeholders, not legal advice.</p></main>}

// Phase 1 route additions. These are frontend-only demo pages; persistence and
// authentication will be connected during the backend phase.
export function BestSellersPage(){
 const {products}=useStore();
 const featured=products.filter(p=>p.badge?.toLowerCase().includes('best')||p.rating>=4.8);
 return <main className="page wrap"><div className="page-heading"><span className="eyebrow gold">CUSTOMER FAVOURITES</span><h1>Our <em>Best Sellers</em></h1><p>Fragrances customers return to, again and again.</p></div><div className="product-grid">{featured.map(p=><ProductCard key={p.id} p={p}/>)}</div></main>;
}
export function NewArrivalsPage(){
 const {products}=useStore();
 const [sort,setSort]=useState("featured");
 const latest=useMemo(()=>{const list=products.filter(p=>p.badge?.toLowerCase()==="new");if(sort==="low")list.sort((a,b)=>a.price-b.price);if(sort==="high")list.sort((a,b)=>b.price-a.price);if(sort==="rating")list.sort((a,b)=>b.rating-a.rating);return list;},[sort]);
 return <main className="page wrap"><div className="page-heading"><span className="eyebrow gold">JUST ARRIVED</span><h1>New <em>Arrivals</em></h1><p>Discover the latest additions to the LUXE fragrance edit.</p></div><div className="shop-tools new-arrivals-tools"><label htmlFor="new-arrivals-sort">Sort arrivals</label><select id="new-arrivals-sort" value={sort} onChange={e=>setSort(e.target.value)}><option value="featured">Featured</option><option value="low">Price: low to high</option><option value="high">Price: high to low</option><option value="rating">Highest rated</option></select></div><div className="product-grid">{latest.map(p=><ProductCard key={p.id} p={p}/>)}</div>{!latest.length&&<p className="muted">New fragrances will appear here soon.</p>}</main>;
}
export function SearchPage(){
 const {products,catalogLoading,catalogError}=useStore();
 const [query,setQuery]=useState('');const [ready,setReady]=useState(false);
 useEffect(()=>{const q=new URLSearchParams(window.location.search).get('q')||'';setQuery(q);setReady(true);},[]);
 const results=useMemo(()=>products.filter(p=>(p.name+' '+p.brand+' '+p.gender+' '+p.notes+' '+p.collection+' '+p.fragranceFamily+' '+p.topNotes.join(' ')).toLowerCase().includes(query.trim().toLowerCase())),[query]);
 return <main className="page wrap"><div className="page-heading"><span className="eyebrow gold">FIND YOUR FRAGRANCE</span><h1>Search <em>LUXE</em></h1><p>Search by fragrance name, brand, collection, scent notes or fragrance family.</p></div><form className="search-form" onSubmit={e=>e.preventDefault()}><Search size={18}/><input autoFocus value={query} onChange={e=>setQuery(e.target.value)} placeholder="Try oud, rose, amber..." aria-label="Search fragrances"/><button className="gold-btn" type="submit">SEARCH</button></form>{ready&&query.trim()&&results.length>0&&<div className="search-suggestions" aria-label="Search suggestions">{results.slice(0,5).map(p=><Link key={p.id} href={`/products/${p.slug}`}><span>{p.name}</span><small>{p.brand} · {p.collection}</small></Link>)}</div>}{ready&&<><p className="muted">{query?`${results.length} result(s) for “${query}”`:`Explore all ${results.length} fragrances`}</p>{results.length?<div className="product-grid">{results.map(p=><ProductCard key={p.id} p={p}/>)}</div>:<div className="empty-state"><Search size={38}/><h2>No matching fragrances</h2><p>We couldn&apos;t find a perfume matching “{query}”. Try another product name or scent note.</p><button className="outline-btn" onClick={()=>setQuery('')}>CLEAR SEARCH</button></div>}</>}</main>;
}
export function OrderSuccessPage(){
 const [order,setOrder]=useState<DemoOrder|null>(null);
 useEffect(()=>{try{const raw=localStorage.getItem("luxe-last-order");if(raw){const parsed:unknown=JSON.parse(raw);if(isDemoOrder(parsed))setOrder(parsed);}}catch{setOrder(null);}},[]);
 return <main className="page wrap"><div className="success-box"><CheckCircle2 size={54}/><span className="eyebrow gold">DEMO ORDER CONFIRMATION</span><h1>Thank you for choosing <em>LUXE.</em></h1>{order?<><p>Your demo order reference is <b>{order.id}</b>.</p><div className="demo-notice"><b>Payment status</b><p>{order.payment==="cod"?"Cash on Delivery selected. No payment has been collected.":`${order.payment==="bkash"?"bKash":"Nagad"} selected. Payment is NOT completed; no payment gateway is connected.`}</p></div><div className="order-detail-panel"><h3>Customer & delivery details</h3><p><b>Name:</b> {order.customer.name}</p><p><b>Email:</b> {order.customer.email}</p><p><b>Phone:</b> {order.customer.phone}</p><p><b>Delivery address:</b> {order.customer.address}, {order.customer.area}, {order.customer.district}, {order.customer.division} {order.customer.postalCode}</p><p><b>Delivery instructions:</b> {order.customer.instructions||"None"}</p></div><div className="order-detail-panel"><h3>Items in this demo order</h3>{order.items.map(item=><div className="receipt-line" key={`${item.id}-${item.size}`}><span>{item.name} · {item.size} × {item.qty}</span><b>{money(item.lineTotal)}</b></div>)}<div className="receipt-line"><span>Subtotal</span><b>{money(order.subtotal)}</b></div><div className="receipt-line"><span>Discount {order.discountCode?`(${order.discountCode})`:""}</span><b>− {money(order.discount)}</b></div><div className="receipt-line"><span>Delivery</span><b>{order.delivery?money(order.delivery):"FREE"}</b></div><div className="receipt-line receipt-total"><span>Order total</span><b>{money(order.total)}</b></div></div><p className="muted small">Estimated delivery: to be confirmed by the store. This demo has no courier integration or live fulfilment updates.</p><div className="account-links"><Link className="outline-btn" href={`/track-order?order=${order.id}`}>TRACK DEMO ORDER <ArrowRight/></Link><Link className="gold-btn" href="/shop">CONTINUE SHOPPING</Link></div><p className="muted small">This order snapshot is stored only in this browser. No real order was sent to a server.</p></>:<><p>No recent demo order was found in this browser.</p><Link className="gold-btn" href="/shop">EXPLORE PERFUMES</Link></>}</div></main>;
}
export function TrackOrderPage(){
 const [orderId,setOrderId]=useState('');const [phone,setPhone]=useState('');const [result,setResult]=useState<DemoOrder|null>(null);const [message,setMessage]=useState('');
 useEffect(()=>{const id=new URLSearchParams(window.location.search).get('order');if(id)setOrderId(id);},[]);
 function lookup(e:FormEvent<HTMLFormElement>){e.preventDefault();setMessage('');setResult(null);try{const id=orderId.trim();const raw=localStorage.getItem('luxe-order-'+id);if(!raw){setMessage('No demo order found for this reference in this browser. Check the order ID and try again.');return;}const parsed:unknown=JSON.parse(raw);if(!isDemoOrder(parsed)){setMessage('This saved record is not a valid demo order.');return;}const order=parsed;if(normalizeBdPhone(String(order.customer?.phone||''))!==normalizeBdPhone(phone)){setMessage('The phone number does not match this demo order. Please check and try again.');return;}setResult(order);}catch{setMessage('Unable to read this demo order.');}}
 const steps=[['Order Placed','Your demo order was saved in this browser.'],['Confirmed','This is a sample confirmation state, not a store confirmation.'],['Processing','The demo order is marked as being prepared.'],['Shipped','This sample status represents dispatch.'],['Out for Delivery','This sample status represents the final delivery stage.'],['Delivered','This is a demo completion state, not courier confirmation.']];
 const activeStep=Math.max(0,Math.min(5,Number(result?.statusStep??0)));
 return <main className="page wrap narrow"><div className="page-heading"><span className="eyebrow gold">ORDER SUPPORT</span><h1>Track Your <em>Order</em></h1><p>Enter the demo order reference and the phone number used at checkout.</p></div><form className="form-panel track-form" onSubmit={lookup}><label>Order reference<input required value={orderId} onChange={e=>setOrderId(e.target.value)} placeholder="e.g. LUX12345678"/></label><label>Checkout phone number<input required type="tel" inputMode="tel" value={phone} onChange={e=>setPhone(e.target.value)} placeholder="01XXXXXXXXX"/></label><button className="gold-btn" type="submit">FIND ORDER <ArrowRight/></button>{message&&<p className="form-error" role="alert">{message}</p>}{result&&<div className="tracking-result"><div className="order-card-head"><div><span className="eyebrow gold">ORDER REFERENCE</span><h2>{result.id}</h2></div><span className="order-status">DEMO · {steps[activeStep][0].toUpperCase()}</span></div><div className="detail-facts"><div><small>ORDER DATE</small><b>{new Date(result.createdAt).toLocaleDateString()}</b></div><div><small>ORDER TOTAL</small><b>{money(result.total)}</b></div><div><small>PAYMENT METHOD</small><b>{result.payment==='bkash'?'bKash':result.payment==='nagad'?'Nagad':'Cash on Delivery'}</b></div><div><small>DELIVERY TO</small><b>{result.customer?.district||'Not available'}</b></div></div><h3 className="tracking-title">Demo order timeline</h3><ol className="status-timeline">{steps.map(([title,description],index)=><li className={index<=activeStep?'complete':''} key={title} aria-current={index===activeStep?'step':undefined}><b>{title}</b><span>{description}</span></li>)}</ol><p className="muted small">All six stages are illustrative demo states. No courier or live fulfilment updates are connected.</p></div>}{!result&&<p className="muted small">Demo tracking only. Your order ID and checkout phone number are matched against orders saved in this browser.</p>}</form></main>;
}

export function PolicyDetailPage({kind}:{kind:'shipping'|'returns'|'privacy'|'terms'}){
 const content={shipping:{title:'Shipping Policy',desc:'Delivery charges, service areas and estimated delivery times should be confirmed by the store before launch.',heading:'Delivery information',body:'The current storefront demo displays a ৳120 delivery fee for orders below ৳8,000 and complimentary delivery for orders of ৳8,000 or more. Actual delivery availability and timelines must be confirmed by the business.'},returns:{title:'Return & Refund Policy',desc:'Please review the conditions for return requests and refunds.',heading:'Returns and refunds',body:'Publish the final return window, eligible product condition, sealed-item rules and refund process here. Fragrance products may have hygiene-related restrictions. Confirm the final terms with applicable local requirements.'},privacy:{title:'Privacy Policy',desc:'How customer information should be handled.',heading:'Customer information',body:'Customer information should only be used for order fulfilment, customer support and marketing messages where consent has been given. Add details about data retention, access, deletion and third-party services before launch.'},terms:{title:'Terms & Conditions',desc:'The terms that apply when customers use the storefront.',heading:'Store terms',body:'Product availability, prices, delivery charges and order acceptance should be confirmed by the store. These sample terms are placeholders and are not legal advice.'}}[kind];
 return <main className="page wrap narrow"><div className="page-heading"><span className="eyebrow gold">LUXE INFORMATION</span><h1>{content.title.split(' ').slice(0,-1).join(' ')} <em>{content.title.split(' ').slice(-1)}</em></h1><p>{content.desc}</p></div><h2>{content.heading}</h2><p className="body-copy">{content.body}</p><p className="muted small">This is editable placeholder content. Replace it with your verified business policy before publishing.</p><Link className="continue-link" href="/contact">Contact customer support</Link></main>;
}
