# Phase 13 — Frontend & Backend Integration

## Branch and baseline
- Branch: `phase13/frontend-backend-integration`
- Base: Phase 12 implementation commit `1a40bff56cc65a85869b5e408a9ba0382bd36c7b`
- Phase 12 PR #9 remains open and unmerged. Phase 13 does not merge it.

## Baseline audit
- Customer frontend: Next.js 15 / React 19 / TypeScript; primary storefront is currently implemented in `customer-frontend/src/components/store.tsx` and imports a hard-coded product catalog from `src/lib/products.ts`.
- Admin frontend: Next.js 15 / React 19 / TypeScript; `management-workspace.tsx` currently uses in-memory demo records. Admin README explicitly says no API/database/auth is connected.
- Backend exposes versioned REST endpoints under `/api/v1`, uses HttpOnly JWT cookies (`lp_access`, `lp_refresh`), and enables cookie-based CSRF protection.
- Existing endpoints include auth, public catalog, cart, wishlist, checkout preview, customer orders, COD payment requests, staff catalog, staff orders, staff inventory and staff payment operations.
- Cart and wishlist require authentication. Checkout preview requires authentication. Staff endpoints enforce roles/permissions.

## Delivery sequence
1. Shared integration contract: API base URL, typed fetch/error handling, credentials, CSRF bootstrap, safe session refresh.
2. Customer authentication: register/login/logout/refresh/current-session handling; protected-route UX.
3. Customer catalog: replace hard-coded product listing/detail data with public catalog API; preserve editorial/brand content only where it is not transactional product data.
4. Customer cart and wishlist: server-persisted operations and resilient loading/empty/error states.
5. Checkout and orders: server quote is authoritative; submit order using the existing backend contract; order history/detail/tracking/cancellation.
6. Payment UI: connect only to supported backend payment methods and clearly show unavailable/deferred providers; never simulate a successful payment.
7. Admin authentication and authorization: staff session and permission-aware navigation/actions.
8. Admin CRUD: products, variants, images, categories, brands, collections, inventory, orders, customers, payments, newsletter and contact workflows according to existing backend endpoints.
9. Remove demo fixtures and client-only mutations from production workflows; keep optional seeded demo data isolated from runtime production paths.
10. Verification: frontend lint/typecheck/build, backend tests, API contract tests, database migration checks, browser QA for customer/admin flows, and local end-to-end verification.

## Security requirements
- Never expose backend JWTs or provider secrets to JavaScript/localStorage.
- Keep HttpOnly cookies; use `credentials: include` and the CSRF cookie/header protocol for unsafe methods.
- Never trust client prices, totals, inventory, roles, permissions or payment status.
- Do not store real customer credentials or payment data in demo fixtures.
- Keep CORS allowlist and cookie settings aligned with the actual local/deployed origins.

## Completion criteria
Phase 13 is complete only when customer and admin flows use real backend/database data, demo-only mutations are removed from production flows, automated checks pass, and browser end-to-end tests confirm the critical journeys. A successful build alone is not completion.

## Status
- [x] Repository audit and Phase 13 branch creation.
- [x] API client and CSRF/session foundation (customer + admin clients, credentialed fetch, CSRF bootstrap, error normalization).
- [x] Customer authentication foundation (register/login/current-user API and connected sign-in/register form).
- [ ] Customer catalog.
- [ ] Cart and wishlist.
- [ ] Checkout, orders and payment UI.
- [ ] Admin authentication and CRUD.
- [ ] Demo-data removal.
- [ ] Automated and browser verification.
