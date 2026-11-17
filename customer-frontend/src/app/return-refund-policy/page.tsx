import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Return & Refund Policy', description: 'Review editable return and refund policy information for the LUXE storefront.' };
import {PolicyDetailPage} from '@/components/store';
export default function Page(){return <PolicyDetailPage kind="returns"/>}
