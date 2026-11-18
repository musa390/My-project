# Phase 11 — Advanced Business Logic & OOP Report

Report status: IMPLEMENTATION + GITHUB CI PASS — owner-local Windows verification pending
Branch: `phase11/advanced-business-logic-oop`
Draft PR: https://github.com/musa390/My-project/pull/8
Base: `phase10/payment-integration`

## Executive summary

Phase 11 implementation has been added on a dedicated branch. This report does not declare the phase 100% complete until the backend build/tests, migration workflow and owner-local clean-database check pass.

Phase 10 remains IN PROGRESS independently. SSLCommerz is not part of Phase 11 and remains deferred until the final payment phase.

## Implemented

### OOP and business policies

- [ENC] `InventoryQuantity` is an immutable domain value that validates supported database ranges, ensures reserved quantity never exceeds on-hand quantity, calculates available stock, and validates reserve/release/adjustment operations.
- [INH] `AbstractCouponDiscountStrategy` shares discount rounding/cap invariants inherited by fixed and percentage strategies.
- [ABS] `CouponDiscountStrategy`, `ShippingPolicy`, `PaymentGateway`, `NotificationService`, order policy contracts and return transition contracts define behavior independently of concrete implementations.
- [POL] Fixed/percentage discounts, free-shipping threshold/flat-rate shipping, order/payment transition policies, notification persistence and return state transitions implement the contracts.
- [CRUD] Staff catalog CRUD/archive endpoints remain protected by the existing permission model.

### Product, inventory and order

- Product activation checks for at least one active variant.
- Inventory controller adjustments and catalog inventory updates use the immutable inventory value object.
- Checkout validates inventory through the same domain invariant while retaining database row locks and conditional reservation writes.
- Existing order snapshot, status-transition, cancellation and exactly-once stock release behavior is preserved.
- Staff cannot set an order directly to RETURNED; it must pass through the approved return-request workflow.

### Discount and shipping

- Existing fixed and percentage coupon implementations continue to use the common strategy contract.
- Checkout selects the first supported shipping policy from the ordered policy list.
- Free shipping is available when the configured subtotal threshold is reached; threshold 0.00 disables this strategy.
- Flat-rate shipping remains the fallback.

### Payment and refunds

- Existing PaymentGateway abstraction is retained without inventing provider endpoints.
- `RefundAmountPolicy` centralizes remaining refundable amount and over-refund validation.
- Refund requests remain MANUAL_REVIEW until staff records a separate reconciliation reference.
- No bKash/Nagad/SSLCommerz adapters, callbacks, or live transactions are claimed complete.

### Notifications

- V7 creates the user-scoped in-app notification table.
- Notifications are inserted transactionally and idempotently for order placement, cancellation and staff order-status changes.
- Authenticated users can list their own notifications and mark their own notifications as read.
- No external email/SMS/push delivery provider is configured.

### Returns

- V8 adds full-order return request and event-audit tables.
- Only DELIVERED orders are eligible.
- Customer ownership, one-request-per-order and idempotency-key checks are enforced.
- Staff may approve/reject a request; only approved requests may be marked received.
- Receipt moves the order from DELIVERED to RETURNED and writes both return and order history.
- Return receipt does not automatically restock inventory or mark payment refunded. Refund remains a separate manual-review workflow.

## Database changes

- V7: `user_notifications`
- V8: `return_requests`, `return_request_events`
- Existing V1–V6 migration files are unchanged.

## Tests added or extended

- Inventory quantity boundary and reserve/release tests.
- Product publication policy tests.
- Shipping strategy selection tests.
- Refund amount policy tests.
- Notification integration tests for idempotency, user isolation and read behavior.
- Return integration tests for eligibility, idempotency, review, receipt, audit and ownership isolation.
- Existing customer order integration tests extended to verify notification inbox ownership and cancellation notification creation.
- Existing checkout pricing and payment transition tests remain part of the full Maven suite.

Exact test totals and final acceptance status will be recorded only after GitHub Actions completes on the implementation commit.

## Verification results

- Backend CI PASS: https://github.com/musa390/My-project/actions/runs/44231050193
- Database Migration CI PASS: https://github.com/musa390/My-project/actions/runs/44231049804
- Verified commit: `605b859f705134924b9b545c934b48d79af34965`
- Java 21 + MySQL 8.4; Maven `clean verify`: **72 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
- Database Migration CI applied V1–V8 on a clean MySQL 8.4 service and verified payment, notification and return tables.
- No Customer Website or Admin Website source files were changed.

## Remaining final gate

1. Owner-local Windows clean-database verification on the Phase 11 branch.
2. Owner review of the new return policy and API behavior.
3. Keep PR #8 draft/unmerged until owner approval; do not merge without explicit approval.

PR #8 remains draft and unmerged. No merge is performed without explicit approval.
