# LUXE PERFUME — Admin Frontend

A separate administration application for the LUXE PERFUME store.

## Stack
- Next.js 15.5.29 App Router
- React 19.1.0 + TypeScript
- Plain CSS with a responsive luxury dark/gold design system
- Lucide React icons

## Local development
```bash
cd admin-frontend
npm install
npm run dev
```
Open http://localhost:3000.

## Routes
- `/` — Dashboard
- `/login` — Admin sign-in UI (demo only)
- `/products`, `/categories`, `/brands`, `/collections`
- `/orders`, `/customers`, `/inventory`, `/payments`
- `/coupons`, `/reviews`, `/banners`, `/newsletter`
- `/analytics`, `/staff`, `/settings`

## Important boundaries
All displayed records and metrics are illustrative demo data. The login form does not authenticate users. Management controls are presentation-only and do not persist changes. No backend API, database, payment provider, or real customer data is connected. Do not enter real credentials. Real operations require the later Spring Boot API, authentication, authorization and audit logging phases.

## Verification
Run `npm install`, `npm run typecheck`, `npm run lint`, and `npm run build` locally. Browser-test the dashboard, every management route, login UI, keyboard navigation, and mobile layouts before marking Phase 3 complete.
