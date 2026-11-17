# LUXE PERFUME — Database Design (Phase 4)

Target: MySQL 8.4, InnoDB, utf8mb4. Database: luxe_perfume.
Migration authority: backend/src/main/resources/db/migration/ (Flyway-compatible naming).

## Design principles
- Normalized relational model; junction tables for many-to-many relationships.
- BIGINT UNSIGNED surrogate IDs; composite primary keys for pure junctions.
- Money uses DECIMAL(12,2), never FLOAT. Currency defaults to BDT.
- UTC timestamps; configure application and database session timezone consistently.
- Product variants are sellable SKUs; price and stock belong to the variant.
- Order item and shipping fields are snapshots, preserving order history if catalogue or address changes.
- Payment attempts are separate from orders; provider references are unique per provider.
- Inventory is variant-level, with a stock movement ledger. Update balance and ledger in one backend transaction.
- RBAC uses roles, permissions, user_roles and role_permissions. Spring Security must enforce permissions server-side.
- No password, real customer details, admin account or live payment credential is seeded.

## Entity catalogue
| Domain | Tables | Purpose |
|---|---|---|
| Identity | users, roles, permissions, user_roles, role_permissions | Accounts and RBAC |
| Address | addresses | Saved delivery addresses |
| Catalogue | products, categories, brands, collections, collection_products | Product taxonomy and merchandising |
| Product detail | product_images, product_variants | Gallery and sellable variants |
| Shopping | carts, cart_items, wishlists, wishlist_items | Cart and saved products |
| Orders | customer_orders, order_items | Order totals and immutable item snapshots |
| Payments | payments | Multiple payment attempts per order |
| Stock | inventory, inventory_movements | Variant stock and audit ledger |
| Promotions | coupons, coupon_products, coupon_categories, coupon_redemptions | Eligibility and redemption |
| Engagement | reviews, banners, newsletter_subscribers | Moderation, home content and consent |

## ER relationship map
~~~mermaid
erDiagram
 USERS ||--o{ USER_ROLES : assigned
 ROLES ||--o{ USER_ROLES : grants
 ROLES ||--o{ ROLE_PERMISSIONS : contains
 PERMISSIONS ||--o{ ROLE_PERMISSIONS : defines
 USERS ||--o{ ADDRESSES : saves
 USERS ||--o{ CARTS : owns
 CARTS ||--o{ CART_ITEMS : contains
 PRODUCT_VARIANTS ||--o{ CART_ITEMS : selected
 USERS ||--o{ WISHLISTS : owns
 WISHLISTS ||--o{ WISHLIST_ITEMS : contains
 PRODUCTS ||--o{ WISHLIST_ITEMS : saved
 CATEGORIES ||--o{ PRODUCTS : classifies
 CATEGORIES o|--o{ CATEGORIES : parent_of
 BRANDS o|--o{ PRODUCTS : brands
 PRODUCTS ||--o{ PRODUCT_IMAGES : displays
 PRODUCTS ||--|{ PRODUCT_VARIANTS : offers
 COLLECTIONS ||--o{ COLLECTION_PRODUCTS : groups
 PRODUCTS ||--o{ COLLECTION_PRODUCTS : included
 PRODUCT_VARIANTS ||--|| INVENTORY : stock
 PRODUCT_VARIANTS ||--o{ INVENTORY_MOVEMENTS : ledger
 USERS o|--o{ CUSTOMER_ORDERS : places
 CUSTOMER_ORDERS ||--|{ ORDER_ITEMS : contains
 PRODUCT_VARIANTS o|--o{ ORDER_ITEMS : snapshot_source
 CUSTOMER_ORDERS ||--o{ PAYMENTS : payment_attempts
 COUPONS ||--o{ COUPON_REDEMPTIONS : redeemed
 CUSTOMER_ORDERS ||--o| COUPON_REDEMPTIONS : applies
 USERS o|--o{ COUPON_REDEMPTIONS : redeems
 COUPONS ||--o{ COUPON_PRODUCTS : targets
 PRODUCTS ||--o{ COUPON_PRODUCTS : eligible
 COUPONS ||--o{ COUPON_CATEGORIES : targets
 CATEGORIES ||--o{ COUPON_CATEGORIES : eligible
 PRODUCTS ||--o{ REVIEWS : receives
 USERS ||--o{ REVIEWS : writes
 ORDER_ITEMS o|--o| REVIEWS : may_verify
~~~

## Integrity rules
- Unique email, role/permission code, slug, SKU, coupon code, order number and newsletter email.
- Products require a category and may optionally reference a brand. Products have images and variants.
- Cart item quantity > 0; one variant appears once per cart. Wishlist composite key prevents duplicates.
- Order grand total = subtotal - discount + shipping + tax; order line total = unit price * quantity - line discount.
- Order item snapshots preserve name, variant, SKU and price; user FK may become NULL without deleting order history.
- Payment callback must be verified server-side; never trust browser redirect to mark paid.
- Reserved stock cannot exceed on-hand stock. All stock adjustments append inventory_movements transactionally.
- Reviews are 1–5 and unique per user/product; moderation status is explicit. Backend must verify that an optional order item belongs to the same user and product before treating a review as verified.
- Coupon percentage <= 100; date ranges and monetary constraints are checked.
- Archive products/variants instead of deleting referenced historical records.

## Migration set
1. V1__create_core_schema.sql — 29 relational tables, indexes, 35 foreign keys/check constraints and initial RBAC permission catalog. No account is created.
2. V2__insert_development_sample_data.sql — demo brand, categories, products, variants, images, inventory, collection, coupon, banner and newsletter sample.

Once applied in a shared database, migrations are immutable. Add V3+ for future changes. Flyway Maven dependency and Spring Boot startup integration are Phase 5.

## Local setup and validation
1. Copy .env.example to .env and set strong local-only MySQL passwords; never commit .env.
2. Run docker compose up -d mysql from repository root; inspect docker compose ps.
3. Apply V1 then V2 using the PowerShell commands in database/README.md.
4. Run database/verification/phase4_verify.sql and check all results are PASS.
5. Do not expose MySQL port 3306 publicly. No production data is included.

## Explicit exclusions
No backend API, auth service, payment processor, production data, default admin login, or SQL triggers for business workflows are included. Implement business workflows transactionally in Spring Boot.
