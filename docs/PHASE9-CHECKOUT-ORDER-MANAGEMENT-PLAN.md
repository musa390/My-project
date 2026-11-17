# Phase 9 — Checkout & Order Management Plan

**Status:** Started — planning and branch foundation created; backend implementation pending.
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

## Implementation sequence

- [ ] Inspect existing address, cart, coupon, order, order-item, coupon-redemption and inventory schema plus auth principal conventions.
- [ ] Add DTO validation and server-side checkout quote/pricing/shipping/coupon services.
- [ ] Implement transactional order creation, stock reservation and cart conversion.
- [ ] Implement customer history, detail, tracking and cancellation with stock release.
- [ ] Implement Staff/Admin order list/detail/status transitions and permission checks.
- [ ] Add integration tests for ownership, price tampering, coupon limits/restrictions, stock races/rollback, totals, snapshots, status transitions and cancellation idempotency.
- [ ] Run MySQL 8.4 GitHub Actions `mvn clean verify`; fix all failures.
- [ ] Update API/OpenAPI documentation, Phase 9 report and project status.
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
