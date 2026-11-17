# LUXE PERFUME — Master Project Plan

## Goal

Build a complete full-stack perfume e-commerce platform while preserving the existing Customer Website as the approved visual and functional baseline.

## Phase roadmap

| Phase | Scope | Status |
|---|---|---|
| 0 | Project audit and planning | Complete |
| 1 | Root setup, separate app/service folders, documentation and GitHub workflow | Complete |
| 2 | Customer frontend finalization and verification | Complete — 100% |
| 3 | Separate Admin frontend | Ready to start |
| 4 | MySQL database design and ERD | Pending |
| 5 | Spring Boot backend foundation | Pending |
| 6 | Authentication and authorization | Pending |
| 7 | Product and catalog backend | Pending |
| 8 | Cart, wishlist and inventory | Pending |
| 9 | Checkout and order management | Pending |
| 10 | Payment gateways in sandbox/demo plus COD | Pending |
| 11 | Domain/business logic and Java OOP | Pending |
| 12 | Google, Cloudinary, Resend and other approved integrations | Pending |
| 13 | Customer/Admin frontend and backend integration | Pending; obtain approval before modifying protected Customer Website source |
| 14 | Testing, security and QA | Pending |
| 15 | Deployment and GitHub release | Pending |

## Phase 2 completion note

The Customer Frontend Phase 2 scope is complete. The project owner confirmed the static verification script, TypeScript typecheck, lint, production build and browser QA all passed. See `docs/PHASE2-CUSTOMER-FRONTEND-REPORT.md` for the scope, evidence qualification and remaining boundaries. Login/Register and Google sign-in remain demo UI; backend authentication, persistent data and live payments are not included in Phase 2.

## Core decisions

- Customer and Admin are separate Next.js applications.
- Backend: Java, Spring Boot, Maven, Spring Security, Spring Data JPA and Hibernate.
- Database: MySQL; Flyway migrations are authoritative inside the backend project.
- API: REST + JSON with OpenAPI contract documentation in `api/openapi/`.
- Payment: COD plus provider sandbox/demo environments only until production use is explicitly approved. Verify payment status server-side.
- Authentication: secure server-managed cookies/session or a carefully specified JWT cookie design; never store authentication tokens in LocalStorage.
- Secrets: environment variables only; `.env` is ignored by Git.

## Phase 3 protection rule

Phase 3 develops only the separate `admin-frontend/` application and its documentation. Do not modify `customer-frontend/` without explicit approval.

## Completion rule

A phase is complete only when its deliverables exist, relevant automated checks pass, manual checks are recorded where required, and `docs/PROJECT_STATUS.md` plus `docs/CHANGELOG.md` are updated. Do not report planned work as completed.
