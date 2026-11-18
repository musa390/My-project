import type { Metadata } from "next";
import { CollectionDetailPage } from "@/components/store";

export async function generateMetadata({params}:{params:Promise<{slug:string}>}):Promise<Metadata>{
  const {slug}=await params;
  const label=decodeURIComponent(slug).replace(/-/g," ");
  return {title:`${label.charAt(0).toUpperCase()+label.slice(1)} Collection`,description:`Explore LUXE ${label} fragrances from the live collection catalogue.`};
}

export default async function Page({params}:{params:Promise<{slug:string}>}){
  const {slug}=await params;
  return <CollectionDetailPage slug={slug}/>;
}
