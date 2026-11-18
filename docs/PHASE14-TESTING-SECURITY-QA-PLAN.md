# Phase 14 — Testing, Security & Quality Assurance

## Goal

Establish repeatable automated and owner-environment verification for the Phase 13 customer storefront, admin workspace, backend APIs, and critical commerce workflows. Resolve critical defects found during verification before declaring acceptance.

## Branch and dependency

- Branch: `phase14/testing-security-quality-assurance`
- Base: `phase13/frontend-backend-integration`
- Phase 13 PR #10 remains open, Draft, and unmerged.
- Phase 14 work must not merge Phase 13 or change its PR state without explicit owner approval.

## Baseline (2026-11-18)

### Existing CI evidence

Phase 13 PR #10 records:
- Backend CI: Maven BUILD SUCCESS; 80 tests; 0 failures/errors; Flyway migrations validated.
- Frontend CI: TypeScript, ESLint, customer/admin production builds and route smoke tests passed.

### Owner Windows environment

Latest supplied local Maven run:
- Tests run: 77
- Failures: 0
- Errors: 54
- Skipped: 0
- Result: BUILD FAILURE

The earlier first-cause diagnostic reported MySQL authentication failure for `luxe_app` (SQLSTATE 28000 / error 1045). Docker TCP login succeeded as `luxe_app@%` against `luxe_perfume` on MySQL 8.4.13. Windows host TCP connectivity to 127.0.0.1:3308 also passed.

The latest failed local run must be reproduced and diagnosed; do not assume the environment fix succeeded until a fresh run proves it.

## Workstreams and acceptance

### 14.1 Baseline and test infrastructure
- Reconcile CI and local test counts and identify the exact tested commit.
- Inspect Maven/Spring test property sources, environment variable resolution, test profiles, and database endpoints.
- Make test database configuration explicit and reproducible.
- Ensure test data is isolated and repeatable; preserve owner data.
- Verify CI triggers for the Phase 14 branch and plan documentation.

### 14.2 Backend automated tests
- Unit tests for domain rules and validation.
- Service tests for transactional business logic and failure cases.
- Repository tests for query correctness and persistence constraints.
- API integration tests for status codes, validation, serialization, and error handling.
- Authentication tests for registration/login/session/CSRF/logout flows.
- Authorization tests for customer/staff role and permission boundaries.

### 14.3 Critical commerce flows
- Product/variant availability and price integrity.
- Cart and checkout quote.
- Order creation and immutable order-item snapshots.
- Inventory reservation, cancellation release, and idempotency.
- COD payment confirmation and manual refund/reconciliation workflow.
- Returns and audited staff order transitions.

### 14.4 Frontend quality
- Customer and admin component/integration tests where the existing stack supports them.
- TypeScript typecheck.
- ESLint.
- Customer and admin production builds.
- Route and API error-state smoke tests.

### 14.5 Browser and responsive QA
- Customer journeys: auth, catalog, product, cart, address, checkout/COD, order history, cancellation/tracking, contact/newsletter.
- Admin journeys: protected access, catalog management, order/customer/payment workflows, inventory, returns, content, reviews, newsletter, analytics, and staff permissions.
- Responsive viewport checks and supported-browser smoke checks.
- Use clearly labelled test records; do not create real payments or send live provider messages without owner authorization.

### 14.6 Security, dependencies, and performance
- Review cookie flags, CSRF, CORS, JWT configuration, password/session handling, object ownership, staff permissions, and sensitive logging.
- Audit Maven and npm dependencies using supported project tooling; classify findings by severity and reachability.
- Review API pagination, query behavior, indexes, and critical endpoint response time.
- Do not publish secrets, tokens, personal data, or database dumps in CI artifacts or chat.

### 14.7 Final regression and acceptance
- Re-run all required backend and frontend checks after fixes.
- Record exact commit, commands, test counts, outcomes, and known limitations.
- Critical unresolved defects: zero.
- Backend tests and frontend typecheck/lint/build: pass.
- Customer and admin critical browser journeys: pass.
- Security and dependency findings: reviewed and dispositioned.
- Owner acceptance: explicit.

## Rules

- No destructive database reset, volume deletion, or migration rewrite to make tests pass.
- Do not skip failing tests or weaken assertions merely to obtain a green build.
- Do not claim a test passed without its actual result.
- Live Google OAuth, Cloudinary, Resend, bKash/Nagad or other provider checks require the owner's authorized sandbox configuration; mock tests do not count as live-provider verification.
- Phase 14 is not complete until the acceptance criteria above are evidenced.
