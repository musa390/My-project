# LUXE PERFUME — Master Project Plan

## Goal
Build a full-stack perfume e-commerce platform while preserving the existing Customer Website as the approved visual and functional baseline.

## Phase roadmap
| Phase | Scope | Status |
|---|---|---|
| 0 | Project audit and planning | Complete |
| 1 | Project setup and GitHub | Complete — 100% |
| 2 | Customer Frontend finalization | Complete — 100% |
| 3 | Separate Admin Frontend | In progress |
| 4 | MySQL database design and ERD | Pending |
| 5 | Spring Boot backend foundation | Pending |
| 6 | Authentication and authorization | Pending |
| 7 | Product and catalog backend | Pending |
| 8 | Cart, wishlist and inventory | Pending |
| 9 | Checkout and order management | Pending |
| 10 | Sandbox payment integrations and COD | Pending |
| 11 | Domain/business logic and Java OOP | Pending |
| 12 | Approved third-party integrations | Pending |
| 13 | Frontend/backend integration | Pending; obtain approval before modifying protected Customer Website source |
| 14 | Testing, security and QA | Pending |
| 15 | Deployment and GitHub release | Pending |

## Phase 2 completion
Owner confirmed verification script, typecheck, lint, production build and browser QA passed. See `docs/PHASE2-CUSTOMER-FRONTEND-REPORT.md`. Authentication, OAuth, persistent data and live payments remain out of scope.

## Phase 3 scope
Build the separate `admin-frontend/` only. Initial scaffold and dashboard shell are committed. Complete management screens, interactions, responsive/accessibility states and QA using labelled demo data until API/database integration. Do not modify `customer-frontend/` without explicit approval.

## Core architecture
- Separate Customer and Admin Next.js applications.
- Backend: Java, Spring Boot, Maven, Spring Security, Spring Data JPA, Hibernate.
- Database: MySQL; Flyway migrations under backend resources.
- API: REST + JSON with OpenAPI contracts.
- Payments: COD and sandbox/demo only until production approval; verify server-side.
- Auth: secure server-managed cookies/session or explicitly designed JWT cookie; never LocalStorage tokens.
- Secrets: environment variables; never commit `.env`.

## Completion rule
A phase is complete only after deliverables, relevant automated checks, manual checks and documentation are verified. Never report planned work as completed.
