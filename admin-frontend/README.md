# LUXE PERFUME — Admin Frontend

**Phase 3 status:** Interface implementation in progress; local runtime and browser QA must be confirmed by the project owner.

## Stack
- Next.js 15.5.29 App Router
- React 19.1 + TypeScript 5
- Tailwind CSS 4
- lucide-react

## Run locally
```bash
npm install
npm run verify:phase3
npm run typecheck
npm run lint
npm run build
npm run dev
``
Open `http://localhost:3000`.

## Admin modules
Dashboard, Login placeholder, Products, Categories, Brands, Collections, Orders, Customers, Inventory, Payments, Coupons & Discounts, Reviews, Banners, Newsletter, Analytics, Staff & Roles and Settings.

## Demo-only behaviour
Management pages include searchable/filterable demo tables and temporary create/edit/delete/status interactions. Changes live only in client-side page state and are lost on refresh. Dashboard metrics and records are illustrative. Payments do not process money; newsletter does not send email; staff roles are not enforced; login does not authenticate. Do not enter real credentials or customer data.

## Architecture boundaries
- This is a separate app; do not place Admin pages in `customer-frontend/`.
- No API, database, authentication, payment provider or live store operation is connected.
- All data must remain labelled demo until backend and database integration.
- Customer Website source is protected and must not be changed during Phase 3 without explicit approval.

## QA
Run `npm run verify:phase3`, `npm run typecheck`, `npm run lint` and `npm run build`. Then manually check all routes and interactions at mobile, tablet and desktop widths. Phase 3 must not be marked 100% until those checks pass.
