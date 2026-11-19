# Changelog

## 2026-11-19 — Deployment preparation and quality gate
- Added official-contract SSLCommerz and bKash sandbox gateway adapters.
- Added SSLCommerz/bKash sandbox environment configuration placeholders.
- Resolved the final customer-order test merge brace conflict and retained the latest implementation.
- Added `test` scripts for both Next.js applications, combining static verification, typecheck, lint and production build.
- Customer and Admin npm test gates pass with zero npm audit vulnerabilities.
- Spring Boot backend package compiles successfully with `mvn -DskipTests package`.
- Created GitHub deployment branch `deployment-complete` and pull request `#13`.
- Render staging service was found; latest deployment failed with TiDB SQL 1045 access denied.
- Updated deployment branch, frontend build fixes and Render blueprint target.

## 2026-11-17 — Phase 5 started
- Created the phase5/backend-foundation working branch.
- Added Java 21 / Spring Boot 4.1.2 / Maven backend foundation.
- Configured MySQL, JPA validation mode and Flyway discovery for existing V1/V2 migrations.
- Added shared API response DTOs, centralized exception handling, health endpoint, configurable CORS and Swagger/OpenAPI.
- Added PowerShell environment loader, integration tests and MySQL 8.4 backend CI.
- Updated Phase 5 plan, status and handoff documentation.
- Local build and database verification remain pending; Phase 5 is not complete.
- Customer Website and Admin Frontend source trees were not changed.

## 2026-11-17 — Phase 4 completed (100%)
- Finalized normalized MySQL 8.4 schema: 30 tables, 37 foreign-key relationships and 38 CHECK constraints.
- Added ERD, schema documentation, Flyway-compatible V1/V2 migrations and illustrative sample data.
- Added MySQL verification SQL and GitHub Actions database migration CI.
- GitHub Actions run 44094846967 succeeded: MySQL service healthy, V1 and V2 applied, verification step passed.
- Added Phase 4 final report and updated project status, master plan and handoff.
- Customer and Admin frontend source files were not changed.

## 2026-11-17 — Phase 3 completed (100%)
- Owner-provided local Admin typecheck, lint and production build passed.
- GitHub Actions Phase 3 workflow run 44090886836 succeeded.
- Project owner confirmed manual browser QA completed.
- Added Phase 3 final report and updated project documentation.
- Admin records/interactions remain demo-only and in-memory.

## 2026-11-17 — Phase 2 completed
- Added demo Customer Login/Register UI; Google sign-in remains disabled pending secure OAuth configuration.
- Owner confirmed verification script, typecheck, lint, production build and browser QA passed.
- Real auth, database, live data and payment remain out of scope.

## 2026-11-17 — Phase 1 verified
- Repository musa390/My-project and initial project upload commit verified: 590f26b405c9d9f0ea27212eb57619822662bc39.
- Original root README preserved.
