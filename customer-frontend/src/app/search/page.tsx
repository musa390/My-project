import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Search Perfumes', robots: { index: false, follow: false }, description: 'Search LUXE perfumes by name, brand, collection and fragrance notes.' };
import {SearchPage} from '@/components/store';
export default function Page(){return <SearchPage/>}
