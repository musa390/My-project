# Phase 9 — Checkout & Order Management Progress Report

**Status:** Step 1 audit and Step 2 Checkout Validation/Pricing Engine implementation complete; Step 2 GitHub Maven CI PASS. Order creation and management remain pending.
**Branch:** `phase9/checkout-order-management`
**Base:** `phase8/cart-wishlist-inventory`

## Foundation
- Created a dedicated Phase 9 branch from the Phase 8 implementation branch.
- Existing V1 schema already contains addresses, coupons, coupon product/category restrictions, coupon redemptions, customer_orders, order_items, order_status_history, carts/cart_items, inventory and inventory_movements.
- Initial plan reuses the existing schema; V1/V2/V3 remain immutable.
- Backend CI workflow will be extended to run for Phase 9 branch pushes and pull requests.

## Next implementation work
See `PHASE9-CHECKOUT-ORDER-MANAGEMENT-PLAN.md` for the API contract, transaction rules, security boundaries, cancellation policy and test checklist.

## Acceptance
Phase 9 is not complete until Customer order creation/history/tracking/cancellation and Staff/Admin order management are implemented, backend-calculated totals and atomic stock reservation are verified by MySQL 8.4 integration tests, CI passes, and protected frontend/schema constraints are respected.


## Step 1 — Existing Backend & Database Audit

**Audit result:** Completed against the Phase 9 branch at commit `a5630ae377ce4a9ebfaa49eedf3596ea4107b14c`. No runtime source or migration was changed during this audit.

### Confirmed existing schema
- `addresses`: customer-owned address rows with recipient, phone, address lines, area, city, district, postal code and country code; a generated key enforces one default address per user.
- `carts` / `cart_items`: one active cart per user and unique variant line; cart status supports `ACTIVE`, `CONVERTED`, and `ABANDONED`.
- `coupons`, `coupon_products`, `coupon_categories`, `coupon_redemptions`: percentage/fixed coupons, date window, minimum spend, max discount, global/per-user limits, optional product/category scope, usage counter and one redemption per order.
- `customer_orders`, `order_items`, `order_status_history`: order totals and shipping snapshots, item/product/variant/SKU/price/quantity snapshots, status audit actor/note/time.
- `inventory` / `inventory_movements`: on-hand/reserved/reorder quantities and movement types including `RESERVATION` and `RELEASE`.
- Monetary columns use `DECIMAL(12,2)`; order CHECK constraints enforce nonnegative amounts and `grand_total = subtotal - discount_total + shipping_total + tax_total`.

### Confirmed backend conventions
- Existing endpoints use Spring MVC + `JdbcTemplate`, not a domain JPA entity/repository model for these modules.
- Authenticated principal is a numeric user ID (`Authentication.getPrincipal()`); derive customer identity from it, never from request JSON.
- Cart reads current variant prices from MySQL and cart writes lock sellable variants and inventory rows. Cart does not reserve stock, so Phase 9 must reserve only inside order creation.
- Existing inventory adjustments use transactional row locks and write movement-ledger records.
- Security uses cookie JWT + DB session validation, CSRF protection, and `@PreAuthorize` permission checks.
- RBAC seed includes `orders.read` and `orders.write`; ADMIN receives all permissions, MANAGER receives both, CUSTOMER receives `orders.read`.

### Issues to address during implementation
1. `SecurityConfig` currently applies `PERM_orders.write` to every `/api/v1/staff/orders/**` route. Split read-only GET routes to `PERM_orders.read` and keep status mutation on `PERM_orders.write`; add tests for both boundaries.
2. Shipping and tax policy must be explicit server-side configuration. Do not guess a delivery fee or accept client-supplied shipping/tax values. Inspect current configuration before choosing defaults.
3. Reservation movement sign convention and cancellation idempotency must be consistently defined and tested with inventory row locks.
4. Coupon preview may be advisory; order creation must lock and revalidate coupon limits/restrictions inside the same transaction as redemption.

### Schema decision
The existing V1 schema already supports the planned Phase 9 workflow. No V4 migration is justified by the current audit. V1/V2/V3 remain immutable.


## Java 21 OOP requirement

The project owner confirmed that Java OOP is the primary backend programming approach and all four pillars are mandatory in Phase 9. A dedicated architecture document now maps Encapsulation, Inheritance, Abstraction and Polymorphism to concrete checkout/order domain classes, strategy interfaces, services and tests. This is architecture documentation only; runtime Java classes and APIs have not yet been implemented or tested.


## Step 2 — Checkout Validation & Pricing Engine

- Added the authenticated `POST /api/v1/checkout/preview` endpoint.
- Added Java 21 domain/pricing classes using Encapsulation, Inheritance, Abstraction and Polymorphism.
- Server loads customer-owned address, active cart, current database prices, product/variant availability and inventory stock.
- Added percentage/fixed coupon strategies with date, usage, minimum-spend, maximum-discount and product/category-scope validation.
- Added configurable server-side flat shipping and percentage tax policies. Development defaults are explicitly 0.00 / 0.00%; production values must be configured.
- Checkout preview does not create orders, reserve stock, consume coupon usage or mutate carts.
- Added OOP unit tests and MySQL-backed API integration tests. **GitHub Actions verification is pending; no Maven PASS claim is made yet.**


### Step 2 — Maven verification: PASS

- GitHub Actions: https://github.com/musa390/My-project/actions/runs/44143372079
- Java 21 + MySQL 8.4.
- `mvn --batch-mode --no-transfer-progress clean verify`: **BUILD SUCCESS**.
- **35 tests, 0 failures, 0 errors, 0 skipped**.
- Checkout integration: 6 tests; OOP pricing unit tests: 3 tests.
- Flyway V1/V2/V3 applied successfully in CI.
- The run verifies the Step 2 branch checkpoint. Owner-local clean-database verification remains pending; preserve the earlier failed local database.
