# Phase 5 — Backend Foundation

**Status:** Implementation and GitHub CI verified; local owner verification pending  
**Working branch:** `phase5/backend-foundation`  
**Stack:** Java 21, Spring Boot 4.1.2, Maven, MySQL 8.4, Spring Data JPA, Flyway, REST, springdoc-openapi 3.2.0.

## Scope
1. Spring Boot and Maven project setup.
2. Layered package architecture, shared JPA base entity and reusable repository/service contracts.
3. Environment-based MySQL connection.
4. JPA/Hibernate configuration with schema validation (no Hibernate schema creation).
5. Flyway integration and validation of Phase 4 V1/V2 migration history.
6. Shared API success/error response structures.
7. Centralized exception handling.
8. Validation dependency and exception mapping.
9. Configurable CORS for local Customer/Admin frontend origins.
10. OpenAPI and Swagger UI.
11. Health endpoint and database-aware Actuator health.
12. Automated integration tests and GitHub Actions CI using MySQL 8.4.

## Acceptance criteria
- [ ] JDK 21 and Maven are available on the owner's development machine.
- [ ] Docker Engine is running and local MySQL 8.4 container is healthy.
- [x] Backend Maven build succeeds in GitHub Actions.
- [x] Flyway applies V1 and V2 to a clean MySQL 8.4 CI database.
- [x] Application context starts and JPA connects to MySQL in integration tests.
- [x] `/api/v1/health` returns HTTP 200.
- [x] `/actuator/health` reports the database component as UP in integration tests.
- [x] OpenAPI JSON loads in integration tests; Swagger UI is configured.
- [x] Integration tests pass (4 tests).
- [x] GitHub Actions backend CI passes (run 44109910795).
- [x] Customer and Admin frontend source trees remain unchanged.
- [ ] Owner confirms local `mvn clean verify`, backend startup and local endpoint checks.

## MySQL 8.4 compatibility note
MySQL does not permit cascading foreign-key actions on a base column of a STORED generated column. The four indexed generated columns involved in this schema now use VIRTUAL generation, preserving the uniqueness rules while allowing cascading foreign keys. The CI database is disposable. If a persistent local database already recorded an earlier V1 checksum, do not drop it or blindly run the updated migration; reconcile its Flyway history before starting the updated backend.

## Safety and migration rules
- Never commit `.env` or real credentials.
- Never expose internal exception details in API responses.
- Keep Hibernate DDL generation disabled.
- Do not rewrite migrations already applied to a persistent database; future schema changes use V3+.
- Do not implement authentication, payment or full product/order workflows in Phase 5.
- Do not modify Customer Website source without explicit approval.
