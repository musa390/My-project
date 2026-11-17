# Phase 8 — Cart, Wishlist & Inventory Progress Report

**Status:** Initial backend implementation complete; CI verification passed. Phase 8 remains in progress pending further review and final acceptance.  
**Branch:** `phase8/cart-wishlist-inventory`  
**Base:** `phase7/product-catalog-backend`  
**Draft PR #5:** https://github.com/musa390/My-project/pull/5

## Implemented

### Persistent Customer cart
- `GET /api/v1/cart`
- `POST /api/v1/cart/items`
- `PUT /api/v1/cart/items/{itemId}`
- `DELETE /api/v1/cart/items/{itemId}`
- Cart is stored in `carts` and `cart_items`, keyed by authenticated user.
- Repeated add increments the existing variant line.
- Update and remove are restricted to the authenticated user's active cart.
- A cart survives logout and a fresh login.
- Prices and totals are recalculated from the current active variant price; client-supplied price is not trusted.
- Cart reads show current availability and flag unavailable lines.

### Persistent Customer wishlist
- `GET /api/v1/wishlist`
- `POST /api/v1/wishlist/items/{productId}`
- `DELETE /api/v1/wishlist/items/{productId}`
- Stored in `wishlists` and `wishlist_items`.
- Each customer has an independent default wishlist.
- Adding an existing product is idempotent.
- Only active products can be added.

### Inventory and stock controls
- `GET /api/v1/staff/inventory/low-stock`
- `GET /api/v1/staff/inventory/variants/{variantId}`
- `GET /api/v1/staff/inventory/variants/{variantId}/movements`
- `POST /api/v1/staff/inventory/variants/{variantId}/adjustments`
- Low-stock detection compares available quantity (on hand minus reserved) to reorder level.
- Manual stock adjustment locks the variant/inventory rows, rejects stock below reserved quantity, and writes an `ADJUSTMENT` movement in the same transaction.
- Existing catalog inventory update now records stock deltas; new variant initial stock records a `RECEIPT` movement.
- GET inventory routes require `PERM_inventory.read`; mutations require `PERM_inventory.write`.

### Stock and cart rules
- Add/update validates active product, active variant and available stock.
- Inventory row locking is used during cart writes and stock adjustments.
- Cart does not reserve inventory. Checkout/order creation will reserve stock atomically in Phase 9.
- Client cannot supply a trusted price; server always recalculates from database.

## Automated verification

**PASS — GitHub Actions run:** https://github.com/musa390/My-project/actions/runs/44134986481

- Commit: `0cb9088dfe2caf46cd744fc179741eb12c73af1f`
- Java 21 / Spring Boot / Maven / MySQL 8.4.
- Flyway successfully applied V1, V2 and V3 on a clean CI database.
- **26 integration tests, 0 failures, 0 errors, 0 skipped.**
- **Maven BUILD SUCCESS.**

Coverage includes cart persistence across logout/login, customer isolation, cart add/update/remove, current-price recalculation, insufficient-stock rejection, wishlist idempotency and isolation, staff inventory permissions, low-stock detection, audited adjustments and rejection below reserved quantity.

## Database and protected scope
- Existing schema was sufficient; no V4 migration was needed.
- V1/V2/V3 remain unchanged.
- No Customer Frontend source files changed.
- No Admin Frontend source files changed.
- PR #5 is open, draft and unmerged.

## Remaining before Phase 8 can be marked 100%
1. Final code/security/concurrency review and any additional edge-case tests.
2. Owner-local Windows `mvn clean verify` against a separately-created clean database. Preserve the previously failed local database; do not repair/drop it blindly.
3. Final verification on the latest branch head and update of this report.
