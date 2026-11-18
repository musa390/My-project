import type { Product, ProductReview } from "@/lib/products";
import { products as visualFallbacks } from "@/lib/products";
import type { CatalogProduct, ProductDetail } from "@/lib/commerce";

type VariantLike = { id?: number; variantName?: string; variant_name?: string; price?: number|string; availableQuantity?: number; available_quantity?: number };
function variantsFrom(value: unknown): VariantLike[] {
  if (Array.isArray(value)) return value as VariantLike[];
  if (typeof value === "string") {
    try { const parsed: unknown = JSON.parse(value); return Array.isArray(parsed) ? parsed as VariantLike[] : []; }
    catch { return []; }
  }
  return [];
}
function volumeOf(variant: VariantLike): "30ml"|"50ml"|"100ml"|null {
  const name=String(variant.variantName||variant.variant_name||"").toLowerCase();
  const match=name.match(/(30|50|100)\s*ml/);
  return match ? (match[1]+"ml") as "30ml"|"50ml"|"100ml" : null;
}
export function toStoreProduct(record: CatalogProduct | ProductDetail, index = 0): Product {
  const detail=record as ProductDetail;
  const variants=variantsFrom((record as CatalogProduct & {variants?:unknown}).variants);
  const primaryImage=detail.images?.find(image=>image.is_primary)?.image_url || detail.images?.[0]?.image_url || record.image_url || "";
  const fallback=visualFallbacks[index % visualFallbacks.length];
  const stockBySize:Product["stockBySize"]={"30ml":0,"50ml":0,"100ml":0};
  for(const variant of variants){
    const size=volumeOf(variant);
    if(size)stockBySize[size]=Number(variant.availableQuantity??variant.available_quantity??0);
  }
  const prices=variants.map(v=>Number(v.price||0)).filter(Number.isFinite);
  const rawReviews=(detail as ProductDetail & {reviews?:Array<Record<string,unknown>>}).reviews||[];
  const reviews:ProductReview[]=rawReviews.map(review=>({name:[review.first_name,review.last_name].filter(Boolean).join(" ")||"Customer",rating:Number(review.rating||0),title:String(review.title||""),comment:String(review.body||""),date:String(review.created_at||"").slice(0,10)}));
  const minPrice=Number(record.price_min??(prices.length?Math.min(...prices):0));

  const image=primaryImage||fallback.image;
  const images=detail.images?.map(item=>item.image_url).filter(Boolean) || (primaryImage?[primaryImage]:[]);
  return {
    id:String(record.id),slug:record.slug,name:record.name,brand:record.brand_name||String((detail as Record<string,unknown>).brand_name||"LUXE PERFUME"),
    gender:"Unisex",collection:record.category_name||"Fragrance",price:minPrice,
    size:variants[0]?(String(variants[0].variantName||variants[0].variant_name||"100ml")):"100ml",
    notes:record.short_description||detail.description||"",description:detail.description||record.short_description||"",
    image,images,badge:record.is_featured?"Featured":undefined,rating:Number((record as CatalogProduct & {rating_avg?:number|string}).rating_avg||0),reviewCount:Number((record as CatalogProduct & {review_count?:number}).review_count||reviews.length),
    topNotes:[],heartNotes:[],baseNotes:[],fragranceFamily:record.category_name||"Fragrance",
    longevity:"Not specified",sillage:"Not specified",
    stock:Number((record as CatalogProduct & {available_stock?:number}).available_stock??(record.available?1:0)),
    stockBySize,reviews
  };
}
