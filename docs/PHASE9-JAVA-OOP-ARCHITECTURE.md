# Phase 9 — Java OOP Architecture

**Language:** Java 21  
**Framework:** Spring Boot 4.1.2  
**Persistence:** MySQL 8.4 using the existing `JdbcTemplate` convention  
**Design goal:** Apply all four OOP pillars to real checkout/order business rules. OOP is part of Phase 9 itself; it is not deferred to Phase 11.

## Architectural rules

- Controllers handle HTTP mapping, request validation and response envelopes only.
- Application services coordinate use cases and transactions.
- Domain objects encapsulate invariants and state transitions.
- Interfaces define replaceable business policies.
- Strategy implementations are selected polymorphically; do not use large `if/else` blocks for coupon or shipping type selection.
- Repositories/gateways encapsulate SQL access. Keep SQL out of controllers and domain objects.
- Use `BigDecimal` for all money. Never use `double` or `float` for monetary values.
- Keep current schema and migration history unchanged unless a reviewed schema gap proves a V4 migration necessary.

## Four OOP pillars and concrete Phase 9 use

### 1. Encapsulation

Domain objects keep state private and expose behavior that preserves invariants.

Planned examples:
- `OrderDraft`: private customer/address/cart snapshot and pricing data; constructor/factory validates required values; no public setters that allow invalid totals.
- `OrderLine`: private quantity, unit price and discount; validates positive quantity and nonnegative amounts; calculates its own line total.
- `MoneyBreakdown`: immutable subtotal, discount, shipping, tax and grand total; creates totals only through a validating factory.
- `OrderStatus`: controlled transition method; callers cannot assign arbitrary status.

### 2. Inheritance

Use inheritance only where implementations genuinely share a stable algorithm.

Planned examples:
- `AbstractCouponDiscountStrategy` implements shared percentage/fixed discount safety rules and common rounding/cap behavior.
- `PercentageCouponDiscountStrategy` and `FixedCouponDiscountStrategy` extend that base class and provide their specific calculation.
- Avoid creating a generic inheritance hierarchy for every DTO or database row.

### 3. Abstraction

Use interfaces to describe what a policy/service does without exposing its implementation.

Planned interfaces:
- `CouponDiscountStrategy`: identifies supported coupon type and calculates eligible discount.
- `ShippingPolicy`: calculates shipping from a validated server-side checkout context.
- `TaxPolicy`: calculates tax from a server-side pricing context.
- `CheckoutQuoteService`: produces a validated quote.
- `OrderRepository` and `InventoryReservationService`: isolate persistence and stock reservation operations where it improves testability.

### 4. Polymorphism

Application services depend on interfaces and call the same method on different implementations.

Planned examples:
- Inject `List<CouponDiscountStrategy>`; select the strategy that supports `PERCENTAGE` or `FIXED`, then invoke the same `calculate(...)` contract.
- Inject one configured `ShippingPolicy`; later policies can change without changing checkout orchestration.
- Inject `TaxPolicy`; tax behavior can be configured or replaced without changing order creation.

## Planned package structure

```text
backend/src/main/java/com/luxeperfume/backend/
  checkout/
    CheckoutController.java
    CheckoutApplicationService.java
    dto/
      CheckoutPreviewRequest.java
      CheckoutQuoteResponse.java
    domain/
      CheckoutContext.java
      MoneyBreakdown.java
      OrderLine.java
      OrderDraft.java
    pricing/
      CheckoutQuoteService.java
      CouponDiscountStrategy.java
      AbstractCouponDiscountStrategy.java
      PercentageCouponDiscountStrategy.java
      FixedCouponDiscountStrategy.java
      ShippingPolicy.java
      TaxPolicy.java
  order/
    OrderController.java
    StaffOrderController.java
    OrderApplicationService.java
    OrderStatus.java
    OrderStatusTransitionPolicy.java
    OrderRepository.java
    OrderNumberGenerator.java
  inventory/
    InventoryReservationService.java
```

Package names and exact class boundaries may be refined after implementation, but the four OOP pillars and their business responsibilities must remain represented.

## Transaction boundary

Order creation is one application-service transaction:
1. Resolve authenticated customer identity.
2. Lock and validate the owned address and active cart.
3. Read current catalog prices and lock inventory rows in deterministic variant-ID order.
4. Lock and revalidate coupon availability/limits if a coupon is supplied.
5. Calculate totals through the pricing abstractions.
6. Persist order and immutable snapshots.
7. Reserve stock and write inventory movement ledger entries.
8. Record coupon redemption and increment usage.
9. Convert the active cart.
10. Commit all operations together or roll everything back.

Checkout preview is read/validation only and must not reserve stock or consume coupon usage.

## Configuration policy

Shipping and tax amounts must come from explicit server-side configuration or a documented policy. Do not silently invent a delivery fee, accept a browser-provided fee, or assume a zero tax policy without explicit configuration. The configuration values must be validated at application startup.

## Test expectations for OOP

- Unit-test each coupon strategy independently, including rounding, caps and invalid values.
- Test polymorphic strategy selection for both supported coupon types.
- Test domain-object invariants and order status transitions.
- Mock policy interfaces to test checkout orchestration without external integrations.
- Add MySQL 8.4 integration tests for transaction rollback, coupon limits, ownership and stock reservation.
