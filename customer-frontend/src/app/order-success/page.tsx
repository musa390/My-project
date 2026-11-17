import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Demo Order Confirmation', robots: { index: false, follow: false }, description: 'View the confirmation details for a demo LUXE order.' };
import {OrderSuccessPage} from '@/components/store';
export default function Page(){return <OrderSuccessPage/>}
