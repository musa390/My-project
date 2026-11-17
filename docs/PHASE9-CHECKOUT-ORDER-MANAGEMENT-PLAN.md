# Phase 9 — Checkout & Order Management Plan

**Status:** Steps 1–3 implemented; Step 3 GitHub Maven CI PASS. Customer history/detail/cancellation and Staff/Admin order management remain pending.
**Branch:** `phase9/checkout-order-management`
**Base:** `phase8/cart-wishlist-inventory`
**Dependency:** Phase 8 persistent cart, catalog pricing, inventory and authentication APIs.
**Database:** Reuse the existing V1 schema initially. Do not edit V1/V2/V3; introduce V4 only if implementation proves a schema change is necessary.

## Scope and acceptance criteria

A signed-in Customer can create an order from their saved cart and owned delivery address. The backend, not the browser, calculates every amount, validates coupons and stock, creates immutable order/item/address snapshots, and reserves inventory atomically. Customers can view order history, track status and cancel only when policy permits. Authorized Staff/Admin can list, inspect and transition orders. All changes are persisted in MySQL and covered by MySQL 8.4 integration tests.

## Planned API

### Customer
- `POST /api/v1/checkout/preview` — validate address/cart/coupon and return a server-calculated quote without creating an order or reserving stock.
- `POST /api/v1/orders` — create order from the authenticated customer's active cart and owned address.
- `GET /api/v1/orders` — paginated order history for the authenticated customer only.
- `GET /api/v1/orders/{orderNumber}` — customer-owned order detail and status history.
- `POST /api/v1/orders/{orderNumber}/cancel` — request cancellation only when allowed by the transition policy.

### Staff/Admin
- `GET /api/v1/staff/orders` — permission-protected, paginated order list with status/date/search filters.
- `GET /api/v1/staff/orders/{orderNumber}` — order detail and status history.
- `PATCH /api/v1/staff/orders/{orderNumber}/status` — validated status transition with actor and audit history.

## Backend rules

1. Derive customer identity only from the authenticated principal; never accept a user/customer ID from the request.
2. Accept an owned saved-address ID; verify ownership and copy recipient, phone and complete address fields into the order snapshot in the same transaction.
3. Read cart lines, active products/variants and current prices from MySQL. Ignore any client-supplied unit price, subtotal, discount, delivery charge, tax or grand total.
4. Use `BigDecimal` and database `DECIMAL` values for all money. Round consistently to two decimal places.
5. Calculate subtotal from current variant price × quantity. Validate each line's product/variant status and available stock.
6. Validate coupon active dates, minimum amount, percentage/fixed type, maximum discount, global usage limit, per-customer usage limit and optional product/category restrictions. Lock the coupon row when redeeming; increment usage and insert one redemption record atomically with order creation.
7. Calculate delivery charge using a documented configurable server-side policy. Do not invent or accept a browser-provided charge. Tax remains a server-side policy value (zero only if explicitly configured as zero).
8. Calculate `grand_total = subtotal - discount_total + shipping_total + tax_total`; enforce non-negative values and the database constraints.
9. Generate a unique, non-guessable order number server-side; handle collisions safely.
10. Create `customer_orders`, `order_items`, initial `order_status_history`, optional `coupon_redemptions`, inventory reservation movements, and cart conversion in one transaction.
11. Lock inventory rows in a deterministic variant-ID order. Recheck `quantity_on_hand - quantity_reserved` while locked; reserve the requested quantities atomically. Any failure rolls back the entire order.
12. Snapshot product name, variant name, SKU, unit price, quantity, line discount, recipient, phone, email and shipping address. Later catalog/address changes must not rewrite past orders.
13. Define and enforce allowed order transitions. Customer cancellation is permitted only before dispatch (PENDING/CONFIRMED/PROCESSING); cancellation releases reserved stock and writes RELEASE movements exactly once. SHIPPED/DELIVERED/terminal orders cannot be customer-cancelled through this endpoint.
14. Staff status transitions must be authorized, validated, audited with actor and note, and must not silently alter payment status. Payment collection/refunds belong to Phase 10.
15. Scope every customer read/cancel query by the authenticated customer. Staff access requires explicit order permissions.

