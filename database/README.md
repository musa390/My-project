# LUXE PERFUME Database

Phase 4 targets MySQL 8.4, InnoDB and utf8mb4. Authoritative migrations:
- backend/src/main/resources/db/migration/V1__create_core_schema.sql
- backend/src/main/resources/db/migration/V2__insert_development_sample_data.sql

Flyway-compatible migrations are stored in backend resources. Phase 5 will add Flyway to Spring Boot; apply manually with MySQL CLI for Phase 4 verification.

## Start local MySQL
From repository root:
1. Copy .env.example to .env.
2. Set strong local-only MYSQL_PASSWORD and MYSQL_ROOT_PASSWORD values. Never commit .env.
3. Run docker compose up -d mysql.
4. Check docker compose ps until MySQL is healthy.

## Apply migrations (PowerShell, repository root)
~~~powershell
Get-Content -Raw .\backend\src\main\resources\db\migration\V1__create_core_schema.sql | docker compose exec -T mysql sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
Get-Content -Raw .\backend\src\main\resources\db\migration\V2__insert_development_sample_data.sql | docker compose exec -T mysql sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
~~~

## Verify
~~~powershell
Get-Content -Raw .\database\verification\phase4_verify.sql | docker compose exec -T mysql sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
~~~
Expected checks: 30 tables, 37 foreign keys, RBAC catalog, two demo products/variants and stock, sample coupon and newsletter row. Every result must be PASS. Stop and share the full error if any command fails.

## Safety
All seed rows are illustrative. No real user, admin credential or payment is seeded. Keep .env private and do not expose MySQL publicly. Never edit an already-applied migration; add V3+ instead.
