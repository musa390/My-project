# LUXE PERFUME — Customer Storefront

Luxury-inspired, responsive customer-facing perfume storefront built with Next.js App Router, React and TypeScript.

## Scope

This package contains the **customer frontend only**. It does not include a backend, database, admin dashboard, real payment processing, or real Google OAuth. Cart, wishlist, account profile, saved addresses and demo orders use the current browser's Local Storage where noted.

## Included customer routes

- `/` — Home
- `/shop`, `/search`, `/best-sellers`, `/new-arrivals`
- `/collections` and `/collections/[slug]`
- `/products/[slug]`
- `/cart`, `/checkout`, `/order-success`
- `/wishlist`, `/account`, `/login`
- `/my-orders` (also `/orders`), `/track-order`
- `/about`, `/contact`, `/faq`
- `/shipping-policy`, `/return-refund-policy`, `/privacy-policy`, `/terms-conditions`
- Custom not-found page

## Technology

- Next.js 15 App Router
- React 19
- TypeScript
- Tailwind CSS 4 and custom responsive CSS
- Next.js Image for remote Unsplash image optimization and responsive loading
- Lucide React icons
- React Context API for cart and wishlist state
- Browser Local Storage for demo persistence

## Run locally

Node.js 20.9 or newer is recommended.

```bash
npm install
npm run dev
```

Open `http://localhost:3000`.

In VS Code, open this project folder and use **Terminal → Run Task** for the `LUXE:` install, typecheck, lint, build, development-server, or production-server task. The production task serves the completed build at `http://127.0.0.1:3187`. ESLint and Tailwind CSS IntelliSense are listed as recommended extensions.

## Phase 5 verification helper (Windows)

The portable source ZIP intentionally excludes `node_modules`. On Windows, open PowerShell in the project folder and run:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\verify-phase5.ps1
```

This installs dependencies for the current OS, runs TypeScript/ESLint/production build, starts the production server and smoke-tests the key routes. After it passes, complete the manual desktop/tablet/mobile checks in `BROWSER-QA-CHECKLIST.md`.

## Verification commands

```bash
npm run verify:phase6
npm run typecheck
npm run lint
npm run build
npm start
```

## Phase 1 — Routes, navigation and storefront structure

- Shared responsive header, footer and breadcrumb navigation.
- Active navigation state and accessible mobile menu toggle.
- Required customer routes and collection detail routes.
- All eight collection families: Signature, Lumière, Oud, Fresh, Floral, Woody, Amber and Citrus.
- Page metadata, Open Graph defaults, robots route and sitemap route. Set `NEXT_PUBLIC_SITE_URL` in `.env.local` to your canonical deployment URL for absolute sitemap URLs and metadata base.

## Phase 2 — Product system

- 12 demo perfume products with unique IDs and slugs.
- Product descriptions, brand, gender, collection, fragrance family, top/heart/base notes, longevity, sillage, ratings and sample reviews.
- Three-image gallery per product, selectable thumbnails, related products and recently viewed products.
- 30ml, 50ml and 100ml price tiers and size-specific demo stock.
- Product-card add-to-bag, wishlist, quick view, size selection, product detail actions and brief accessible toast feedback.
- Shop filters for collection/category, brand, gender, fragrance family, price and availability; search, sorting and load-more support.

## Phase 3 — Cart and checkout

- Cart and wishlist Local Storage persistence.
- Add/remove/update cart lines; selected bottle size is preserved.
- Cart quantities are capped by size-specific demo inventory.
- Checkout fields include customer contact details and Bangladesh delivery address fields.
- Postal code is optional; if entered, it must be four digits.
- Bangladesh mobile number validation for primary and alternative phone numbers.
- Demo discount codes: `LUXE10` (10%) and `WELCOME5` (5%), capped at ৳1,000.
- Delivery is shown as ৳120 below ৳8,000 and free at/above ৳8,000.
- Payment choices are **bKash, Nagad and Cash on Delivery only**. bKash/Nagad are selection-only; no payment is taken.
- Demo order ID, confirmation receipt, customer and item summary, and local order snapshot.

## Phase 4 — Account, search and order management

- Local profile and saved-address management with validation and storage error handling.
- Google sign-in UI is intentionally disabled; no fake authentication is created.
- Logout is unavailable because no authenticated session exists; local profile/address clearing is provided separately.
- Search by product name, brand, gender, scent notes, collection and fragrance family, with suggestions and empty state.
- Demo order history with expandable order details.
- Order tracking requires both order reference and matching checkout phone number. Bangladesh `01...` and `+880...` formats are normalized for comparison.
- Six illustrative tracking stages: Order Placed, Confirmed, Processing, Shipped, Out for Delivery and Delivered.

## Important demo limitations

- Product inventory, prices, discounts, ratings, reviews and tracking stages are sample frontend data.
- One sample product is intentionally marked sold out so the out-of-stock filter and disabled purchase state can be tested.
- Product images are remote Unsplash URLs; confirm image loading, licensing suitability and visual accuracy before launch.
- Contact details, social profile URLs, map location, brand history and policy text are editable placeholders. Replace them with verified business information.
- Newsletter forms are demo-only and do not send or store subscriptions. Contact form defaults to demo mode; optional Resend email delivery is available after owner configuration.
- Orders and account details are local to this browser and are not synced across devices.
- Real order processing, stock reservation, secure customer accounts, OAuth, payment collection and courier updates require a future backend and appropriate integrations.

## SEO, sitemap and robots

- Root metadata provides a default title template, description, Open Graph and Twitter summary metadata.
- Customer-facing pages define route-specific titles and descriptions; account, cart, checkout, search and order utility pages are marked `noindex` where appropriate.
- `src/app/sitemap.ts` builds a de-duplicated sitemap from the public routes, actual product collection names and product slugs.
- `src/app/robots.ts` allows public storefront pages and disallows private/demo utility routes and API endpoints.
- Set a real canonical URL in `.env.local` using `NEXT_PUBLIC_SITE_URL=https://your-real-domain.example`. Until the owner supplies the actual domain, the sitemap intentionally returns no absolute URLs and robots.txt omits its sitemap directive.

