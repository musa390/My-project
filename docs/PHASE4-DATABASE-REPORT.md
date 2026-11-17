# Phase 4 — Database Design Final Report

**Status:** Complete — 100%  
**Completion date:** 2026-11-17  
**Database target:** MySQL 8.4, InnoDB, utf8mb4  
**Database name:** luxe_perfume

## Delivered
- Normalized relational schema with 30 tables.
- 37 foreign-key relationships, 38 CHECK constraints, indexes, unique keys and generated-column uniqueness guards.
- ER diagram and documented entity catalogue, cardinalities, deletion behavior, data rules and migration policy.
- V1 core schema migration and V2 development sample-data migration in Flyway-compatible locations.
- RBAC roles/permissions catalog without creating a default admin account or password.
- Development sample data for brand, categories, collection, products, product images, sellable variants, inventory balances/movements, coupon, banner and newsletter subscriber.
- Docker Compose MySQL setup instructions and PowerShell apply/verification commands.
- SQL verification queries and a GitHub Actions MySQL 8.4 migration CI workflow.

## Schema coverage
- Identity and access: users, roles, permissions, user_roles, role_permissions.
- Customer address book: addresses.
- Catalogue: products, categories, brands, collections, collection_products.
- Product media and variants: product_images, product_variants.
- Shopping: carts, cart_items, wishlists, wishlist_items.
- Orders: customer_orders, order_items, order_status_history.
- Payments: payments.
- Inventory: inventory, inventory_movements.
- Promotions: coupons, coupon_products, coupon_categories, coupon_redemptions.
- Engagement: reviews, banners, newsletter_subscribers.

## Verification evidence
GitHub Actions workflow [Database Migration CI run 44094846967](https://github.com/musa390/My-project/actions/runs/44094846967) completed with **SUCCESS** on commit `e526a527e7e2673d9715ec2f756292c7e14b98c8`.

The workflow provisions a disposable MySQL 8.4 service, applies V1 and V2 migrations, then runs `database/verification/phase4_verify.sql`. All workflow steps completed successfully:
- MySQL service initialized and became healthy.
- V1 schema migration: PASS.
- V2 sample-data migration: PASS.
- Schema, foreign-key, RBAC, sample product/variant/inventory, coupon, newsletter and orphan-reference verification step: PASS.

## Important environment distinction
The CI database is temporary and is destroyed after the workflow. The repository includes `docker-compose.yml` and `database/README.md` so the owner can create a persistent local development database. Local startup commands are provided; no claim is made that a MySQL instance is currently running on the owner's computer.

## Safety and boundaries
- Seed content is illustrative only.
- No real customer information, admin login/password, API secret or payment credential is included.
- Payment processing, authentication services, backend APIs and transactional business workflows are later phases.
- Customer Website and Admin Frontend source code were not modified during Phase 4.

## Sign-off
Database schema, ERD, migrations, sample data, relationship constraints, setup documentation and MySQL 8.4 CI verification are complete. **Phase 4 is marked 100% Complete.**
