import type { Metadata } from "next";
import { getProduct } from "@/lib/commerce";
import { ProductPage } from "@/components/store";

export async function generateMetadata({params}:{params:Promise<{slug:string}>}):Promise<Metadata>{
  const {slug}=await params;
  try{
    const product=await getProduct(slug);
    return {title:product.name,description:product.description||product.short_description||`Explore ${product.name} from the LUXE fragrance catalogue.`};
  }catch{
    return {title:"Perfume Not Found | LUXE"};
  }
}

export default async function Page({params}:{params:Promise<{slug:string}>}){
  const {slug}=await params;
  return <ProductPage slug={slug}/>;
}
