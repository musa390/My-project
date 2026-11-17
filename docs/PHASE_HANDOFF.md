# Phase Handoff — LUXE PERFUME

## Current checkpoint
- Phase 0: Complete.
- Phase 1: Complete — 100%.
- Phase 2: Complete — 100%.
- Phase 3: Complete — 100%.
- Phase 4: Complete — 100%; MySQL 8.4 migrations, seed data and verification CI passed.
- **Current: Phase 5 — Spring Boot Backend Foundation (In Progress).**

## Sources of truth
- docs/PROJECT_MASTER_PLAN.md
- docs/PROJECT_STATUS.md
- docs/DATABASE_DESIGN.md
- docs/PHASE4-DATABASE-REPORT.md
- docs/PHASE5-BACKEND-FOUNDATION-PLAN.md
- database/README.md

## Customer Website protection
customer-frontend/ is the approved baseline. Do not change Customer Website source, UI, routes, styles or behavior without explicit approval. Keep Admin Frontend source unchanged unless the task explicitly requires approved work there.

## Phase 4 delivered
- MySQL 8.4 target and existing Docker Compose service.
- 30-table normalized schema with 37 foreign keys and 38 CHECK constraints.
- ERD, data integrity rules and documented migration policy.
- V1 schema and V2 illustrative sample data migrations.
- SQL verification queries and MySQL 8.4 GitHub Actions CI.
- CI run 44094846967 passed: MySQL service healthy, V1 applied, V2 applied and verification step successful.
- Local persistent database setup instructions: database/README.md.

## Phase 5 current work
- Working branch: phase5/backend-foundation.
- Java 21 + Spring Boot 4.1.2 + Maven application scaffold.
- MySQL environment configuration, JPA validation mode and Flyway discovery.
- Common response DTOs, global exception handling, health API, CORS and OpenAPI.
- Integration tests, environment loader script and MySQL-backed CI.
- See backend/README.md for local PowerShell commands.
- Do not edit applied V1/V2 migrations; future changes start at V3.
- Local build and database verification must pass before Phase 5 can be marked complete.

## Phase 5 completion gate
1. JDK 21 and Maven installed.
2. Docker Engine running; MySQL 8.4 container healthy.
3. Maven clean verify passes locally.
4. Flyway applies V1 and V2 to the local database.
5. Backend starts and connects to MySQL.
6. Health, Actuator health, Swagger UI and OpenAPI endpoints work.
7. GitHub Actions backend CI passes.
8. Customer and Admin frontend source trees remain unchanged.
