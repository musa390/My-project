import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Privacy Policy', description: 'Review editable privacy policy information for the LUXE storefront.' };
import {PolicyDetailPage} from '@/components/store';
export default function Page(){return <PolicyDetailPage kind="privacy"/>}
