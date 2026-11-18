# LUXE PERFUME — Master Project Plan

## Goal
Build a full-stack perfume e-commerce platform while preserving the existing Customer Website as the approved visual and functional baseline.

## Phase roadmap
| Phase | Scope | Status |
|---|---|---|
| 0 | Project audit and planning | Complete |
| 1 | Project setup and GitHub | Complete — 100% |
| 2 | Customer Frontend finalization | Complete — 100% |
| 3 | Separate Admin Frontend | Complete — 100% |
| 4 | MySQL database design and ERD | **Complete — 100%** |
| 5 | Spring Boot backend foundation | **Implementation + CI verified; local owner verification pending** |
| 6 | Authentication and authorization | Pending |
| 7 | Product and catalog backend | Pending |
| 8 | Cart, wishlist and inventory | Pending |
| 9 | Checkout and order management | Pending |
| 10 | Sandbox payment integrations and COD | IN PROGRESS — COD/refund foundation done; bKash/Nagad sandbox pending; SSLCommerz deferred |
| 11 | Domain/business logic and Java OOP | Implementation committed; CI and owner-local verification pending |
| 12 | Approved third-party integrations | Pending |
| 13 | Frontend/backend integration | Pending; approval required before modifying protected Customer Website source |
| 14 | Testing, security and QA | Pending |
| 15 | Deployment and GitHub release | Pending |

## Phase 2 completion
Owner confirmed verification script, typecheck, lint, production build and browser QA passed. See docs/PHASE2-CUSTOMER-FRONTEND-REPORT.md.

## Phase 3 completion
Admin Frontend modules and demo interactions are complete. Owner-provided typecheck, lint and production build passed; GitHub Actions run 44090886836 succeeded; owner confirmed manual browser QA. See docs/PHASE3-ADMIN-FRONTEND-REPORT.md.

## Phase 4 completion
A normalized MySQL 8.4 schema, ERD, 30-table V1 migration, V2 demo seed migration, 37 foreign keys, 38 CHECK constraints, Docker setup, verification SQL and MySQL CI are complete. GitHub Actions run 44094846967 succeeded after applying both migrations to a disposable MySQL 8.4 database and running the verification step. See docs/PHASE4-DATABASE-REPORT.md. Local persistent setup instructions are in database/README.md.

## Phase 5 foundation
Working branch: phase5/backend-foundation. Java 21, Spring Boot 4.1.2, Maven, MySQL/Flyway configuration, shared API response/error handling, reusable BaseRepository/CrudService foundation, health endpoint, CORS, OpenAPI/Swagger and integration tests are implemented. Backend CI run 44109910795 passed on code commit f252915e050cd0a9e1f7434f21d7e98fc0973b04, including four integration tests against MySQL 8.4. The schema uses VIRTUAL generated columns where cascading foreign keys require them for MySQL 8.4 compatibility. Local MySQL connectivity, local Maven verification and endpoint checks remain owner-confirmation gates. See docs/PHASE5-BACKEND-FOUNDATION-PLAN.md.

## Phase 10/11 implementation boundary\n- Phase 10 remains IN PROGRESS until bKash/Nagad official sandbox adapters, callbacks, server-side verification and sandbox acceptance are complete. SSLCommerz is deferred until the final payment phase.\n- Phase 11 is developed on `phase11/advanced-business-logic-oop` with draft PR #8. It adds inventory/product invariants, discount/shipping strategies, in-app notifications, full-order return review/receipt, refund amount policy and V7/V8 migrations. CI and owner-local verification are still required before 100% completion.\n\n## Core architecture
- Separate Customer and Admin Next.js applications.
- Backend: Java, Spring Boot, Maven, Spring Security, Spring Data JPA, Hibernate.
- Database: MySQL; Flyway migrations under backend resources.
- API: REST + JSON with OpenAPI contracts.
- Payments: COD and sandbox/demo only until production approval; verify server-side.
- Auth: secure server-managed cookies/session or explicitly designed JWT cookie; never LocalStorage tokens.
- Secrets: environment variables; never commit .env.

## Completion rule
A phase is complete only after deliverables, relevant automated checks, manual checks and documentation are verified. Never report planned work as completed.
