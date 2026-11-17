# Phase 9 — Checkout & Order Management Progress Report

**Status:** Steps 1–5 runtime implementation committed; latest branch-head GitHub Maven CI PASS. Final regression/security review and owner-local clean-database verification remain pending.
**Branch:** `phase9/checkout-order-management`
**Base:** `phase8/cart-wishlist-inventory`

## Foundation
- Created a dedicated Phase 9 branch from the Phase 8 implementation branch.
- Existing V1 schema already contains addresses, coupons, coupon product/category restrictions, coupon redemptions, customer_orders, order_items, order_status_history, carts/cart_items, inventory and inventory_movements.
- Initial plan reuses the existing schema; V1/V2/V3 remain immutable.
- Backend CI workflow will be extended to run for Phase 9 branch pushes and pull requests.

## Next implementation work
Complete final regression/security review and owner-local clean-database verification. Staff/Admin order list/detail/status transitions and order read/write permission separation are implemented. See `PHASE9-CHECKOUT-ORDER-MANAGEMENT-PLAN.md` for transaction rules, security boundaries and acceptance tests.

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

The project owner confirmed that Java OOP is the primary backend programming approach and all four pillars are mandatory in Phase 9. A dedicated architecture document now maps Encapsulation, Inheritance, Abstraction and Polymorphism to concrete checkout/order domain classes, strategy interfaces, services and tests. This architecture is now used by the runtime pricing and order-placement classes; Step 3 is verified by MySQL 8.4 GitHub Actions.


## Step 2 — Checkout Validation & Pricing Engine

- Added the authenticated `POST /api/v1/checkout/preview` endpoint.
- Added Java 21 domain/pricing classes using Encapsulation, Inheritance, Abstraction and Polymorphism.
- Server loads customer-owned address, active cart, current database prices, product/variant availability and inventory stock.
- Added percentage/fixed coupon strategies with date, usage, minimum-spend, maximum-discount and product/category-scope validation.
- Added configurable server-side flat shipping and percentage tax policies. Development defaults are explicitly 0.00 / 0.00%; production values must be configured.
- Checkout preview does not create orders, reserve stock, consume coupon usage or mutate carts.
- Added OOP unit tests and MySQL-backed API integration tests. GitHub Actions run 44143372079 passed at the Step 2 checkpoint; Step 3 is covered by the latest 38-test run below.


### Step 2 — Maven verification: PASS

- GitHub Actions: https://github.com/musa390/My-project/actions/runs/44143372079
- Java 21 + MySQL 8.4.
- `mvn --batch-mode --no-transfer-progress clean verify`: **BUILD SUCCESS**.
- **35 tests, 0 failures, 0 errors, 0 skipped**.
- Checkout integration: 6 tests; OOP pricing unit tests: 3 tests.
- Flyway V1/V2/V3 applied successfully in CI.
- The run verifies the Step 2 branch checkpoint. Owner-local clean-database verification remains pending; preserve the earlier failed local database.


## Step 3 — Transactional Order Creation & Inventory Reservation

Implemented authenticated Customer endpoint `POST /api/v1/orders`.

### Runtime implementation
- `OrderApplicationService` coordinates the entire order workflow in one Spring `@Transactional` method.
- Locks the customer's ACTIVE cart, owned delivery address, cart lines, sellable variants and inventory rows. Cart add/update/remove endpoints use the same parent-cart lock, preventing concurrent cart edits from racing with checkout.
- Locks variants/inventory in ascending variant ID order; recalculates current prices and stock from MySQL rather than trusting checkout preview or browser totals.
- Locks and revalidates coupon usage, dates, minimum spend and product/category scope before redemption.
- Generates server-side order numbers using `OrderNumberGenerator` / `UuidOrderNumberGenerator`; the unique database key enforces uniqueness.
- Writes customer order totals and shipping snapshots, item name/variant/SKU/price/quantity/discount snapshots and initial PENDING status history.
- Reserves stock by increasing `quantity_reserved` with a guarded update and writes positive-quantity `RESERVATION` movement records referencing the order.
- Increments coupon usage and creates the coupon redemption record.
- Converts the active cart only after all other order writes succeed. A repeated submission cannot create a second order from the converted cart.
- Any failure rolls back all writes in the transaction. No payment is marked PAID; payment gateway work remains Phase 10.

