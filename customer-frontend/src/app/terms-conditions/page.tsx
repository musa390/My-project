import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Terms & Conditions', description: 'Review editable terms and conditions for the LUXE storefront.' };
import {PolicyDetailPage} from '@/components/store';
export default function Page(){return <PolicyDetailPage kind="terms"/>}
