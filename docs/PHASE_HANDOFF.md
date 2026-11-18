# Phase Handoff — LUXE PERFUME

## Current checkpoint
- Phases 0–4: Complete as recorded in `docs/PROJECT_STATUS.md`.
- Phases 5–9: See `docs/PROJECT_STATUS.md` for the exact CI and owner-local verification gates.
- Phase 10: IN PROGRESS. COD and manual refund foundation are verified at the previous CI checkpoint; bKash/Nagad sandbox integration remains blocked on official merchant specifications and sandbox acceptance. SSLCommerz is deferred until the final payment phase.
- **Current: Phase 11 — Advanced Business Logic & OOP implementation committed on `phase11/advanced-business-logic-oop`; draft PR #8 is open. Backend CI, Database Migration CI and owner-local clean-database verification are pending.**

## Sources of truth
- docs/PROJECT_MASTER_PLAN.md
- docs/PROJECT_STATUS.md
- docs/DATABASE_DESIGN.md
- docs/PHASE4-DATABASE-REPORT.md
- docs/PHASE5-BACKEND-FOUNDATION-PLAN.md
- docs/PHASE10-FINAL-STATUS-REPORT.md
- docs/PHASE11-ADVANCED-BUSINESS-LOGIC-OOP-PLAN.md
- docs/PHASE11-ADVANCED-BUSINESS-LOGIC-REPORT.md
- database/README.md

## Customer Website protection
customer-frontend/ is the approved baseline. Do not change Customer Website source, UI, routes, styles or behavior without explicit approval. Keep Admin Frontend source unchanged unless the task explicitly requires approved work there.

## Phase 4 delivered
- MySQL 8.4 target and existing Docker Compose service.
- 30-table normalized schema with 37 foreign keys and 38 CHECK constraints.
- ERD, data integrity rules and documented migration policy.
- V1 schema and V2 illustrative sample data migrations.
- SQL verification queries and MySQL 8.4 GitHub Actions CI.
- Database Migration CI run 44109253809 passed: MySQL service healthy, V1 applied, V2 applied and verification step successful.
- Local persistent database setup instructions: database/README.md.

## Phase 5 current work
- Working branch: phase5/backend-foundation.
- Java 21 + Spring Boot 4.1.2 + Maven application scaffold.
- MySQL environment configuration, JPA validation mode and Flyway discovery.
- Common response DTOs, global exception handling, validation mapping, health API, CORS and OpenAPI.
- Shared JPA BaseEntity, BaseRepository, CrudService and transactional AbstractCrudService.
- Integration tests cover API health, database-aware Actuator health, OpenAPI JSON and Flyway V1/V2.
- Backend Foundation CI run 44109910795 passed on commit f252915e050cd0a9e1f7434f21d7e98fc0973b04.
- MySQL 8.4 compatibility fix: generated columns involved in cascading foreign keys use VIRTUAL generated columns.
- Local verification is still required before Phase 5 is declared 100% complete.
- See backend/README.md for local PowerShell commands.
- Do not modify migrations already applied to a persistent database. If an existing local database has the previous V1 checksum, stop before running the updated branch and reconcile its Flyway history safely; do not drop the database.

## Phase 5 completion gate
1. JDK 21 and Maven installed locally.
2. Docker Engine running; MySQL 8.4 container healthy.
3. Maven clean verify passes locally using the current branch.
4. Flyway applies V1 and V2 to the intended local database, with any pre-existing migration history handled safely.
5. Backend starts and connects to MySQL.
6. Health, Actuator health, Swagger UI and OpenAPI endpoints work.
7. GitHub Actions backend CI passes — **PASS**.
8. Customer and Admin frontend source trees remain unchanged.
