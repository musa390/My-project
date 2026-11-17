import type { Metadata } from "next";
export const metadata: Metadata = { title: 'My Orders', robots: { index: false, follow: false }, description: 'Review demo orders saved in this browser.' };
import {OrdersPage} from '@/components/store';

export default function Page(){
  return <OrdersPage/>;
}
