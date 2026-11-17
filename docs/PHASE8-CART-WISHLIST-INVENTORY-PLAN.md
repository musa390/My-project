# Phase 8 — Cart, Wishlist & Inventory Plan

**Branch:** `phase8/cart-wishlist-inventory`  
**Base:** `phase7/product-catalog-backend`  
**Status:** Started — planning and schema audit complete; implementation pending.

## Goal
Deliver database-persistent Customer carts and wishlists, validate current catalog prices and available stock on every cart operation, and provide audited inventory adjustment and low-stock visibility for authorized staff.

## Existing schema audit
The existing V1 schema already contains:
- `carts` and `cart_items` with one active cart per user and one row per variant.
- `wishlists` and `wishlist_items` with per-user named wishlist uniqueness.
- `inventory` with on-hand, reserved and reorder quantities.
- `inventory_movements` with constrained movement types and non-zero quantity changes.
- `product_variants` with current price, active state and product relation.
- Existing role permissions include `inventory.read` and `inventory.write`.

Do not edit V1, V2 or V3. No V4 is planned unless implementation proves a real schema gap.

## Customer API
- `GET /api/v1/cart` — get the authenticated customer's persistent cart.
- `POST /api/v1/cart/items` — add a variant and quantity; repeated adds increment the existing line.
- `PUT /api/v1/cart/items/{itemId}` — set quantity.
- `DELETE /api/v1/cart/items/{itemId}` — remove an owned cart line.
- `GET /api/v1/wishlist` — get the authenticated customer's default wishlist.
- `POST /api/v1/wishlist/items/{productId}` — add an active product (idempotent).
- `DELETE /api/v1/wishlist/items/{productId}` — remove an item.

All Customer routes require a valid authenticated session. Ownership is resolved from the JWT-authenticated user ID; clients cannot submit or override user IDs.

## Cart and price rules
- Persist only variant ID and quantity; never trust a client-supplied price.
- Recalculate unit price, line total and cart subtotal from the current active variant price on every cart read/write.
- Return current price and availability status so a price/stock change since the previous visit is visible.
- Validate product ACTIVE, variant active and positive bounded quantity.
- Lock the inventory row while validating add/update. Available stock = quantity_on_hand - quantity_reserved.
- For a customer's existing cart line, validate the resulting total quantity, not only the incremental quantity.
- Do not reserve inventory merely because an item is in a cart. Order/checkout reservation belongs to Phase 9 and must be atomic there.

## Inventory and staff API
- Extend the existing staff inventory update to write an `ADJUSTMENT` movement when on-hand quantity changes.
- Add permission-protected inventory detail and low-stock endpoints.
- Add an explicit stock adjustment endpoint that locks inventory, rejects changes below reserved quantity, updates on-hand quantity and writes an audit movement in the same transaction.
- Low stock = available quantity (on hand minus reserved) less than or equal to reorder level.
- GET inventory endpoints require `PERM_inventory.read`; stock mutations require `PERM_inventory.write`.
- No inventory reservation or sale movement is performed in Phase 8; those are order lifecycle operations.

## Security
- Customer A must never read, modify or delete Customer B's cart or wishlist.
- Customer routes derive user ID exclusively from authenticated principal.
- Staff inventory reads and writes use existing granular permissions.
- All state-changing cookie-authenticated requests require CSRF token.
- Use parameterized SQL and transactional locking for inventory changes.

## Integration tests
- Persistent cart survives repeated GETs and is isolated per customer.
- Add increments existing variant line; quantity update and remove work.
- Inactive/draft product or inactive variant cannot be added.
- Quantity must be positive and not exceed available stock.
- Cart prices and totals come from current DB prices, not request payload.
- Wishlist add/remove persists and is isolated per customer.
- Stock adjustments update inventory and create movement history atomically.
- Adjustment below reserved stock is rejected.
- Low-stock endpoint applies reorder threshold and permission boundaries.
- Anonymous cart/wishlist and unauthorized staff requests are denied.

## Completion criteria
1. All listed APIs implemented and documented.
2. Database persistence and user ownership verified by MySQL integration tests.
3. Stock checks, inventory adjustments and movement audit verified transactionally.
4. Maven `clean verify` and GitHub Actions pass against clean MySQL 8.4.
5. Existing migrations remain immutable unless a documented V4 gap is found.
6. Customer/Admin Frontend source files remain unchanged.
7. Final report and project status updated; PR reviewed and left unmerged pending owner approval.


## Implementation checkpoint (2026-11-17)
- Added authenticated persistent cart and wishlist endpoints in `CartWishlistController`.
- Cart lines persist by user and variant. Add increments an existing line; quantity update and removal are scoped to the authenticated user's active cart.
- Cart response recalculates unit prices and line totals from current database variant prices. Client-supplied price is ignored.
- Cart writes lock the active variant and inventory row, then validate on-hand minus reserved stock. Cart entries do not reserve stock.
- Wishlist add is idempotent and only accepts active products.
- Added staff inventory detail, low-stock and movement-history reads plus transactional manual stock adjustments with `ADJUSTMENT` ledger entries.
- Existing catalog inventory updates and initial variant receipts now write inventory movement records.
- GET inventory routes require `PERM_inventory.read`; stock mutations require `PERM_inventory.write`.
- Added integration/security coverage including cart persistence across logout/login, user isolation, current-price recalculation, stock validation, wishlist persistence, low-stock and inventory audit.
- GitHub Actions run `44134986481` succeeded on commit `0cb9088dfe2caf46cd744fc179741eb12c73af1f`: MySQL 8.4, V1/V2/V3 applied, 26 tests, 0 failures/errors, Maven BUILD SUCCESS.
- No V4 migration was needed; V1/V2/V3 remain unchanged.
- No Customer Frontend or Admin Frontend source files changed.

## Remaining work
- Review concurrency and edge cases, add further tests where needed, and verify the final branch head after all follow-up fixes.
- Owner-local Windows verification against a separate clean database remains pending.
- PR #5 remains draft and unmerged; no branch merge has been performed.
