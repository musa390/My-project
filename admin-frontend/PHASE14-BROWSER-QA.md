# Phase 14 Admin Browser QA

Automated Chromium smoke in CI checks these routes: /login, /, /products, /categories, /brands, /collections, /orders, /customers, /inventory, /payments, /returns, /contact-messages, /coupons, /banners, /reviews, /newsletter, /analytics, /staff.

Viewports: 390×844 mobile, 768×1024 tablet, 1440×900 desktop.

The browser test fails for HTTP 404/5xx, uncaught page exceptions, or horizontal document overflow. Route smoke does not authenticate as staff and does not prove server-side authorization.

Manual critical-flow acceptance (requires owner-created staging staff accounts with least-privilege roles):
- [ ] Anonymous users cannot access protected admin/staff data.
- [ ] Customer-role account cannot access staff workspace.
- [ ] Verify least-privilege staff permissions for catalog, orders, inventory, and payments.
- [ ] Create/update/archive a test product and verify validation.
- [ ] Inspect an E2E test order and advance only permitted status transitions.
- [ ] Verify inventory adjustments and audit history.
- [ ] Review COD collection and manual refund reconciliation without real money movement.
- [ ] Verify returns, contact inbox, coupons, banners, reviews, newsletter, analytics, and staff management.
- [ ] Confirm no live email/provider action is triggered.
