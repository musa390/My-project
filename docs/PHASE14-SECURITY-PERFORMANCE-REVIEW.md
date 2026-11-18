# Phase 14 — Security, Dependency & Performance Review

Review scope: source inspection and automated gates on `phase14/testing-security-quality-assurance`. This is a review record, not a penetration-test certificate.

## Security controls inspected

### Authentication and sessions
- Password encoder is BCrypt with cost 12.
- Registration enforces a 12–72 UTF-8 byte password range.
- Access tokens are signed JWTs with issuer, audience, purpose, subject, session ID, issue time, and expiry.
- JWT authentication checks the server-side session is active, unexpired, not revoked, and belongs to an active, non-deleted account.
- Refresh/logout/session behavior has integration-test coverage in `BackendFoundationIntegrationTest`.
- JWT parsing failures clear the security context and do not expose parser exceptions to the caller.

### Authorization
- Spring method security is enabled.
- Admin routes require ADMIN.
- Staff order reads and writes have separate authorities.
- Inventory reads and writes have separate authorities.
- Product/catalog writes use permission authorities.
- Customer/staff permission boundaries have integration-test coverage.

### Browser/API security
- CSRF protection is enabled with a cookie token repository.
- HTTP Basic and form login are disabled.
- CORS uses an explicit configured origin list and credentials; wildcard origins are not used in the inspected implementation.
- Authentication cookies are created by the authentication service; cookie flags require runtime/profile verification as well as source review.

### Payment/refund behavior
- The inspected refund service implements a manual-review and reconciliation workflow.
- It does not claim a provider refund occurred until staff record a reconciliation reference.
- Existing tests cover refund amount rules and payment/refund integration paths.
- Live bKash/Nagad or other payment-provider sandbox verification is not evidenced by this source review and remains outside this automated gate.

## Security configuration risks / required deployment controls

1. The application has a development JWT secret fallback. The JWT service rejects that exact fallback when the active profile contains `prod`; deployment must activate the production profile and inject a unique secret from a secret manager. Do not rely on the fallback in any deployed environment.
2. The test and local environments use database credentials from environment variables. Never commit `.env`, expose `MYSQL_PASSWORD`, or place credentials in test reports.
3. OAuth, email, and media-provider integrations are optional/configuration-dependent. Their real provider security and redirect allowlists need owner-controlled sandbox checks before production use.
4. This review did not perform an external penetration test, authenticated browser session test against a deployed environment, or live payment sandbox transaction.

## Dependency audit evidence

- Frontend jobs run `npm audit --audit-level=high` for customer and admin dependency trees.
- Backend CI resolves and inventories Maven dependencies. A Maven CVE database scan is not claimed by dependency inventory alone; review advisories and update candidates before release.
- Any reported high/critical advisory must be assessed for affected version, runtime reachability, exploitability, and safe upgrade path. Do not suppress audit failures without a written disposition.

## Performance review

- Chromium smoke checks exercise customer/admin route rendering at 390×844, 768×1024, and 1440×900 and fail on horizontal overflow or HTTP 404/5xx.
- Backend integration tests exercise database-backed catalog, cart, checkout, order, inventory, payment, refund, return, and staff flows.
- These checks are correctness/route smoke tests, not a production load test. No production throughput, p95 latency, or concurrency SLA is claimed.
- Before production traffic, run a separately approved load test against a staging environment with representative data and monitor database query plans, connection-pool saturation, and p95/p99 latency.

## Findings disposition

| Finding | Severity | Required disposition |
|---|---|---|
| Development JWT fallback exists | High deployment risk if production profile/secret are misconfigured | Enforce production profile and secret-manager injection at deployment |
| Local Windows Maven run previously failed with MySQL 1045 | Environment/configuration blocker | Use the supplied local verification helper; CI uses isolated MySQL service |
| Backend dependency CVE scan not yet automated | Medium process gap | Review Maven advisories before release; do not label dependency inventory as CVE scan |
| No staging load-test evidence | Medium process gap | Run approved staging load test before production launch |
| Live payment provider sandbox not verified | External dependency | Owner-provided sandbox access required; COD/manual refund tests do not prove gateway integration |

## Acceptance boundary

Do not declare Phase 14 fully accepted unless:
- all CI quality-gate jobs pass on the final PR head;
- backend tests and Flyway migration verification pass;
- customer/admin TypeScript, lint, production build, and Chromium route/responsive smoke pass;
- npm audits and CodeQL results are reviewed;
- no unresolved critical/high exploitable finding remains;
- owner-local verification and any provider-specific staging checks are either passed or explicitly recorded as release blockers/waivers.
