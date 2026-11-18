# Phase 11 — Advanced Business Logic & OOP

Status: IN PROGRESS — kickoff and implementation plan
Branch: `phase11/advanced-business-logic-oop`
Parent workstream: `phase10/payment-integration`
Payment boundary: Phase 10 remains IN PROGRESS. Phase 11 may proceed on independent business-domain work. SSLCommerz is explicitly deferred until the final payment phase.

## Goals

Apply Java OOP to real e-commerce rules, not merely add classes or comments. Every rule must be enforced by executable code and covered by automated tests.

Required markers:
- `[ENC]` Encapsulation: private state, validated construction/mutation, controlled methods.
- `[INH]` Inheritance: shared audited/persistent base behavior only where it genuinely reduces duplication (for example, BaseEntity).
- `[ABS]` Abstraction: stable contracts such as DiscountStrategy, ShippingStrategy, PaymentGateway, and NotificationService.
- `[POL]` Polymorphism: multiple concrete strategies selected through interfaces, without provider-specific conditionals in core workflows.
- `[CRUD]` Persistence operations: explicit create/read/update/archive behavior with authorization and transaction boundaries.

A marker comment is documentation only; acceptance requires behavior and tests.

## Scope and work packages

1. Repository and domain audit
   - Inspect the current Phase 10 branch head and Phase 4–10 domain model, migrations, services, controllers, security permissions, and tests.
   - Reuse existing entities and tables; avoid duplicate models and unnecessary schema changes.
   - Preserve existing API compatibility unless a documented migration is necessary.

2. Encapsulation and shared entity model
   - Review Product, ProductVariant, Inventory, Order, OrderItem, Payment, and Refund state ownership.
   - Introduce or refine BaseEntity only if the existing persistence model supports a safe common abstraction.
   - Keep IDs, audit timestamps, and state changes controlled; validate invariants at domain/service boundaries.

3. Product and inventory business rules
   - Product publication/archival and variant rules.
   - Inventory available quantity = on-hand minus reserved; never permit negative available stock or adjustment below reserved stock.
   - Ensure all stock changes and movement audit writes are atomic.

4. Order business rules
   - Centralize allowed order-state transitions.
   - Preserve immutable order-item price/name snapshots.
   - Cancellation, reservation release, and return rules must be idempotent and transactional.
   - Do not allow client-supplied totals or status values to override server calculations.

5. Discount strategy
   - Define a DiscountStrategy abstraction and concrete strategies only for discounts supported by the current schema/business requirements.
   - Validate eligibility, date window, usage limits, minimum spend, currency, stacking policy, and rounding.
   - Ensure checkout preview and order creation use the same authoritative calculation.

6. Shipping strategy
   - Define ShippingStrategy and a result/value object.
   - Support only shipping methods and pricing rules grounded in the existing project schema/configuration; do not invent carrier integrations.
   - Validate destination/serviceability and calculate shipping server-side.

7. Payment strategy boundary
   - Reuse the existing PaymentGateway abstraction and payment state transition policy.
   - Do not implement SSLCommerz in Phase 11.
   - Do not pretend bKash/Nagad adapters or provider verification are complete; keep those Phase 10 blockers visible.
   - COD remains the only currently verified payment workflow until official sandbox specifications and verification tests are supplied.

8. Permission and notification rules
   - Centralize or consistently enforce least-privilege permission checks for customer/staff/admin actions.
   - Add a NotificationService abstraction with safe, testable delivery adapters (for example, in-app/database or test implementation) only where existing persistence supports it.
   - Never log credentials, reset tokens, payment secrets, or sensitive customer data.

9. Return/refund rules
   - Reuse Phase 10 refund request/manual-review/reconciliation workflow.
   - Keep refund request separate from confirmed provider refund; no automatic provider refund claim.
   - Enforce refundable balance, currency, authorization, idempotency, and valid transitions.

10. Database transaction management
    - Use Spring transactions at application-service boundaries.
    - Keep inventory, order, payment/refund, and audit writes atomic.
    - Add concurrency/rollback tests for stock reservation, cancellation, refund reconciliation, and duplicate requests.

## Implementation sequence

- Step 0: baseline audit and branch/CI check.
- Step 1: domain invariant and OOP marker audit.
- Step 2: BaseEntity/encapsulation improvements where justified.
- Step 3: Product and inventory rules.
- Step 4: Order lifecycle rules.
- Step 5: DiscountStrategy.
- Step 6: ShippingStrategy.
- Step 7: Payment strategy integration boundary (no SSLCommerz; no unverified provider adapters).
- Step 8: permissions and notification abstraction.
- Step 9: return/refund rule consolidation.
- Step 10: transaction/concurrency regression suite.
- Step 11: documentation, security review, full Maven verification, CI and final report.

## Acceptance criteria

- All five markers appear only where the corresponding real behavior exists.
- No decorative/inaccurate marker comments.
- Unit/integration tests cover normal, invalid, boundary, unauthorized, duplicate/idempotent, rollback, and concurrency-sensitive cases.
- Existing Phase 4–10 tests remain green.
- Flyway migrations are additive and reviewed; never rewrite applied migrations.
- No Customer Website/Admin Website source changes unless separately approved.
- No secrets in source, tests, logs, or Git history.
- Phase 10 stays IN PROGRESS until bKash/Nagad official sandbox work is verified; Phase 11 progress does not change Phase 10 status.
- SSLCommerz remains deferred until the final payment phase.
- Keep the Phase 11 PR draft/unmerged until owner review; do not merge without explicit approval.

## Current known dependencies

- Phase 10 branch has a PaymentGateway interface and COD/refund workflows.
- bKash and Nagad provider-specific adapters, authenticated callbacks, server-side verification, and real sandbox acceptance remain incomplete.
- Official merchant sandbox specifications are required before implementing provider-specific protocol details.
