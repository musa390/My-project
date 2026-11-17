import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Best-Selling Perfumes', description: 'Discover the fragrances featured as LUXE demo best sellers.' };
import {BestSellersPage} from '@/components/store';
export default function Page(){return <BestSellersPage/>}
