# Phase 11 — Advanced Business Logic & OOP

Status: IMPLEMENTATION COMMITTED — automated CI and owner-local verification pending
Branch: `phase11/advanced-business-logic-oop`
Draft PR: https://github.com/musa390/My-project/pull/8
Base branch: `phase10/payment-integration`

Phase 10 remains IN PROGRESS independently. Phase 11 does not close or waive the unfinished bKash/Nagad sandbox work. SSLCommerz is explicitly deferred until the final payment phase.

## Goal

Apply Java OOP to real e-commerce business rules. Markers [ENC], [INH], [ABS], [POL], and [CRUD] identify actual implementation points; each must correspond to executable behavior and tests.

## Implemented work packages

1. **Encapsulation and domain invariants**
   - Immutable `InventoryQuantity` validates on-hand, reserved and reorder quantities; exposes available stock; validates reservation, release and adjustment operations.
   - Existing immutable checkout, coupon, money and payment domain objects retain controlled state and validation.
   - Product publication requires at least one active variant.

2. **Inheritance**
   - `AbstractCouponDiscountStrategy` centralizes discount caps, rounding and eligible-subtotal protection.
   - Fixed and percentage discount strategies inherit the shared invariant implementation.
   - The JDBC-first application does not force JPA entities to inherit `BaseEntity` where no concrete JPA entity model exists.

3. **Abstraction and polymorphism**
   - `CouponDiscountStrategy`: fixed and percentage implementations selected at runtime.
   - `ShippingPolicy`: free-shipping threshold and flat-rate implementations selected by ordered strategy support.
   - `PaymentGateway`: existing provider contract retained; no bKash/Nagad adapters are claimed complete.
   - Order lifecycle, cancellation, inventory reservation, notification and return status behavior use focused contracts/policies.

4. **Product, inventory and order rules**
   - Product activation validates active variants.
   - Inventory value object rejects invalid quantities, over-reservation, and adjustments below reserved stock.
   - Checkout uses the inventory invariant while preserving row locks and atomic reservation ledger writes.
   - Existing order snapshot, cancellation, order status and stock release transactions remain in place.
   - Direct Staff status mutation cannot bypass the approved return-request workflow to set RETURNED.

5. **Discount and shipping strategies**
   - Existing fixed/percentage coupon strategies remain server-side.
   - Shipping now selects between configurable free-shipping threshold and flat-rate policies.
   - Default free-shipping threshold is disabled (0.00), preserving existing configured flat-rate behavior.

6. **Payment and refund rules**
   - Existing PaymentGateway abstraction and payment transition policy remain.
   - Refund amount policy centralizes remaining refundable balance and prevents over-refunds.
   - Refund requests remain manual review/reconciliation; no provider-side refund is claimed.

7. **Notifications**
   - Added transactional, idempotent in-app notification persistence and authenticated user-scoped inbox/read endpoints.
   - Order placement, customer cancellation and staff order-status changes enqueue notifications.
   - No email, SMS, push provider or external delivery is configured.

8. **Return workflow**
   - Customer can request one full-order return only after delivery.
   - Idempotency key and order ownership are enforced.
   - Staff can approve/reject; only approved requests can be marked received.
   - Receiving an approved return transitions the order to RETURNED with audit history.
   - Return receipt does not automatically restock inventory or refund payment; the existing manual refund workflow remains separate.

9. **Database and transactions**
   - Additive Flyway migrations V7 (user notifications) and V8 (return requests/audit).
   - V1–V6 are unchanged.
   - Multi-row writes use Spring transaction boundaries; return/order/notification records are written atomically.

## OOP marker convention

- [ENC] Encapsulation and invariant protection
- [INH] Inheritance where it removes duplicated behavior
- [ABS] Abstraction/interface contract
- [POL] Runtime polymorphism/strategy selection
- [CRUD] Persisted create/read/update/archive behavior with authorization

## Verification gates

- Maven `clean verify` on Java 21 + MySQL 8.4.
- Backend CI and Database Migration CI must pass on the Phase 11 commit.
- New unit/integration tests cover inventory boundaries, product publication, shipping selection, notification idempotency/ownership, refund balance, return eligibility/transitions and existing order regressions.
- Owner-local Windows clean-database verification remains a separate final gate.
- No Customer Website or Admin Website source changes.
- No secrets in source control.
- Keep PR #8 draft/unmerged until owner review; do not merge without explicit approval.

## Explicitly out of scope

- SSLCommerz: deferred until the final payment phase.
- bKash/Nagad provider adapters, callbacks and server-to-server verification: remain Phase 10 blockers pending official merchant specifications and sandbox acceptance.
- Live payment processing or real-money transactions.
