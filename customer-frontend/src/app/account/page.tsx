import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Customer Account', robots: { index: false, follow: false }, description: 'Manage your local LUXE demo profile and saved delivery addresses.' };
import {AccountPage} from '@/components/store';
export default function Page(){return <AccountPage/>;}
