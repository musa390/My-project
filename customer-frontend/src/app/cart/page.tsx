import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Shopping Bag', robots: { index: false, follow: false }, description: 'Review the perfumes and sizes in your LUXE demo shopping bag.' };
import {CartPage} from '@/components/store'; export default function Page(){return <CartPage/>}
