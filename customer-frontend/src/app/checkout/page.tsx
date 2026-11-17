import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Checkout', robots: { index: false, follow: false }, description: 'Enter delivery details and select a demo payment method for your LUXE order.' };
import {CheckoutPage} from '@/components/store'; export default function Page(){return <CheckoutPage/>}
