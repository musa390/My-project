import type { Metadata } from "next";
import { products } from "@/lib/products";
import { ProductPage } from "@/components/store";
export async function generateMetadata({params}:{params:Promise<{slug:string}>}):Promise<Metadata>{
 const {slug}=await params;
 const product=products.find(item=>item.slug===slug);
 return product?{title:product.name,description:product.description}:{title:"Perfume Not Found | LUXE"};
}
export default async function Page({params}:{params:Promise<{slug:string}>}){const {slug}=await params;return <ProductPage slug={slug}/>}
