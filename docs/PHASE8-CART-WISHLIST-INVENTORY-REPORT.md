# Phase 8 — Cart, Wishlist & Inventory Report

**Status:** Started — implementation and verification not yet complete.  
**Branch:** `phase8/cart-wishlist-inventory`  
**Base:** `phase7/product-catalog-backend`  
**Draft PR:** pending initial setup checkpoint.

## Completed at start
- Created the Phase 8 working branch from the Phase 7 catalog backend branch.
- Audited V1 schema and confirmed cart, wishlist, inventory and inventory movement tables already exist.
- Confirmed existing granular permissions include `inventory.read` and `inventory.write`.
- Defined customer ownership, price recalculation, stock validation, inventory audit and test acceptance criteria.
- Confirmed V1/V2/V3 should remain immutable.

## Not yet implemented
- Customer cart endpoints and persistence behavior.
- Wishlist endpoints and persistence behavior.
- Current-price recalculation and cart stock validation.
- Inventory audit movements and stock adjustment endpoint.
- Low-stock detection API.
- Phase 8 integration/security tests and CI verification.

No claim of Phase 8 completion is made until the implementation and tests pass.
