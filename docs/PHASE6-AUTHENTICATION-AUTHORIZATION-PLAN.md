# Phase 6 — Authentication & Authorization

**Status:** Started — architecture and implementation plan  
**Working branch:** `phase6/authentication-authorization`  
**Base branch:** `phase5/backend-foundation`  
**Scope:** Customer, Admin and Staff identity, authentication, sessions, permissions and protected API routes.

## Goals

1. Customer registration and login using email/password.
2. Secure password hashing and credential validation.
3. Short-lived signed JWT access credentials delivered only in Secure, HttpOnly cookies.
4. Server-side session records with expiry, refresh/rotation, revocation and logout.
5. Google OAuth 2.0 / OpenID Connect login with server-side token validation.
6. Forgot-password and reset-password flow using single-use, expiring, hashed reset tokens.
7. Role-based access control for CUSTOMER, STAFF and ADMIN, plus granular staff permissions.
8. Protected API routes and consistent 401/403 responses.
9. Security-focused automated tests and documentation.

## Security architecture decisions

- Use Spring Security for authentication, authorization, filter-chain configuration and method security.
- Use a password encoder designed for password storage (prefer Argon2id where dependency/runtime support is verified; otherwise use BCrypt with an explicitly configured work factor). Never store plaintext passwords or reversible password encryption.
- Use short-lived JWT access tokens in Secure, HttpOnly cookies. Do not store authentication tokens in localStorage or sessionStorage.
- Keep server-side session state in the database. Store only a cryptographic hash of refresh/session secrets; rotate credentials and revoke sessions on logout, password reset and security-sensitive account changes.
- Configure cookie attributes deliberately: HttpOnly, Secure in HTTPS environments, SameSite appropriate to the OAuth/deployment flow, narrow Path and expiry. Do not treat HttpOnly alone as CSRF protection.
- Protect cookie-authenticated state-changing requests with CSRF defenses and strict CORS allowlists. Never use wildcard credentialed CORS.
- Validate JWT signature, issuer, audience, expiry and token purpose. Use a managed signing key supplied through environment/secret management; never commit keys.
- Use OAuth 2.0 Authorization Code flow with OpenID Connect, state and nonce validation, and PKCE where applicable. Validate identity tokens on the backend.
- Do not automatically link accounts solely by an unverified email address.
- Password-reset tokens are single-use, time-limited, stored hashed, and must not reveal whether an email address is registered.
- Enforce authorization on the backend for every protected operation; frontend route guards are only a usability layer.
- Keep authentication and authorization separate from business features planned for later phases.

## Planned implementation sequence

### 6.1 — Security baseline
- Add compatible Spring Security and OAuth2/OIDC dependencies for the repository's Spring Boot version.
- Define security configuration, authentication entry point, access-denied handling, CORS and CSRF policy.
- Add security configuration tests before exposing protected endpoints.

### 6.2 — Identity and password credentials
- Add entities/repositories/services for customer identity and credentials, reusing the Phase 4 schema where it fits.
- Review existing users, roles, permissions and user-role mappings before any schema change.
- Implement registration, input validation, duplicate-account handling and password hashing.
- Add login and account-status checks.

### 6.3 — Session and JWT cookie lifecycle
- Add a server-side session/refresh-token persistence model through a new Flyway V3 migration if needed; never rewrite V1/V2.
- Issue short-lived access JWT cookies and rotate server-tracked refresh/session credentials.
- Implement session listing/revocation as appropriate, logout and expiry.
- Add CSRF protection for cookie-authenticated unsafe requests.

### 6.4 — Google OIDC
- Configure provider settings through environment variables.
- Implement authorization-code/OIDC callback, state/nonce validation, identity verification and safe account linking.
- Keep Google login disabled until real provider credentials and redirect URIs are configured by the repository owner.

### 6.5 — Password recovery
- Implement forgot-password request with uniform responses.
- Implement single-use, expiring reset token storage and reset flow.
- Revoke existing sessions after successful password reset.

### 6.6 — Roles, permissions and route protection
- Define CUSTOMER, STAFF and ADMIN role policy using the existing Phase 4 RBAC tables.
- Add granular staff permission checks and method-level authorization.
- Protect customer, staff and admin API route groups.
- Ensure role assignment and privileged changes cannot be self-escalated by customers.

### 6.7 — Verification and handoff
- Add unit and integration tests for registration, login, hashing, cookies, JWT validation, session rotation/revocation, logout, OAuth validation, password reset, role boundaries, staff permissions, CSRF, CORS and protected routes.
- Run Maven verification against MySQL 8.4 in CI.
- Document local setup, environment variables, API contracts and owner-run verification.
- Keep Customer Website and Admin Frontend source trees unchanged unless the owner explicitly authorizes frontend integration.

## Acceptance criteria

- [ ] Customer registration works and persists a correctly hashed password.
- [ ] Login authenticates valid credentials and rejects invalid or inactive accounts.
- [ ] Access JWT is validated and delivered only through the configured HttpOnly cookie.
- [ ] Server-side sessions/refresh credentials can be rotated, expired and revoked.
- [ ] Logout revokes the current session and clears cookies.
- [ ] Google OIDC flow validates state, nonce, issuer, audience and identity tokens.
- [ ] Forgot-password responses do not disclose account existence.
- [ ] Reset tokens are hashed, single-use and expire.
- [ ] CUSTOMER, STAFF and ADMIN role boundaries are enforced server-side.
- [ ] Staff permissions are enforced for protected operations.
- [ ] Unauthorized requests return 401; authenticated but forbidden requests return 403.
- [ ] CSRF and CORS protections are tested for cookie-authenticated requests.
- [ ] No secrets, signing keys, OAuth credentials or real reset tokens are committed.
- [ ] Maven tests and GitHub Actions pass on MySQL 8.4.
- [ ] Customer Website and Admin Frontend source trees remain unchanged.
- [ ] Owner confirms local setup and end-to-end verification.

## Database migration rules

- Treat Phase 4 V1/V2 as immutable.
- Add schema changes only through V3+ Flyway migrations.
- Do not drop or repair a persistent database blindly.
- Inspect current schema and Flyway history before applying any migration.
- Do not seed a default production administrator or commit credentials.

## Out of scope

- Customer/Admin frontend integration unless explicitly approved.
- Product, cart, inventory, checkout, order and payment workflows (later phases).
- Production OAuth enablement until the owner configures provider credentials and approved callback URLs.
