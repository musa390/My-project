# Phase 6 — Authentication & Authorization Report

**Branch:** `phase6/authentication-authorization`  
**Status:** Backend implementation and 13-test CI suite passed; external provider and owner-local verification remain pending.  
**Base:** `phase5/backend-foundation`

## Implemented

- Spring Security stateless API filter chain; health/OpenAPI/auth endpoints are explicitly public and other business routes require authentication.
- BCrypt password hashing (work factor 12); registration creates CUSTOMER users only.
- JWT access credentials with signature, issuer, audience, expiry and purpose validation.
- Access and refresh credentials are delivered only through HttpOnly cookies. Production profile rejects the development JWT key and non-Secure auth cookies.
- Database-backed sessions; refresh secrets are stored as SHA-256 hashes and rotated on refresh.
- Logout revokes the server-side session and clears both cookies.
- Password-reset tokens are random, hashed in the database, expire after 30 minutes, are single-use, and revoke existing sessions after a successful reset.
- Forgot-password returns the same public response for known and unknown accounts. Resend delivery is enabled only when configured.
- Optional Google OIDC profile uses Spring Security's OIDC flow, requires a verified email, provisions a CUSTOMER identity and avoids automatic email-only account linking.
- CUSTOMER, ADMIN, MANAGER and INVENTORY_STAFF role policies; staff product/order/inventory/payment routes require their corresponding permission authorities.
- CSRF cookie repository and CORS header support for `X-XSRF-TOKEN`.
- Flyway V3 adds `auth_sessions` and `password_reset_tokens`; V1 and V2 were not modified.

## API endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/auth/csrf` | Obtain CSRF token |
| POST | `/api/v1/auth/register` | Customer registration |
| POST | `/api/v1/auth/login` | Local login |
| POST | `/api/v1/auth/refresh` | Rotate refresh credential |
| POST | `/api/v1/auth/logout` | Revoke current session |
| POST | `/api/v1/auth/forgot-password` | Request reset email |
| POST | `/api/v1/auth/reset-password` | Consume reset token |

Unsafe cookie-authenticated requests must send the CSRF value in `X-XSRF-TOKEN`. Access and refresh tokens are not returned in the JSON response and must not be stored in localStorage/sessionStorage.

## Automated verification

Latest backend verification at the time of this report:
- GitHub Actions run: [Backend Foundation CI](https://github.com/musa390/My-project/actions/runs/44127080101)
- MySQL 8.4 service and Java 21.
- `mvn clean verify`: **BUILD SUCCESS**.
- Integration tests: **13 run, 0 failures, 0 errors, 0 skipped**.
- Coverage includes health/OpenAPI, Flyway V1–V3, registration/password hashing, login, refresh rotation, logout/revocation, forgot-password response privacy, CSRF, anonymous protected routes and customer/staff permission boundaries.

The separate Phase 6 database workflow is being finalized to assert the two V3 tables after preserving the Phase 4 V1/V2 verification. Do not interpret a pending/failed database workflow as a pass.

## External configuration still required

These credentials must be supplied by the project owner through local environment configuration or the deployment secret manager; never commit them:
- Google OAuth Client ID/Secret and the exact backend redirect URI. Google sign-in remains disabled until the `google` Spring profile is activated with valid credentials.
- Resend API key and verified sender address. Without them, the reset request still returns a generic response but no email can be delivered.
- Unique production `JWT_SECRET_BASE64`, `AUTH_COOKIE_SECURE=true`, HTTPS, production CORS origins and the approved frontend redirect URL.

## Remaining acceptance checks

- Live Google authorization-code/OIDC callback with the owner's Google Cloud credentials.
- Real reset-email delivery and end-to-end password reset with a configured Resend sender.
- Production HTTPS cookie, CORS and secret-manager configuration review.
- Owner-run local verification against a clean, separately created database. Preserve the existing local database with its earlier failed V1 migration; do not run repair/drop operations blindly.
- Final security review and approval before merging the draft PR.

## Protected project areas

No source files under `customer-frontend/` or `admin-frontend/` were changed in Phase 6. Phase 4 V1/V2 migrations were preserved. No real secrets or provider credentials were committed.

## Pull request

Main implementation PR: [#1 — Phase 6 Authentication & Authorization](https://github.com/musa390/My-project/pull/1) (draft; not merged).
