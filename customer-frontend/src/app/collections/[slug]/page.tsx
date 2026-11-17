import type { Metadata } from 'next';
import {ProductCard} from '@/components/store';
import {products} from '@/lib/products';
export async function generateMetadata({params}:{params:Promise<{slug:string}>}):Promise<Metadata>{const {slug}=await params;const label=decodeURIComponent(slug).replace(/-/g,' ');return {title:`${label.charAt(0).toUpperCase()+label.slice(1)} Collection`,description:`Explore LUXE ${label} fragrance collection.`};}
export default async function Page({params}:{params:Promise<{slug:string}>}){
 const {slug}=await params;const title=decodeURIComponent(slug).replace(/-/g,' ');const matches=products.filter(p=>p.collection.toLowerCase()===title.toLowerCase());
 return <main className="page wrap"><div className="page-heading"><span className="eyebrow gold">THE LUXE EDIT</span><h1>{title.split(' ').map(x=>x.charAt(0).toUpperCase()+x.slice(1)).join(' ')} <em>Collection</em></h1><p>Explore fragrances from this collection.</p></div>{matches.length?<div className="product-grid">{matches.map(p=><ProductCard key={p.id} p={p}/>)}</div>:<p className="muted">This collection is being curated. <a href="/shop">Browse all fragrances.</a></p>}</main>;
}