### Step 3 — Maven verification: PASS

- GitHub Actions: https://github.com/musa390/My-project/actions/runs/44145457771
- Branch head: `76b8876fe313ce0101c45ea9651a5ad05f7a0e67`.
- Java 21 + MySQL 8.4; `mvn --batch-mode --no-transfer-progress clean verify`.
- **38 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS.**
- Order placement integration: 3 tests; checkout preview integration: 6; OOP pricing unit tests: 3.
- Flyway validated/applied V1/V2/V3 in the ephemeral CI database.
- Owner-local clean-database verification remains pending; preserve the earlier failed local database.

## Step 4 — Customer Order Management

Implemented customer endpoints:
- `GET /api/v1/orders` — paginated order history for the authenticated customer.
- `GET /api/v1/orders/{orderNumber}` — order and item snapshots plus status history.
- `GET /api/v1/orders/{orderNumber}/tracking` — status timeline.
- `POST /api/v1/orders/{orderNumber}/cancel` — policy-checked customer cancellation.

All customer queries are scoped by the authenticated principal's user ID; an order belonging to another customer is returned as NOT_FOUND. Cancellation is permitted only for pre-dispatch PENDING/CONFIRMED/PROCESSING orders with UNPAID/FAILED payment status. It locks the order and inventory rows, releases reserved stock with negative RELEASE ledger movements, changes the order to CANCELLED and writes the status-history event in one transaction. Duplicate cancellation cannot release stock a second time. Payment/refund state is not changed.

Added the `OrderCancellationPolicy` abstraction and `PreDispatchOrderCancellationPolicy` implementation. Added MySQL integration tests for customer ownership boundaries, history/detail/tracking, successful cancellation, exactly-once stock release and rejection of shipped orders.

**Step 4 verification: PASS.** Step 4 is included in GitHub Actions run 44148435956 with Java 21 + MySQL 8.4: 45 total tests, 0 failures/errors/skips, Maven BUILD SUCCESS. The run also verifies Step 5.


## Step 5 — Staff/Admin Order Management

Implemented Staff/Admin APIs under `/api/v1/staff/orders`:
- `GET /` — paginated list with order status, payment status and free-text filters across order number, customer email, recipient and phone.
- `GET /{orderNumber}` — order snapshots, line items, payment records and status history.
- `PATCH /{orderNumber}/status` — status transition with an optional staff note.

The status lifecycle is explicitly constrained: PENDING → CONFIRMED/CANCELLED; CONFIRMED → PROCESSING/CANCELLED; PROCESSING → SHIPPED/CANCELLED; SHIPPED → DELIVERED. Backward/terminal transitions and unsupported RETURNED transitions are rejected.

SecurityConfig now separates GET order routes (`PERM_orders.read`) from mutation routes (`PERM_orders.write`). The controller additionally requires ADMIN or MANAGER role and the matching permission; status mutation requires write permission. The actor ID comes from the authenticated principal.

Staff cancellation is limited to pre-dispatch unpaid/failed orders. It locks the order and inventory rows, releases each reservation with a negative RELEASE ledger movement, updates the order status and writes the audit event in one transaction. Duplicate cancellation cannot release stock twice. Paid/pending-payment cancellations are rejected pending payment/refund handling.

Added `OrderStatusTransitionPolicy` and `DefaultOrderStatusTransitionPolicy` for OOP abstraction/polymorphism. Added three MySQL integration tests for staff search/detail and full valid status progression with audit actor, exactly-once cancellation release, and read-only versus write permission separation.

**Step 5 verification: PASS.** GitHub Actions run 44148435956 — Java 21 + MySQL 8.4, `mvn --batch-mode --no-transfer-progress clean verify`, **45 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. This run includes the four Customer Order Management tests and three Staff Order Management tests. Flyway V1/V2/V3 were applied in the ephemeral CI database.

## Remaining Phase 9 scope
Customer and Staff/Admin order history/detail/tracking/status management and permission separation are implemented and CI-verified. Remaining Phase 9 scope: final regression/security review and owner-local clean-database verification.