## Java 21 OOP requirement

Phase 9 must implement Encapsulation, Inheritance, Abstraction and Polymorphism in the checkout/order business logic. The concrete class/interface design, package structure, transaction boundary and OOP test expectations are documented in `PHASE9-JAVA-OOP-ARCHITECTURE.md`. This requirement applies to Phase 9 itself; it is not deferred to Phase 11.

## Implementation sequence

- [x] Inspect existing address, cart, coupon, order, order-item, coupon-redemption and inventory schema plus auth principal conventions.
- [x] Add DTO validation and server-side checkout quote/pricing/shipping/coupon services; Step 2 Maven CI PASS.
- [x] Implement transactional order creation, stock reservation, coupon redemption, order snapshots and cart conversion; Step 3 Maven CI PASS.
- [ ] Implement customer history, detail, tracking and cancellation with stock release.
- [ ] Implement Staff/Admin order list/detail/status transitions and permission checks.
- [x] Add Step 2/3 unit and MySQL integration tests for checkout validation, order snapshots, coupon redemption, stock reservation, stock rejection, duplicate submissions and cart conversion. Customer cancellation and Staff/Admin status tests remain for later steps.
- [x] Run MySQL 8.4 GitHub Actions `mvn clean verify` for Steps 2–3 — PASS, 38 tests, 0 failures, 0 errors, 0 skipped; Maven BUILD SUCCESS (workflow run 44145457771).
- [ ] Update API/OpenAPI documentation for the remaining customer and staff order-management endpoints; Step 3 plan/report/project status updated.
- [ ] Review diff to ensure Customer/Admin frontend source files and V1/V2/V3 are unchanged.
- [ ] Owner-local clean-database verification remains an explicit environment acceptance step.

## Out of scope
- Real payment gateway integration or marking an order PAID from a browser callback (Phase 10).
- Customer/Admin frontend source changes and frontend-backend integration (Phase 13; requires explicit approval).
- Production deployment and live customer data.

## Protected project rules
- Keep Customer Website and Admin Website source files unchanged.
- Never commit secrets or request secrets in chat.
- Do not merge this draft PR without owner approval.
- Preserve the previously failed local database; do not run `flyway repair`, `DROP DATABASE`, or delete Docker volumes as a shortcut.


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


## Step 2 — Checkout Validation & Pricing Engine (implementation checkpoint)

**Runtime API:** `POST /api/v1/checkout/preview`

**Request:** `{ "addressId": 123, "couponCode": "OPTIONAL-CODE" }`. Unknown client fields such as price, subtotal, discount, shipping, tax and grand total are ignored and never used.

**Validation and calculation:**
- Requires an authenticated customer; customer ID is taken from the verified security principal.
- Confirms the selected address belongs to that customer.
- Loads the customer's active cart, product/variant status, current database price and available stock.
- Rejects empty carts, inactive/deleted products, inactive variants and insufficient available stock.
- Validates coupon active state, start/end dates, global usage limit, per-customer redemption limit, minimum cart subtotal, and product/category restrictions.
- Supports percentage and fixed coupons; maximum discount and eligible subtotal caps are enforced.
- Product/category restriction scopes are combined as eligible-product OR eligible-category matches; only eligible cart lines receive allocated discounts.
- Uses `BigDecimal` for money and a two-decimal rounding policy.
- Shipping and tax are calculated by server-side policy interfaces. Development defaults are explicitly configured as BDT 0.00 shipping and 0.00% tax; production/business values must be set through `CHECKOUT_SHIPPING_FLAT_RATE` and `CHECKOUT_TAX_PERCENTAGE`.
- Preview is read-only: it does not reserve inventory, consume coupon usage, create an order or change the cart. Order creation revalidates all values transactionally in Step 3.

**Java OOP used in runtime code:**
- Encapsulation: `CheckoutLine`, `MoneyBreakdown`, `Coupon`.
- Inheritance: `AbstractCouponDiscountStrategy`.
- Abstraction: `CouponDiscountStrategy`, `ShippingPolicy`, `TaxPolicy`.
- Polymorphism: percentage/fixed coupon strategy implementations and injected policy interfaces.

