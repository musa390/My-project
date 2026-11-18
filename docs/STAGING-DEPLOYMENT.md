# LUXE PERFUME — Staging deployment

This deployment is intentionally isolated from production. It uses the Phase 14 QA branch and an external MySQL-compatible staging database. Do not use production credentials or customer data.

## Topology

- Customer frontend: Vercel project rooted at `customer-frontend`.
- Admin frontend: separate Vercel project rooted at `admin-frontend`.
- Backend: Render Blueprint service from `render.yaml`, using `backend/Dockerfile`.
- Database: separate TiDB Cloud Starter cluster (MySQL protocol compatible), database/schema `luxe_perfume_staging`.

TiDB is MySQL-compatible, not the same server product/version as MySQL 8.4. Treat migration compatibility as unverified until the staging Flyway migrations and application integration checks pass. Do not use the staging database for production.

## Branch and deployment safety

- Deploy only branch `phase14/staging-deployment-setup`.
- Render Blueprint sets `autoDeploy: false`; review and manually deploy after environment variables are configured.
- Do not merge PR #11 or this staging setup PR as part of deployment setup.
- Keep all credentials in provider secret/environment settings. Never commit `.env`, passwords, JWT keys, database URLs, OAuth secrets, or API keys.

## 1. TiDB Cloud Starter

Create a Starter cluster and a dedicated database/schema named `luxe_perfume_staging`. Keep the free-tier spending controls at zero/disabled for paid overages where the provider offers that setting. Confirm the cluster region, network access policy, TLS requirement, and current free quota before creating it.

Create a database user limited to this staging schema. Require TLS. Copy the JDBC connection details into a secure password manager temporarily; do not paste them into chat or commit them.

The JDBC URL must use the provider's documented MySQL Connector/J TLS settings and the cluster's actual host/port/database. Do not guess TLS flags or disable certificate verification. Confirm the TiDB certificate/CA instructions for Java Connector/J before first connection.

## 2. Render backend

In Render Dashboard, create a Blueprint from this repository and select branch `phase14/staging-deployment-setup`. Review `render.yaml` before applying. The service is configured as a free Docker web service with automatic deploy disabled; free instances may sleep and are not production-grade.

Set these secrets/values in the Render service environment:
- `DATABASE_URL`: JDBC URL supplied by TiDB, including the documented TLS verification options.
- `MYSQL_USER`, `MYSQL_PASSWORD`: staging-only database user credentials.
- `JWT_SECRET_BASE64`: unique cryptographically random key encoded as Base64, at least 32 random bytes. Never reuse the development fallback.
- `CORS_ALLOWED_ORIGINS`: exact HTTPS origins of both deployed Vercel projects, comma-separated, no wildcard.
- `STOREFRONT_URL`: exact Customer Vercel HTTPS origin.
- `PASSWORD_RESET_FRONTEND_URL`: Customer origin plus `/reset-password`.
- `AUTH_OAUTH_SUCCESS_REDIRECT`: Customer account URL if OAuth is configured.

The staging Spring profile requires secrets rather than falling back to the development JWT key, enables secure cookies, disables Swagger/OpenAPI exposure, validates schema with Hibernate, and runs Flyway migrations. The health endpoint is `/actuator/health`.

Do not create public database ingress. Only the API needs database connectivity. If TiDB IP allowlisting is required, use the Render outbound IP guidance for the selected service/region; do not allow all addresses unless there is no safer supported option and the risk is explicitly reviewed.

## 3. Vercel Customer project

Create a Vercel project from the same repository and branch:
- Root Directory: `customer-frontend`
- Framework: Next.js
- Install command: `npm ci`
- Build command: `npm run build`
- Environment variable for Preview/Staging: `NEXT_PUBLIC_API_BASE_URL` = Render API HTTPS origin
- `NEXT_PUBLIC_SITE_URL` = Customer Vercel HTTPS origin
- Keep contact form mode as `demo` until a verified sender is intentionally configured.

## 4. Vercel Admin project

Create a second Vercel project from the same repository and branch:
- Root Directory: `admin-frontend`
- Framework: Next.js
- Install command: `npm ci`
- Build command: `npm run build`
- Environment variable for Preview/Staging: `NEXT_PUBLIC_API_BASE_URL` = Render API HTTPS origin

Keep Admin access protected by the backend's server-side authorization. A hidden frontend route is not an authorization control.

## 5. Deployment order

1. Create TiDB Starter cluster and staging schema.
2. Configure and deploy the Render API; confirm Flyway completes and health is UP.
3. Deploy Customer and Admin Vercel projects.
4. Update Render CORS origins with the exact Vercel HTTPS origins and redeploy.
5. Verify customer registration/login, COD checkout, order history/cancellation.
6. Verify Admin/staff authorization boundaries with dedicated test accounts.
7. Run a conservative staging load test and retain results.

## Acceptance limitations

- A successful deployment is not equivalent to authenticated E2E acceptance.
- TiDB compatibility must be proven by migrations and integration tests; if any MySQL 8.4-specific SQL is unsupported, use a genuine MySQL 8.4 service instead.
- Render free instances may sleep and have limited CPU/RAM; performance results on free tier are not production capacity evidence.
- Do not test real payment providers or use real money. Current project scope is COD plus manual refund/reconciliation.
- Owner must review the outstanding development-only picomatch advisory and the local Windows Maven verification status separately.
