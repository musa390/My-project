import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Shipping Policy', description: 'Review editable shipping policy information for the LUXE storefront.' };
import {PolicyDetailPage} from '@/components/store';
export default function Page(){return <PolicyDetailPage kind="shipping"/>}
