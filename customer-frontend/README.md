# LUXE PERFUME — Customer Frontend

Next.js 15 / React 19 / TypeScript storefront integrated with the LUXE Spring Boot REST API.

## Live integrations

- Customer register, login, current session, logout and password reset.
- HttpOnly cookie session with CSRF token bootstrap; safe GET requests can refresh an expired session.
- Product listing, search/filter source data, product details, variants, primary images, stock and approved reviews from the backend catalogue.
- Server-backed cart and wishlist.
- Customer delivery-address create, list, update, delete and default selection.
- Server-authoritative checkout quote, order creation, order history, order details, cancellation and tracking.
- Cash on Delivery request flow. No online payment is represented as completed.
- Newsletter double opt-in and contact form submission.

Editorial brand copy and decorative fallback imagery may remain local. Transactional product, cart, address, checkout and order data must come from the backend.

## Run locally

Node.js 20.9 or newer is recommended.

Create `.env.local` in this directory:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_SITE_URL=http://localhost:3000
```

Then run:

```bash
npm ci
npm run typecheck
npm run lint
npm run build
npm run dev
```

Open `http://localhost:3000`.

The backend CORS allowlist must include the exact customer origin. The backend must be running with a valid MySQL database and migrations applied.

## Routes

- `/`, `/shop`, `/search`, `/best-sellers`, `/new-arrivals`
- `/collections`, `/collections/[slug]`, `/products/[slug]`
- `/cart`, `/checkout`, `/order-success`
- `/wishlist`, `/account`, `/login`, `/register`, `/forgot-password`
- `/my-orders`, `/orders`, `/track-order`
- `/about`, `/contact`, `/faq`, and policy routes

## Security and payment boundaries

- JWT access and refresh tokens are HttpOnly cookies; the frontend does not read or store them in localStorage.
- Unsafe API methods send the CSRF token in `X-XSRF-TOKEN`.
- Client-side prices and stock are display hints only; checkout quote and order creation revalidate server-side.
- Cart and wishlist mutations require an authenticated customer session.
- Only backend-supported Cash on Delivery is enabled in checkout. bKash/Nagad live gateway integration remains a separate payment phase.
- Google OAuth is not enabled in the UI until authorized provider credentials and callback configuration are verified.
- Never commit `.env.local`, passwords, API keys or provider secrets.

## Verification

The Phase 13 GitHub Actions workflow runs:

```bash
npm run verify:phase6
npm run typecheck
npm run lint
npm run build
```

Automated CI does not replace local browser QA. Verify register/login/logout, session refresh, product browsing, cart/wishlist persistence, address CRUD, checkout quote, COD order, order history/cancellation/tracking, contact and newsletter flows against a clean local database.
