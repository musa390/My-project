# Phase 10 — Payment Integration Plan

**Status:** Started — Step 1 (payment schema and integration audit) in progress. No gateway implementation or payment-state mutation is complete yet.
**Working branch:** `phase10/payment-integration`
**Base:** `phase9/checkout-order-management`
**Previous work:** Phase 9 Draft PR #6 remains open, draft and unmerged.
**Stack:** Java 21, Spring Boot 4.1.2, MySQL 8.4, Spring MVC, JdbcTemplate, Flyway, JUnit/MockMvc.
**Scope:** Backend payment integration only. Customer Website and Admin Website frontend source must remain unchanged.

## Objectives and acceptance criteria

Implement Cash on Delivery (COD), SSLCommerz Sandbox, bKash Sandbox and Nagad Sandbox behind one Java OOP gateway abstraction. Persist payment attempts and provider references. Initiate payments only for an authenticated customer's own eligible order. Process provider callbacks/IPN safely and idempotently. Verify payment server-to-server before marking either a payment or order PAID. Handle failed/cancelled/pending transactions and refunds with an auditable record.

The browser redirect or a frontend success message is never proof of payment. A payment can become PAID only after a provider-specific server-side verification response matches the expected order reference, amount, currency and transaction identity, and passes the provider's required signature/authentication checks.

All environments remain sandbox/demo. No live gateway endpoints, live credentials, real-money processing or production activation in this phase.

## Confirmed existing schema (V1)

The Phase 9 branch already contains:
- `customer_orders.payment_status`: `UNPAID`, `PENDING`, `PAID`, `FAILED`, `PARTIALLY_REFUNDED`, `REFUNDED`.
- `payments`: order FK, provider, payment method, provider transaction ID/reference, amount, currency, payment status, initiation/completion timestamps and JSON provider payload.
- Allowed providers: `COD`, `SSLCOMMERZ`, `BKASH`, `NAGAD`, `MANUAL`.
- Allowed payment statuses: `INITIATED`, `PENDING`, `AUTHORIZED`, `PAID`, `FAILED`, `CANCELLED`, `REFUNDED`, `PARTIALLY_REFUNDED`.
- Staff permission catalogue includes `payments.read`.

The existing schema must be inspected further for refund persistence, attempt/idempotency requirements, provider-session uniqueness, and audit needs before deciding whether a Flyway V4 migration is necessary. V1/V2/V3 are immutable; do not edit them.

## Java OOP architecture

Controllers only validate/map HTTP requests and responses. Application services coordinate use cases and transactions. Domain objects enforce state and amount invariants. SQL stays in repository/persistence classes, not controllers or domain objects. Use `BigDecimal` for all monetary values.

Planned abstractions:
- `PaymentGateway` — provider contract for initiation, verification and (where officially supported) refund.
- `SslCommerzGateway`, `BkashGateway`, `NagadGateway` — separate polymorphic adapters.
- `CashOnDeliveryService` — COD-specific lifecycle, not a fake remote gateway.
- `PaymentService` / `PaymentApplicationService` — orchestrates authenticated order ownership, payment attempt persistence and state transitions.
- `PaymentRepository` and `RefundRepository` — persistence boundaries.
- `PaymentStatus` / transition policy — allowed, monotonic state transitions and idempotency.
- Provider HTTP clients/configuration — isolated from business rules; sandbox base URLs only.

Use dependency injection and strategy/polymorphism. Do not put all provider behavior into a large switch/if-else block. Avoid inheritance unless a stable shared algorithm exists.

## Planned API surface (final paths subject to security and schema audit)

Customer:
- `POST /api/v1/orders/{orderNumber}/payments` — initiate COD or a selected sandbox gateway for the authenticated customer's own eligible order.
- Provider-specific server callbacks/IPN endpoints — accept only provider callback data, then verify with the provider from the backend.
- `GET /api/v1/orders/{orderNumber}/payments` — return the authenticated customer's own payment history/status, without secrets or raw sensitive payloads.

Staff/Admin:
- `GET /api/v1/staff/payments` — permission-protected payment search/list/detail using `payments.read`.
- Refund initiation/review endpoints only for authorized staff and only where the provider's official sandbox API supports it.

No endpoint accepts a client-supplied amount as authoritative. Amount and currency are loaded from the locked order row.

