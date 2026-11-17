# LUXE PERFUME — Phase 5 Browser QA Checklist

Run `npm run dev` after the production build succeeds, then open `http://localhost:3000` in Chrome or Edge.

Test at these viewport widths using DevTools responsive mode:
- Mobile: 390 × 844
- Tablet: 768 × 1024
- Desktop: 1440 × 900

## Home (`/`)
- [ ] Announcement bar and navigation render without overlap.
- [ ] Hero image, heading and both CTA/navigation actions work.
- [ ] Floral, Woody, Amber and Citrus links navigate to the intended results.
- [ ] Signature, Lumière, Oud and Fresh collection tiles navigate correctly.
- [ ] Best Sellers, New Arrivals and Trending sections render product cards.
- [ ] Product card actions (quick view, wishlist, add to cart) work where available.
- [ ] Promotional/campaign sections render without clipped text or missing media.
- [ ] Testimonial placeholders are not presented as verified customer reviews.
- [ ] Newsletter shows demo feedback and does not claim to save/send a subscription.
- [ ] Footer links work.

## About (`/about`)
- [ ] Story, mission, vision and values are visible and readable.
- [ ] Image placeholder renders; if configured as video, controls work.
- [ ] Timeline, craftsmanship and sustainability sections render.
- [ ] No text or media overflows at mobile/tablet widths.

## Contact (`/contact`)
- [ ] Required fields reject empty submission.
- [ ] Invalid email is rejected.
- [ ] Too-short name/message is rejected.
- [ ] Demo mode shows clear feedback that nothing is emailed or stored.
- [ ] Blank optional phone/email/social/map settings do not create broken links or blank embeds.
- [ ] Configured WhatsApp, email, social and map links open the expected destinations.

## Shared responsive behavior
- [ ] Mobile menu opens and closes using mouse/touch and keyboard.
- [ ] No horizontal page scrolling at any of the three widths.
- [ ] Buttons and links have visible focus states.
- [ ] Images load and preserve aspect ratio; no broken-image icons.
- [ ] Browser console has no uncaught errors or hydration warnings.

## Record results
For every failed item, record the route, viewport, exact action, expected result, actual result, and a screenshot. Do not mark Browser QA PASS until all checks above are completed in a real browser.