## Phase 6 quality refinements

- Added a keyboard-accessible skip-to-content link and visible focus styles.
- Tablet navigation switches to the accessible collapsible menu at 900px and below; the menu closes after navigation and with Escape.
- Added a shared route loading skeleton and reduced-motion support.
- Added responsive refinements for tablet, mobile and narrow 320–380px screens.
- Expanded Next.js image output formats and responsive image size configuration. Product and campaign imagery uses `next/image`; the hero background is also rendered through the Next.js image optimizer.
- Toast messages use a polite live region and atomic announcement.

## Verification status

Phase 5 implementation and its Windows production/browser verification are documented in [PHASE5-FINAL-VERIFICATION-REPORT.md](./PHASE5-FINAL-VERIFICATION-REPORT.md). Phase 6 changes and the verification limitations for the current build environment are recorded in [PHASE6-FINAL-VERIFICATION-REPORT.md](./PHASE6-FINAL-VERIFICATION-REPORT.md).

## Phase 5 — Home, About & Contact

The Phase 5 storefront includes the premium home hero and announcement bar, fragrance-family links, Signature/Lumière/Oud/Fresh collection tiles, product highlights (new arrivals, best sellers and trending), promotion/campaign areas, editable testimonial slots, and a demo newsletter interaction. The About page includes editable story/media, mission/vision/values, craftsmanship, sustainability and brand-timeline content. The Contact page includes validated fields, configurable business details, WhatsApp/email/social link support, a Google Maps embed placeholder and demo submission feedback.

### Demo business details

Edit `src/lib/site-config.ts` to replace the demo values with verified public business details. Keep unknown values blank. The page will show a clear placeholder rather than inventing a phone number, email, address, social profile or map location. Business hours currently display a clearly labelled sample schedule and should be replaced before launch.

### Contact form modes

The default is demo mode. Submissions show an on-page demo confirmation only; they are not emailed or stored.

For real email delivery, configure a Resend account and verified sending domain, then set these values in `.env.local` (never commit `.env.local`):

```env
NEXT_PUBLIC_CONTACT_FORM_MODE=email
CONTACT_FORM_MODE=email
RESEND_API_KEY=re_your_secret_key
CONTACT_TO_EMAIL=support@your-domain.com
CONTACT_FROM_EMAIL=LUXE PERFUME <contact@your-domain.com>
```

The contact API route validates submitted fields and sends the message through Resend. Do not enable email mode until the sender domain and recipient inbox are verified. Add production spam protection/rate limiting before public launch.

**Important scope note:** Adding real contact details and enabling the contact email service makes the Phase 5 contact workflow real. It does not turn the rest of this frontend demo into a fully operational ecommerce business: checkout/payment processing, authentication, order database, inventory, courier integration and newsletter storage require their own backend/provider integrations.

## Phase 5 — Home, About & Contact Finalization

The Phase 5 presentation content is implemented with editable demo content. Update these files before launch:

- `src/lib/site-config.ts`: business name, address, public phone, support email, WhatsApp number, business hours, social URLs, Google Maps embed URL and announcement bar.
- `src/lib/brand-content.ts`: Home campaign image, About image/video, story copy, mission/vision/values, craftsmanship/sustainability copy, timeline and approved testimonial slots.
- `src/components/store.tsx`: Home section composition and visual layout.

### Contact form modes

The default is demo mode. It only displays local success feedback and does not email or store submissions. To enable email delivery, configure both public and server mode values in `.env.local` and provide a verified Resend sender:

```env
NEXT_PUBLIC_CONTACT_FORM_MODE=email
CONTACT_FORM_MODE=email
RESEND_API_KEY=re_...
CONTACT_TO_EMAIL=support@your-domain.com
CONTACT_FROM_EMAIL=LUXE PERFUME <contact@your-verified-domain.com>
```

Never commit `.env.local` or expose `RESEND_API_KEY` to browser code. Resend requires a verified sending domain/address. The newsletter remains demo-only; it does not subscribe or store addresses.

### Phase 5 owner-side verification

Run on the target Windows computer after extracting the project:

```bash
npm run typecheck
npm run lint
npm run build
npm run dev
```

Check Home, About and Contact on desktop and mobile. Test required fields, invalid email, minimum lengths, demo feedback, optional public links, map embed, image/video replacement and the browser console. Do not publish placeholder business details or testimonial slots as genuine customer information.
