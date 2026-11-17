import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Google Sign-In Setup', robots: { index: false, follow: false }, description: 'Google sign-in is not enabled in this frontend-only demo.' };
import {LoginPage} from '@/components/store';
export default function Page(){return <LoginPage/>;}
