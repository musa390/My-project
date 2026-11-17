# Phase 9 — Checkout & Order Management Progress Report

**Status:** Started — GitHub branch and implementation plan established. No checkout/order runtime implementation is claimed yet.
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
