# Backend — Spring Boot (Phase 5)

Backend implementation is not started. Phase 4 adds Flyway-compatible SQL migrations:
- src/main/resources/db/migration/V1__create_core_schema.sql
- src/main/resources/db/migration/V2__insert_development_sample_data.sql

Phase 5 will add Maven/Spring Boot and Flyway dependency/configuration, then validate migration execution on application startup. See ../database/README.md and ../docs/DATABASE_DESIGN.md. Never commit credentials. No default admin user is seeded.
