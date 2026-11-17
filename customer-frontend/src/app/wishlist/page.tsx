import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Wishlist', robots: { index: false, follow: false }, description: 'View your saved LUXE fragrances.' };
import {WishlistPage} from '@/components/store'; export default function Page(){return <WishlistPage/>}
