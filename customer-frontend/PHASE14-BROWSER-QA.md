# Phase 14 Customer Browser QA

Automated Chromium smoke in CI checks these routes: /, /shop, /login, /register, /forgot-password, /cart, /checkout, /my-orders, /track-order, /contact.

Viewports: 390×844 mobile, 768×1024 tablet, 1440×900 desktop.

The browser test fails for HTTP 404/5xx, uncaught page exceptions, or horizontal document overflow. This is a route/responsive smoke check, not a substitute for authenticated end-to-end order placement with a live backend.

Manual critical-flow acceptance (requires an authorized staging environment and seeded test account):
- [ ] Register and log in as a test customer.
- [ ] Browse/search catalog and open a product/variant.
- [ ] Add/remove cart and wishlist items.
- [ ] Add/edit/select a shipping address.
- [ ] Obtain checkout quote and place a COD test order.
- [ ] Verify order history, tracking, and permitted cancellation.
- [ ] Submit contact/newsletter test records and verify the resulting admin records.
- [ ] Confirm no real payment is initiated and no live email is sent.
