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
| 4 | MySQL database design and ERD | In progress — runtime verification pending |
| 5 | Spring Boot backend foundation | Pending |
| 6 | Authentication and authorization | Pending |
| 7 | Product and catalog backend | Pending |
| 8 | Cart, wishlist and inventory | Pending |
| 9 | Checkout and order management | Pending |
| 10 | Sandbox payment integrations and COD | Pending |
| 11 | Domain/business logic and Java OOP | Pending |
| 12 | Approved third-party integrations | Pending |
| 13 | Frontend/backend integration | Pending; approval required before modifying protected Customer Website source |
| 14 | Testing, security and QA | Pending |
| 15 | Deployment and GitHub release | Pending |

## Phase 2 completion
Owner confirmed verification script, typecheck, lint, production build and browser QA passed. See docs/PHASE2-CUSTOMER-FRONTEND-REPORT.md.

## Phase 3 completion
Admin Frontend modules and demo interactions are complete. Owner-provided typecheck, lint and production build passed; GitHub Actions run 44090886836 succeeded; owner confirmed manual browser QA. See docs/PHASE3-ADMIN-FRONTEND-REPORT.md.

## Phase 4 database design
A normalized MySQL 8.4 schema, ERD, 30-table V1 migration (37 foreign keys), V2 demo seed migration, Docker setup instructions and verification SQL are committed. Runtime database application and verification are still required before sign-off. See docs/DATABASE_DESIGN.md and database/README.md.

## Core architecture
- Separate Customer and Admin Next.js applications.
- Backend: Java, Spring Boot, Maven, Spring Security, Spring Data JPA, Hibernate.
- Database: MySQL; Flyway migrations under backend resources.
- API: REST + JSON with OpenAPI contracts.
- Payments: COD and sandbox/demo only until production approval; verify server-side.
- Auth: secure server-managed cookies/session or explicitly designed JWT cookie; never LocalStorage tokens.
- Secrets: environment variables; never commit .env.

## Completion rule
A phase is complete only after deliverables, relevant automated checks, manual checks and documentation are verified. Never report planned work as completed.
