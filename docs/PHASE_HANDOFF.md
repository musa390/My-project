# Phase Handoff — LUXE PERFUME

## Current checkpoint
- Phase 0: Complete.
- Phase 1: Complete — 100%.
- Phase 2: Complete — 100%.
- Phase 3: Complete — 100%.
- Phase 4: In progress — schema/migrations committed; local MySQL migration and verification pending.
- Next after Phase 4 sign-off: Phase 5 — Spring Boot Backend Foundation.

## Sources of truth
- docs/PROJECT_MASTER_PLAN.md
- docs/PROJECT_STATUS.md
- docs/DATABASE_DESIGN.md
- database/README.md
- docs/PHASE3-ADMIN-FRONTEND-REPORT.md

## Customer Website protection
customer-frontend/ is the approved baseline. Do not change Customer Website source, UI, routes, styles or behavior without explicit approval. Phase 4 did not change customer-frontend or admin-frontend source.

## Phase 4 deliverables
- MySQL 8.4 target and existing Docker Compose service.
- 30-table normalized relational schema in backend/src/main/resources/db/migration/V1__create_core_schema.sql.
- RBAC permission catalog seeded by V1; no user/admin credential seeded.
- Illustrative data in V2__insert_development_sample_data.sql.
- ERD and design constraints in docs/DATABASE_DESIGN.md.
- Apply/verify commands in database/README.md and SQL checks in database/verification/phase4_verify.sql.

## Phase 4 remaining acceptance step
1. In repository root, copy .env.example to .env and set local-only MySQL passwords.
2. Start Docker Desktop, then run docker compose up -d mysql.
3. Confirm docker compose ps reports the mysql service healthy.
4. Apply V1 and V2 using the commands in database/README.md.
5. Run database/verification/phase4_verify.sql.
6. Share the complete output. Every check must PASS; resolve any failure before Phase 4 sign-off.

Do not expose port 3306 publicly. Do not commit .env. Seed data is demo-only; no live payments or real accounts exist.
