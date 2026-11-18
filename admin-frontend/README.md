# LUXE PERFUME — Admin Frontend

Next.js 15 / React 19 / TypeScript admin workspace connected to the authenticated LUXE Spring Boot API.

## Live integrations

- Admin/Manager login, current-session verification, role checks, logout and HttpOnly cookie session.
- Products: create, edit, publish, archive and manage image URLs.
- Categories, brands and collections: create, edit and deactivate.
- Orders: search and update allowed order statuses.
- Customers: directory, detail-ready records and Admin-only account suspension/reactivation.
- Payments: search and confirm supported Cash on Delivery collections.
- Inventory: low-stock list and audited quantity adjustments.
- Returns: review queue and approve/reject decisions.
- Contact inbox: search and status updates.
- Coupons and banners: CRUD/deactivation.
- Reviews: moderation.
- Newsletter: subscriber list and staff unsubscribe.
- Analytics: live database aggregates.
- Staff & Roles: Admin-only role assignment for existing staff accounts.

Settings does not yet have a persistent backend settings API and is intentionally shown as pending. Staff account creation is not exposed here; role assignment applies to existing staff accounts only.

## Run locally

Create `.env.local`:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

Then run:

```bash
npm install
npm run typecheck
npm run lint
npm run build
npm run dev
```

Open `http://localhost:3001`. The customer storefront uses port 3000.

The backend CORS allowlist must include `http://localhost:3001`. A valid staff account with the required backend role/permissions is required.

## Security

- The admin app verifies the backend session on protected routes.
- Only Admin/Manager roles can enter the workspace; Staff & Roles is Admin-only.
- Backend method security remains authoritative. Hiding a control in the UI is not a substitute for backend authorization.
- JWTs remain HttpOnly cookies. CSRF headers are attached to unsafe methods.
- Do not put passwords, API keys, provider secrets or customer credentials in frontend environment variables or source control.

## Verification

```bash
npm install
npm run typecheck
npm run lint
npm run build
```

After CI passes, complete local browser QA for login/role denial, products and images, taxonomy CRUD, inventory adjustments, order status, COD collection, customer status, return decisions, contact messages, coupons, banners, reviews, newsletter and analytics.
