# Phase 13 — Frontend & Backend Integration

## Branch and baseline
- Branch: `phase13/frontend-backend-integration`
- Base commit: Phase 12 implementation `1a40bff56cc65a85869b5e408a9ba0382bd36c7b`
- Phase 12 PR #9 remains open and unmerged. Phase 13 does not merge it.
- Phase 13 changes are isolated from the base branch until reviewed and explicitly merged.

## Integration architecture
- Customer app: Next.js 15 / React 19 / TypeScript on port 3000.
- Admin app: Next.js 15 / React 19 / TypeScript on port 3001.
- Backend: Spring Boot REST API under `/api/v1`; MySQL is the source of truth.
- Frontend API clients use `NEXT_PUBLIC_API_BASE_URL`, credentialed fetch, HttpOnly cookies, CSRF bootstrap and normalized API errors.
- Safe GET requests can attempt a single session refresh. Unsafe requests are never automatically replayed after a 401.
- Backend CORS must explicitly allow both frontend origins.

## Implemented — Customer
- Register, login, current-user profile, logout, password-reset request and password reset.
- HttpOnly access/refresh cookies; tokens are never stored in localStorage.
- Public product catalogue with categories, brands, images, variants, available stock, featured/new/best-seller endpoints and approved review aggregates.
- Full product details are fetched on product pages.
- Server-backed cart and wishlist, including variant selection and quantity updates.
- Delivery-address CRUD and default-address selection.
- Server-calculated checkout quote; order creation uses backend pricing, coupon and stock rules.
- Customer order history, details, cancellation and order tracking.
- Payment UI exposes only supported Cash on Delivery. COD payment request is sent to the backend; no fake success state is used.
- Newsletter double opt-in and contact-form submission.
- Loading, empty, validation and API error states on core flows.

## Implemented — Admin
- Login, current-session/role verification, protected workspace and logout.
- Product CRUD, publish/archive, variant creation and product image URL management.
- Category, brand and collection CRUD/deactivation.
- Order search and status updates.
- Customer directory and account status management (Admin-only status changes).
- Payment search, COD collection confirmation and manual refund-request queueing.
- Low-stock inventory and quantity adjustment.
- Return request queue and approve/reject decisions.
- Contact inbox and status updates.
- Coupon CRUD/deactivation.
- Banner CRUD/deactivation.
- Product review moderation.
- Newsletter subscriber listing and unsubscribe.
- Live analytics aggregates.
- Admin-only staff role management for existing staff accounts.

## Intentionally unavailable / separate work
- bKash and Nagad live gateway callbacks/transactions are not implemented in the existing backend payment phase; checkout does not pretend they work.
- Google OAuth remains disabled in the customer UI until provider credentials and callback configuration are verified in an authorized environment.
- Store Settings has no persistent backend settings API yet.
- Staff account creation is not exposed; staff role assignment applies to existing staff accounts.
- Customer self-service profile editing and review submission are not part of the current backend API contract.
- Product image management accepts secure HTTPS image URLs; the admin UI does not yet provide a direct file-picker-to-Cloudinary upload wizard.

## Security requirements
- Never expose backend JWTs or provider secrets to JavaScript/localStorage.
- Keep HttpOnly cookies; use `credentials: include` and `X-XSRF-TOKEN` for unsafe methods.
- Never trust client prices, totals, inventory, roles, permissions or payment status.
- Backend authorization is authoritative even when the frontend hides controls.
- Do not commit `.env.local`, passwords, API keys or provider secrets.

## Verification and completion gate
- Backend integration tests cover current-user, address ownership, staff customers, payments, coupons, banners, newsletter, reviews and analytics.
- Frontend CI runs the customer verification helper, TypeScript typecheck, ESLint and production builds for both apps.
- A green CI build is necessary but not sufficient.
- Final 100% sign-off also requires local end-to-end browser QA against a clean migrated MySQL database for customer and admin journeys.

## Local environment blocker
The previous local Maven run failed before application context creation with:
`Access denied for user 'luxe_app'@'localhost' (SQL State 28000, Error 1045)`.
This is a local MySQL credential/permission issue, not evidence of a Phase 13 application-code failure. Do not drop/reset the user's existing database. Resolve the local MySQL user configuration or use a separate clean verification database, then run backend tests and browser QA.

## Status
- [x] Repository audit and Phase 13 branch creation.
- [x] Shared API client, CSRF and safe session refresh foundation.
- [x] Customer authentication and password recovery.
- [x] Customer catalogue, product detail, cart and wishlist integration.
- [x] Customer address, checkout, orders, COD and tracking integration.
- [x] Customer contact and newsletter integration.
- [x] Admin authentication, role guard and CRUD/management integrations listed above.
- [x] Replace transactional demo data and client-only mutations in the main customer/admin flows.
- [x] Final automated CI pass: Backend 80 tests, 0 failures/errors; Flyway 9 migrations validated; customer/admin typecheck, lint, production build and route smoke tests passed. [Backend CI](https://github.com/musa390/My-project/actions/runs/44244391311) · [Frontend CI](https://github.com/musa390/My-project/actions/runs/44244440780).
- [ ] Local MySQL authentication repair / isolated clean database verification.
- [ ] Browser end-to-end QA for customer and admin journeys.
- [ ] Explicit Phase 13 completion sign-off.

## Pull request
- Draft PR #10: https://github.com/musa390/My-project/pull/10
- Base: `phase12/additional-integrations`; head: `phase13/frontend-backend-integration`.
- PR #10 is open, draft, mergeable and has not been merged.
