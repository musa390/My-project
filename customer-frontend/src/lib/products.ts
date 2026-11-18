export type ProductReview = {
  name: string;
  rating: number;
  title: string;
  comment: string;
  date: string;
};

export type Product = {
  id: string;
  slug: string;
  name: string;
  brand: string;
  gender: string;
  collection: string;
  price: number;
  oldPrice?: number;
  size: string;
  notes: string;
  description: string;
  image: string;
  images: string[];
  badge?: string;
  rating: number;
  reviewCount: number;
  topNotes: string[];
  heartNotes: string[];
  baseNotes: string[];
  fragranceFamily: string;
  longevity: string;
  sillage: string;
  stock: number;
  stockBySize: Record<"30ml" | "50ml" | "100ml", number>;
  priceBySize?: Partial<Record<"30ml" | "50ml" | "100ml", number>>;
  reviews: ProductReview[];
};

/** Uses exact server-returned variant pricing when available. */
export const productPrice = (product: Product, size: string): number =>
  product.priceBySize?.[size as keyof Product["priceBySize"]] ?? product.price;

/** Stock values come from available inventory returned by the backend. */
export const productStock = (product: Product, size: string): number =>
  product.stockBySize[size as keyof Product["stockBySize"]] ?? 0;

export const money = (amount: number): string => `৳${amount.toLocaleString("en-BD")}`;
