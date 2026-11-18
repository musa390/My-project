import { apiRequest } from "@/lib/api-client";

export type ApiEnvelope<T> = { success: boolean; message: string; data: T; timestamp: string };
export type PageResult<T> = { items: T[]; page: number; size: number; totalElements: number; totalPages: number };

async function data<T>(path: string, options?: Parameters<typeof apiRequest<ApiEnvelope<T>>>[1]): Promise<T> {
  const response = await apiRequest<ApiEnvelope<T>>(path, options);
  if (!response?.success) throw new Error(response?.message || "The server returned an unsuccessful response.");
  return response.data;
}

export type CatalogProduct = {
  id: number; name: string; slug: string; short_description?: string; product_type?: string;
  category_name?: string; category_slug?: string; brand_name?: string; image_url?: string;
  price_min?: number | string; price_max?: number | string; available?: boolean;
};
export type ProductDetail = CatalogProduct & {
  description?: string;
  images?: Array<{ id: number; image_url: string; alt_text?: string; is_primary?: boolean }>;
  variants?: Array<{ id: number; variant_name: string; sku: string; price: number | string; available_quantity: number; volume_ml?: number }>;
};

export async function getCatalog(params: {
  q?: string; category?: string; brand?: string; minPrice?: number; maxPrice?: number;
  inStock?: boolean; sort?: string; page?: number; size?: number;
} = {}): Promise<PageResult<CatalogProduct>> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key,value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  return data<PageResult<CatalogProduct>>(`/api/v1/catalog/products?${query.toString()}`);
}
export async function getProduct(slug: string): Promise<ProductDetail> {
  return data<ProductDetail>(`/api/v1/catalog/products/${encodeURIComponent(slug)}`);
}
export async function getCategories(): Promise<Array<Record<string, unknown>>> {
  return data("/api/v1/catalog/categories");
}
export async function getCollections(): Promise<Array<Record<string, unknown>>> {
  return data("/api/v1/catalog/collections");
}

export type Address = {
  id: number; label: string; recipient_name: string; phone: string;
  address_line1: string; address_line2?: string | null; area?: string | null;
  city: string; district?: string | null; postal_code?: string | null;
  country_code: string; is_default: boolean;
};
export type AddressInput = Omit<Address, "id" | "is_default"> & { makeDefault: boolean };
export async function getAddresses(): Promise<Address[]> { return data("/api/v1/addresses"); }
export async function createAddress(input: AddressInput): Promise<Address> {
  return data("/api/v1/addresses", { method: "POST", body: input });
}
export async function updateAddress(id: number, input: AddressInput): Promise<Address> {
  return data(`/api/v1/addresses/${id}`, { method: "PUT", body: input });
}
export async function deleteAddress(id: number): Promise<void> {
  await data(`/api/v1/addresses/${id}`, { method: "DELETE" });
}
export async function setDefaultAddress(id: number): Promise<Address> {
  return data(`/api/v1/addresses/${id}/default`, { method: "PATCH" });
}

export type CartItem = {
  item_id: number; variant_id: number; product_id: number; product_name: string;
  product_slug: string; variant_name: string; quantity: number; currentUnitPrice: number | string;
  lineTotal: number | string; availableStock: number; image_url?: string;
};
export type Cart = { id: number; currency: string; items: CartItem[]; subtotal: number | string; itemCount: number; lineCount: number; allItemsAvailable: boolean };
export async function getCart(): Promise<Cart> { return data("/api/v1/cart"); }
export async function addCartItem(variantId: number, quantity = 1): Promise<Cart> {
  return data("/api/v1/cart/items", { method: "POST", body: { variantId, quantity } });
}
export async function updateCartItem(itemId: number, quantity: number): Promise<Cart> {
  return data(`/api/v1/cart/items/${itemId}`, { method: "PUT", body: { quantity } });
}
export async function removeCartItem(itemId: number): Promise<Cart> {
  return data(`/api/v1/cart/items/${itemId}`, { method: "DELETE" });
}

export type Wishlist = { id: number; name: string; items: Array<{ product_id: number; name: string; slug: string; starting_price: number | string; image_url?: string }>; itemCount: number };
export async function getWishlist(): Promise<Wishlist> { return data("/api/v1/wishlist"); }
export async function addWishlistItem(productId: number): Promise<Wishlist> {
  return data(`/api/v1/wishlist/items/${productId}`, { method: "POST" });
}
export async function removeWishlistItem(productId: number): Promise<Wishlist> {
  return data(`/api/v1/wishlist/items/${productId}`, { method: "DELETE" });
}

export type CheckoutQuote = {
  addressId: number; recipientName: string; phone: string; addressLine1: string;
  area?: string; city: string; district?: string; customerEmail: string; currency: string;
  couponCode?: string; totals: { subtotal: number; discountTotal: number; shippingTotal: number; taxTotal: number; grandTotal: number };
  items: Array<{ productName: string; variantName: string; quantity: number; unitPrice: number; lineTotal: number }>;
  stockAvailable: boolean; notice: string;
};
export async function previewCheckout(addressId: number, couponCode?: string): Promise<CheckoutQuote> {
  return data("/api/v1/checkout/preview", { method: "POST", body: { addressId, couponCode } });
}
export type PlacedOrder = {
  orderId: number; orderNumber: string; orderStatus: string; paymentStatus: string;
  currency: string; grandTotal: number; shippingRecipient: string; shippingPhone: string;
  placedAt: string;
};
export async function placeOrder(addressId: number, couponCode?: string, customerNote?: string): Promise<PlacedOrder> {
  return data("/api/v1/orders", { method: "POST", body: { addressId, couponCode, customerNote } });
}
export type CustomerOrder = Record<string, unknown> & { orderNumber: string; orderStatus: string; paymentStatus: string; grandTotal: number };
export async function getOrders(page = 0, size = 10): Promise<PageResult<CustomerOrder>> {
  return data(`/api/v1/orders?page=${page}&size=${size}`);
}
export async function getOrder(orderNumber: string): Promise<CustomerOrder> {
  return data(`/api/v1/orders/${encodeURIComponent(orderNumber)}`);
}
export async function getOrderTracking(orderNumber: string): Promise<Record<string, unknown>> {
  return data(`/api/v1/orders/${encodeURIComponent(orderNumber)}/tracking`);
}
export async function cancelOrder(orderNumber: string, reason?: string): Promise<Record<string, unknown>> {
  return data(`/api/v1/orders/${encodeURIComponent(orderNumber)}/cancel`, { method: "POST", body: { reason } });
}
export async function requestCodPayment(orderNumber: string, idempotencyKey: string): Promise<Record<string, unknown>> {
  return data(`/api/v1/orders/${encodeURIComponent(orderNumber)}/payments/cod`, {
    method: "POST", headers: { "Idempotency-Key": idempotencyKey }, body: {},
  });
}
export async function getPaymentHistory(orderNumber: string): Promise<Array<Record<string, unknown>>> {
  return data(`/api/v1/orders/${encodeURIComponent(orderNumber)}/payments`);
}
export async function subscribeNewsletter(email: string): Promise<Record<string, unknown>> {
  return data("/api/v1/newsletter/subscribe", { method: "POST", body: { email, source: "customer-website" } });
}
export async function submitContact(input: { name: string; email: string; subject: string; message: string }): Promise<Record<string, unknown>> {
  return data("/api/v1/contact", { method: "POST", body: input });
}
