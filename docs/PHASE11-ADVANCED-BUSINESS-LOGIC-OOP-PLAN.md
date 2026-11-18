# Phase 11 — Advanced Business Logic & OOP

Status: IN PROGRESS
Branch: phase11/advanced-business-logic-oop
Base branch: phase10/payment-integration
Phase 10 remains IN PROGRESS independently. Phase 11 work is intentionally based on the Phase 10 integration branch so it does not bypass or implicitly merge unfinished payment work.

## Goal

Implement production-oriented Java OOP in the existing Luxe Perfume backend. Markers such as [ENC], [INH], [ABS], [POL], and [CRUD] are documentation aids only; each must correspond to working behavior and automated tests.

## Scope

1. Inspect the existing domain model, schema, services, controllers, security rules, and tests before changing code.
2. Encapsulation: protect Product, Order, and Inventory invariants; validate state changes through methods/services rather than public mutable fields.
3. Inheritance: introduce or reuse a BaseEntity only if it fits the existing persistence model and does not create unnecessary ORM assumptions. Prefer composition where inheritance adds no value.
4. Abstraction: define focused contracts such as DiscountStrategy and ShippingStrategy; retain the existing PaymentGateway abstraction.
5. Polymorphism: implement selectable strategies through interfaces and dependency injection. bKash/Nagad/SSLCommerz concrete gateway adapters remain Phase 10 work and must not be represented as implemented by Phase 11.
6. Product rules: validate price, availability, active status, and safe updates.
7. Inventory rules: prevent negative stock and overselling; reserve/release/adjust stock transactionally and idempotently where appropriate.
8. Order rules: enforce legal status transitions, ownership, item snapshot integrity, and transactional consistency.
9. Discount strategy: support explicit, testable discount policies with amount/currency/eligibility validation.
10. Shipping strategy: support selectable shipping cost/eligibility policies without hard-coded controller branching.
11. Permission rules: enforce least privilege at service/API boundaries; never rely only on UI checks.
12. Notifications: introduce a testable notification abstraction with a safe local/logging implementation; do not send real messages by default.
13. Return/refund rules: align with Phase 10 manual-review/reconciliation workflow; never mark a refund paid merely because it was requested.
14. Database transaction management: keep multi-record business operations atomic and add rollback/concurrency tests.

## OOP marker convention

- [ENC] Encapsulation and invariant protection
- [INH] Inheritance (only where justified)
- [ABS] Abstraction/interface contract
- [POL] Runtime polymorphism/strategy selection
- [CRUD] Persisted create/read/update/delete operation

## Engineering constraints

- Preserve the existing Java/Spring Boot/MySQL/Flyway architecture and coding conventions.
- Inspect current files and migrations before editing; do not invent class or table names.
- Add forward-only Flyway migrations only when schema changes are required.
- Keep secrets out of source control and logs.
- No live payment processing. Phase 10 provider integration remains separate.
- No destructive database reset, force push, or PR merge.
- Add unit/integration tests for each business rule and run the relevant Maven test/build plus GitHub Actions CI.

## Completion criteria

- Each in-scope rule has implementation and automated tests.
- OOP markers map to real code, not comments alone.
- Existing Phase 10 payment/refund behavior remains compatible.
- Database migrations pass from a clean database.
- Backend test/build and required CI workflows pass on the Phase 11 branch.
- A truthful Phase 11 status report and draft PR are published; no merge without explicit approval.

## Initial sequencing

Step 1: Repository/domain architecture audit and baseline test status.
Step 2: Domain encapsulation and shared entity design.
Step 3: Product and inventory business rules.
Step 4: Order lifecycle and transactional rules.
Step 5: Discount and shipping strategies.
Step 6: Permission and notification abstractions.
Step 7: Return/refund compatibility and full regression tests.
Step 8: Documentation, CI verification, and draft PR.
