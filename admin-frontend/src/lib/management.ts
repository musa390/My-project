import { apiRequest } from "@/lib/api-client";

type Envelope<T> = { success: boolean; message: string; data: T };
async function data<T>(path: string, options?: Parameters<typeof apiRequest<Envelope<T>>>[1]): Promise<T> {
  const result = await apiRequest<Envelope<T>>(path, options);
  if (!result?.success) throw new Error(result?.message || "The server returned an unsuccessful response.");
  return result.data;
}
export type Page<T> = { items: T[]; page: number; size: number; totalElements: number; totalPages: number };
export type CatalogRecord = {
  id: number; name: string; slug: string; status?: string; category_name?: string;
  brand_name?: string; price_min?: number; is_featured?: boolean;
};
export type TaxonomyRecord = { id: number; name: string; slug: string; description?: string; is_active?: boolean };
export type StaffOrder = {
  id: number; orderNumber: string; orderStatus: string; paymentStatus: string;
  customerEmail: string; shippingRecipient: string; grandTotal: number; itemCount: number;
};
export type InventoryRecord = { variantId?: number; variant_id?: number; productName?: string; product_name?: string; variantName?: string; variant_name?: string; quantityOnHand?: number; quantity_on_hand?: number; quantityReserved?: number; quantity_reserved?: number; reorderLevel?: number; reorder_level?: number };
export type ContactMessage = { id: number; full_name: string; email: string; subject: string; message: string; status: string; created_at: string };

export const adminApi = {
  products: (q = "") => data<Page<CatalogRecord>>("/api/v1/staff/catalog/products?q=" + encodeURIComponent(q) + "&page=0&size=100"),
  createProduct: (body: Record<string,unknown>) => data<CatalogRecord>("/api/v1/staff/catalog/products", { method: "POST", body }),
  updateProduct: (id: number, body: Record<string,unknown>) => data<CatalogRecord>("/api/v1/staff/catalog/products/" + id, { method: "PUT", body }),
  archiveProduct: (id: number) => data<CatalogRecord>("/api/v1/staff/catalog/products/" + id, { method: "DELETE" }),
  createVariant: (id: number, body: Record<string,unknown>) => data<Record<string,unknown>>("/api/v1/staff/catalog/products/" + id + "/variants", { method: "POST", body }),
  categories: () => data<TaxonomyRecord[]>("/api/v1/staff/catalog/categories"),
  createCategory: (body: Record<string,unknown>) => data<TaxonomyRecord>("/api/v1/staff/catalog/categories", { method: "POST", body }),
  updateCategory: (id:number, body:Record<string,unknown>) => data<TaxonomyRecord>("/api/v1/staff/catalog/categories/" + id, { method:"PUT", body }),
  archiveCategory: (id:number) => data<TaxonomyRecord>("/api/v1/staff/catalog/categories/" + id, { method:"DELETE" }),
  brands: () => data<TaxonomyRecord[]>("/api/v1/staff/catalog/brands"),
  createBrand: (body:Record<string,unknown>) => data<TaxonomyRecord>("/api/v1/staff/catalog/brands", {method:"POST",body}),
  updateBrand: (id:number,body:Record<string,unknown>) => data<TaxonomyRecord>("/api/v1/staff/catalog/brands/" + id,{method:"PUT",body}),
  archiveBrand: (id:number) => data<TaxonomyRecord>("/api/v1/staff/catalog/brands/" + id,{method:"DELETE"}),
  collections: () => data<TaxonomyRecord[]>("/api/v1/staff/catalog/collections"),
  createCollection: (body:Record<string,unknown>) => data<TaxonomyRecord>("/api/v1/staff/catalog/collections",{method:"POST",body}),
  updateCollection: (id:number,body:Record<string,unknown>) => data<TaxonomyRecord>("/api/v1/staff/catalog/collections/" + id,{method:"PUT",body}),
  archiveCollection: (id:number) => data<TaxonomyRecord>("/api/v1/staff/catalog/collections/" + id,{method:"DELETE"}),
  orders: (q = "") => data<Page<StaffOrder>>("/api/v1/staff/orders?q=" + encodeURIComponent(q) + "&page=0&size=100"),
  updateOrderStatus: (orderNumber:string,status:string,note?:string) => data<Record<string,unknown>>("/api/v1/staff/orders/" + encodeURIComponent(orderNumber) + "/status",{method:"PATCH",body:{status,note}}),
  lowStock: () => data<InventoryRecord[]>("/api/v1/staff/inventory/low-stock"),
  adjustInventory: (variantId:number,quantityChange:number,note:string) => data<Record<string,unknown>>("/api/v1/staff/inventory/variants/" + variantId + "/adjustments",{method:"POST",body:{quantityChange,note}}),
  contacts: (status?:string) => data<{items:ContactMessage[];page:number;size:number}>("/api/v1/staff/contact-messages?page=0&size=100" + (status ? "&status=" + encodeURIComponent(status) : "")),
  updateContactStatus: (id:number,status:string) => data<Record<string,unknown>>("/api/v1/staff/contact-messages/" + id + "/status",{method:"PATCH",body:{status}}),
  returns: () => data<Array<Record<string,unknown>>>("/api/v1/staff/returns"),
};
