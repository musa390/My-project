# Phase 3 — Admin Frontend Development Plan

**Status:** In progress  
**Application:** `admin-frontend/`  
**Protection:** Do not modify `customer-frontend/` during Phase 3 without explicit approval.

## Objective
Build a separate responsive administration application. Until backend/database phases are complete, all metrics and records must be clearly labelled as mock/demo data.

## Stack
Next.js 15.5.29 App Router, React 19.1, TypeScript 5, Tailwind CSS 4, lucide-react.

## Milestones

### 3.1 — Foundation
- [x] Separate Admin app package/config scaffold
- [x] Global dark luxury theme foundation
- [x] Shared sidebar/navigation shell
- [x] Dashboard overview with demo-only metrics
- [x] Disabled demo-only sign-in placeholder
- [ ] Install dependencies and verify typecheck/lint/build
- [ ] Browser responsive/accessibility QA

### 3.2 — Core management
- [ ] Products and product detail/create/edit UI
- [ ] Categories and collections
- [ ] Orders and order details
- [ ] Customers and customer profiles
- [ ] Inventory and stock

### 3.3 — Business management
- [ ] Promotions/coupons
- [ ] Reviews moderation
- [ ] Reports/analytics
- [ ] Admin profile and settings

### 3.4 — Final QA
- [ ] Loading, empty, error and confirmation states
- [ ] Form validation and accessible focus states
- [ ] Mobile/tablet/desktop layouts
- [ ] Navigation and interaction checks
- [ ] Typecheck, lint, production build and browser QA
- [ ] Final report and project status update

## Current implementation
Initial shell and dashboard are scaffolded. The dashboard values are illustrative; sign-in is disabled. No API, database, authentication or live store operations are connected.