## Implementation sequence

1. **Step 1 — Existing Payment Schema & Security Audit**
   - Inspect V1 payment table, order payment states, auth principal, CSRF/callback security, permissions, configuration and test conventions.
   - Define payment/refund state machines, idempotency keys and the exact schema gap.
   - Decide whether a narrowly-scoped V4 migration is required. Never change V1/V2/V3.
2. **Step 2 — Payment Domain & OOP Foundation**
   - Add immutable/value domain types, enums, transition policies, gateway interface, provider configuration and unit tests.
3. **Step 3 — COD Payment**
   - Persist COD payment record and define collection/confirmation rules without marking it PAID at order placement.
4. **Step 4 — Payment Initiation & Payment Records**
   - Authenticated ownership checks, order row locks, amount/currency from DB, idempotent attempts and provider-session persistence.
5. **Step 5 — SSLCommerz Sandbox**
   - Hosted checkout initiation, IPN/callback handling and mandatory server-side Order Validation API checks.
6. **Step 6 — bKash Sandbox**
   - Merchant-approved sandbox credentials; server-side token/create/execute/query flow according to current official bKash docs.
7. **Step 7 — Nagad Sandbox**
   - Implement only against official merchant-issued sandbox documentation and credentials. Do not infer endpoints, crypto/signature algorithms or callback fields from unofficial examples.
8. **Step 8 — Failure, Retry & Idempotency**
   - Duplicate callbacks, replay attempts, mismatched amount/currency/order, timeouts and pending/failed/cancelled states.
9. **Step 9 — Refund Workflow**
   - Persist refund requests/results and audit events. Provider refund calls are sandbox-only and implemented only where the official API supports them; otherwise retain a controlled manual-review state.
10. **Step 10 — Security, Integration Tests & CI**
    - MySQL 8.4 integration tests, mocked provider contract tests, authorization/ownership tests, duplicate callback tests, negative verification tests and full Maven clean verify.
11. **Step 11 — Documentation & Completion Report**
    - API docs, environment variable template with placeholders only, sandbox test scenarios, project status and completion report.

## Security and correctness rules

- Never store or commit credentials, private keys, tokens, PINs, OTPs or raw authentication headers. Use environment variables and local ignored configuration.
- Never ask the owner to paste credentials into chat. They may configure them locally through secure prompts/environment variables.
- Do not expose provider secrets, signatures, full payloads or personal financial data in API responses/logs.
- Callback endpoints are not trusted merely because they are reachable. Verify using the provider's server API and required signature/authentication checks.
- Compare verified provider order/transaction reference, exact amount and currency with the persisted payment attempt and order.
- Use database transactions and row locks around state transitions. Ensure repeated callbacks cannot create duplicate paid/refund transitions.
- Do not downgrade PAID to FAILED because of a late failure callback. Conflicting terminal provider states require reconciliation/manual review.
- COD is not PAID at order creation; it remains unpaid until the defined collection confirmation workflow is completed.
- Do not fulfil or release reserved stock merely because a browser returned to a success URL.
- Do not change Customer Website or Admin Website source files.
- Do not merge Phase 9 PR #6 or any Phase 10 PR without explicit owner approval.
- Preserve the isolated Phase 9 verification database and all existing Docker volumes.

## Provider documentation baseline

- SSLCommerz sandbox docs: https://sandbox-gw.sslcommerz.com/docs
  - Session creation, IPN and Order Validation API are separate steps. The validation API must confirm the transaction before local payment status changes.
- SSLCommerz v4 docs: https://developer.sslcommerz.com/doc/v4/index.html
- bKash developer docs: https://developer.bka.sh/docs/checkout-url-process-overview
  - Use the merchant-approved developer portal documentation for token, create, execute, query and refund operations.
- Nagad developer portal: https://developer.nagad.com.bd
  - Public documentation and merchant-specific sandbox access must be confirmed with Nagad. Do not implement from unofficial endpoint lists.

## Completion definition

Phase 10 is complete only when all required demo/sandbox flows are covered by passing automated tests and a clean-database local verification: COD lifecycle; SSLCommerz, bKash and Nagad sandbox initiation/verification; persisted payment records; correct order/payment statuses; failed/cancelled/pending handling; duplicate/replayed callback safety; and the supported refund workflow. No frontend success message alone may mark an order PAID.
