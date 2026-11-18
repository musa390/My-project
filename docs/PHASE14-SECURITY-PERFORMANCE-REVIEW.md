# Phase 14 — Security, Dependency & Performance Review

Review scope: source inspection and automated gates on `phase14/testing-security-quality-assurance`. This is not an external penetration-test certificate.

## Automated evidence (2026-11-18)

The Phase 14 quality-gate run on commit `0d54b18cebf1d65a1c6fcb5c7153a54cefed0d5a` completed these jobs successfully:

- Backend: Maven `clean verify`; 80 tests, 0 failures, 0 errors, 0 skipped; MySQL 8.4 service and Flyway migrations.
- Backend latency smoke: 50 health-endpoint requests; p50 5.0 ms, p95 20.7 ms, max 86.1 ms. This is a CI health-endpoint smoke only, not a production load test.
- Customer frontend: static verifier, TypeScript, ESLint, production dependency audit, production build, Chromium route/responsive smoke. 10 routes and 3 viewport sizes passed.
- Admin frontend: static verifier, TypeScript, ESLint, production dependency audit, production build, Chromium route/responsive smoke. 18 routes and 3 viewport sizes passed.
- CodeQL Java/Kotlin and JavaScript/TypeScript analyses passed.
- Maven dependency inventory passed.
- OSV scan completed and uploaded its report. That run scanned the pre-refresh customer lockfile and reported 7 npm vulnerability records across 4 packages (4 High, 3 Medium), including PostCSS 8.4.31 and picomatch 2.3.2/4.0.7. This scan predates the lockfile refresh below and must not be treated as the final dependency snapshot.

## Dependency remediation and remaining advisory

- Added a PostCSS override to both frontend manifests and ran non-forced `npm audit fix`.
- The generated customer and admin package-lock files were committed to the Phase 14 branch by the CI lockfile-sync job.
- The committed lockfiles now resolve PostCSS 8.5.23.
- Blocking `npm audit --omit=dev --audit-level=high` passed in both frontend jobs: no high/critical production dependency advisory was reported.
- The full development-tooling audit still records high-severity findings rooted in picomatch (GHSA-fc5q-39xc-vjpf / CVE-2026-112366). The advisory affects picomatch through 4.0.7 and currently lists no patched version. It is present only in development/lint tooling in these lockfiles, not in the production dependency tree. Do not use a forced downgrade of `eslint-config-next` merely to silence the audit; that would mismatch the Next.js 15 application. Keep this as an explicit accepted dev-tooling risk until upstream publishes a fixed version, then update and rerun audits.

The next quality-gate run on the post-refresh branch commit must confirm the OSV scan against the committed lockfiles. The earlier OSV result is retained as historical evidence only.

## Security controls inspected

### Authentication and sessions
- Password encoder is BCrypt with cost 12.
- Registration enforces a 12–72 UTF-8 byte password range.
- Access tokens are signed JWTs with issuer, audience, purpose, subject, session ID, issue time, and expiry.
- JWT authentication checks the server-side session is active, unexpired, not revoked, and belongs to an active, non-deleted account.
- Refresh/logout/session behavior has integration-test coverage.
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
- Authentication cookie flags still require deployment-profile verification.

### Payment/refund behavior
- Refund processing uses a manual-review and reconciliation workflow.
- It does not claim a provider refund occurred until staff record a reconciliation reference.
- Integration tests cover COD/payment/refund paths.
- No live bKash/Nagad or other provider sandbox transaction is claimed.

## Required deployment controls

1. The application has a development JWT secret fallback. The JWT service rejects that exact fallback when the active profile contains `prod`; deployment must activate the production profile and inject a unique secret from a secret manager.
2. Never commit `.env`, expose `MYSQL_PASSWORD`, or place credentials in test reports.
3. OAuth, email, and media-provider integrations require owner-controlled sandbox checks and verified redirect allowlists.
4. This review did not perform an external penetration test or a live provider transaction.

## Performance boundary

- Chromium smoke covers customer/admin routes at 390×844, 768×1024, and 1440×900 and fails on HTTP 404/5xx, uncaught page exceptions, or horizontal overflow.
- Backend tests exercise database-backed catalog, cart, checkout, order, inventory, payment, refund, return, and staff flows.
- The 50-request health endpoint latency smoke is not representative load testing. No production throughput or p95/p99 SLA is claimed.
- Before production traffic, run a separately approved staging load test with representative data and monitor query plans, connection-pool saturation, and p95/p99 latency.

## Acceptance boundary

Do not declare Phase 14 fully accepted until:
- the OSV scan passes review on the post-refresh committed lockfiles;
- all latest CI quality-gate jobs pass;
- no unresolved critical production vulnerability or critical business-flow defect remains;
- authenticated customer/admin browser journeys are exercised in a staging environment with test accounts;
- any provider-specific sandbox checks are passed or explicitly waived by the owner;
- the dev-only picomatch advisory is explicitly accepted by the owner or removed after an upstream fix;
- owner acceptance is recorded.
