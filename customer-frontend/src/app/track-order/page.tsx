import type { Metadata } from "next";
export const metadata: Metadata = { title: 'Track Demo Order', robots: { index: false, follow: false }, description: 'Look up a local demo order using its reference and checkout phone number.' };
import {TrackOrderPage} from '@/components/store';
export default function Page(){return <TrackOrderPage/>}
