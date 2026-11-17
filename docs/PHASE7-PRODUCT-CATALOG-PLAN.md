# LUXE PERFUME — Phase 7 Product & Catalog Backend Plan

## Goal
Deliver a secure, database-backed catalog API so authorized Admin/Staff users can manage catalog data and the Customer Website can read the same persisted products and merchandising data. This phase is backend-first: do not change Customer Frontend or Admin Frontend source files.

## Starting point and database contract
- Base branch: `phase6/authentication-authorization`.
- Existing schema is authoritative: MySQL 8.4, InnoDB, utf8mb4.
- Existing V1 tables already cover `products`, `categories`, `brands`, `collections`, `collection_products`, `product_variants`, `product_images`, `inventory`, `order_items`, and `customer_orders`.
- V1/V2/V3 migrations are immutable. Add a V4+ migration only if an actual schema gap is demonstrated; do not duplicate existing columns/tables.
- Product prices and sellable SKUs belong to variants. Stock is variant-level.
- Product delete should be a soft archive/status transition; do not hard-delete historical catalog records.

## API design

### Public/customer read API
- `GET /api/v1/catalog/products` — published products with search, filters, sorting, pagination.
- `GET /api/v1/catalog/products/{slug}` — product detail including category, brand, images, variants and availability.
- `GET /api/v1/catalog/categories`
- `GET /api/v1/catalog/categories/{slug}`
- `GET /api/v1/catalog/brands`
- `GET /api/v1/catalog/collections`
- `GET /api/v1/catalog/collections/{slug}`
- `GET /api/v1/catalog/featured`
- `GET /api/v1/catalog/best-sellers`
- `GET /api/v1/catalog/new-arrivals`

### Authorized management API
All writes require authenticated staff/admin permissions; enforce authorization server-side.
- `/api/v1/staff/catalog/products` — create, update, archive, get/list.
- `/api/v1/staff/catalog/categories` — CRUD.
- `/api/v1/staff/catalog/brands` — CRUD.
- `/api/v1/staff/catalog/collections` — CRUD and product membership.
- Product subresources: `/{productId}/variants`, `/{productId}/images`.
- Image upload endpoint uses Cloudinary only when configured; never store provider secrets in source or return them to clients.

## Product behavior
- Validate name, slug, category, brand, product status/type, SKU, non-negative prices, compare-at price, volume, weight, and image metadata.
- Enforce unique slugs/SKUs and one default variant/image using DB constraints plus friendly API errors.
- Product detail includes all active variants, ordered images, primary image, and availability computed from inventory.
- Availability is true only when an active sellable variant has available quantity `quantity_on_hand - quantity_reserved > 0`.
- Featured uses `products.is_featured`.
- Best sellers are derived from non-cancelled/non-returned order lines; no manually invented sales flag.
- New arrivals are derived from product creation timestamp and a documented configurable window.
- Search/filter/sort/pagination must use parameterized SQL and allowlisted sort fields; never interpolate arbitrary client SQL.
- Customer endpoints expose ACTIVE products only. Draft/archived products remain staff-only.

## Cloudinary
- Optional integration configured with environment variables `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.
- Without complete configuration, upload endpoint returns a clear service-unavailable response; URL-based image metadata management can still be tested.
- Upload validation: image MIME allowlist, size limit, generated public ID/folder, server-side credentials only.
- Store secure Cloudinary URL and public ID; never expose API secret.

## Security
- Customer read endpoints are public and expose only ACTIVE catalog data.
- Staff mutations require the existing products/catalog write permissions.
- Product, category, brand and collection writes are audited via standard timestamps; no client-supplied IDs or ownership claims.
- DTOs only; do not serialize JDBC rows/entities directly.
- Consistent validation/error responses; prevent SQL injection and over-broad pagination.

## Test strategy
- MySQL 8.4 integration tests against the real Flyway V1–V3 schema.
- CRUD tests for product/category/brand/collection.
- Variant/image relationship and validation tests.
- Customer cannot read DRAFT/ARCHIVED products.
- Anonymous user cannot call staff write endpoints; customer cannot mutate catalog; authorized staff permission can.
- Search/filter/sort/pagination and availability tests.
- Featured/best-seller/new-arrival tests.
- Cloudinary configured/unconfigured behavior tests without committing credentials.
- CI must run all backend tests and database migration verification.

## Completion criteria
1. All API requirements implemented and documented in OpenAPI.
2. Admin/Staff catalog writes persist in MySQL and are visible from customer read APIs.
3. Authorization and validation tests pass.
4. Maven build and GitHub Actions pass against MySQL 8.4.
5. No Customer Frontend or Admin Frontend source changes.
6. Local owner verification is recorded separately; do not claim 100% without it.