**Tests added:** strategy/domain unit tests and MySQL-backed MockMvc integration tests for percentage/fixed coupons, max discount, product restrictions, minimum spend, usage limits, address ownership, stock validation, authentication and ignored client totals. CI result is pending; do not mark tests PASS until the latest branch-head workflow succeeds.


### Step 2 verification result

<success>PASS</success> GitHub Actions workflow: https://github.com/musa390/My-project/actions/runs/44143372079

- Java 21 and MySQL 8.4.
- Command: `mvn --batch-mode --no-transfer-progress clean verify`.
- Backend test suite: **35 tests, 0 failures, 0 errors, 0 skipped**.
- Checkout-specific tests: **6 integration tests**.
- OOP pricing tests: **3 unit tests**.
- Flyway V1/V2/V3 applied successfully in the ephemeral CI database.
- Maven result: **BUILD SUCCESS**.

This verifies the Step 2 implementation in GitHub CI. Owner-local verification against a separately created clean database remains pending; the previously failed local database must be preserved.


## Step 3 — Transactional Order Creation & Inventory Reservation

**Runtime API:** `POST /api/v1/orders`

**Request:** `{ "addressId": 123, "couponCode": "OPTIONAL-CODE", "customerNote": "Optional delivery note" }`. Client-supplied prices and totals are ignored.

**Transaction flow:**
1. Derive customer ID from the authenticated numeric principal; endpoint requires CUSTOMER role. Cart add/update/remove operations acquire the same ACTIVE-cart row lock before changing lines, so they serialize with checkout.
2. Lock the customer's ACTIVE cart row with `SELECT ... FOR UPDATE`. A second submission cannot reuse a converted cart.
3. Lock and verify the address belongs to the customer; read the active customer's email.
4. Lock cart items, then lock product/variant and inventory rows in ascending variant ID order. Re-read product state, current DB price, on-hand and reserved quantities.
5. Lock coupon row with `FOR UPDATE`; revalidate active dates, global usage limit, per-user limit, minimum spend and product/category restrictions.
6. Recalculate the quote with the existing BigDecimal pricing/shipping/tax policies.
7. Generate a server-side `LP-YYYYMMDD-12HEX` order number; the database unique key remains the final collision guard.
8. Insert `customer_orders` with customer/address/email/total snapshots and `order_items` with product/variant/SKU/price/quantity/discount/line-total snapshots.
9. Increase `inventory.quantity_reserved` under a guarded update and insert positive-quantity `RESERVATION` movements referencing the order.
10. Increment coupon usage and insert `coupon_redemptions`; insert initial PENDING status history; convert the cart.
11. All writes run inside one Spring `@Transactional` boundary. Any exception rolls back the order, item snapshots, stock reservations, movements, coupon redemption/usage, status history and cart conversion together.

**Java OOP:** `OrderNumberGenerator` / `UuidOrderNumberGenerator` abstract order number generation; `InventoryReservationService` / `JdbcInventoryReservationService` abstract persistence-backed reservation. Existing domain/pricing classes continue to apply Encapsulation, Inheritance, Abstraction and Polymorphism.

**Tests:** 3 MySQL-backed order placement integration tests cover successful snapshots/reservation/redemption/cart conversion, insufficient stock rejection without order/cart mutation, and duplicate submission prevention.

### Step 3 verification result

<success>PASS</success> GitHub Actions workflow: https://github.com/musa390/My-project/actions/runs/44145457771

- Branch head: `76b8876fe313ce0101c45ea9651a5ad05f7a0e67`.
- Java 21 + MySQL 8.4.
- Command: `mvn --batch-mode --no-transfer-progress clean verify`.
- **38 tests, 0 failures, 0 errors, 0 skipped**; Maven **BUILD SUCCESS**.
- Order placement integration: 3 tests; checkout preview integration: 6; OOP pricing unit tests: 3.
- Flyway validated and applied V1/V2/V3 in the ephemeral CI database.
- Owner-local verification against a separately created clean database remains pending; preserve the earlier failed local database.
