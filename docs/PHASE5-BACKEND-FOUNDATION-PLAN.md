# Phase 5 — Backend Foundation

**Status:** In progress  
**Working branch:** `phase5/backend-foundation`  
**Stack:** Java 21, Spring Boot 4.1.2, Maven, MySQL 8.4, Spring Data JPA, Flyway, REST, springdoc-openapi 3.2.0.

## Scope
1. Spring Boot and Maven project setup.
2. Layered package architecture and shared JPA base entity.
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
- [ ] JDK 21 and Maven are available on the development machine.
- [ ] Docker Engine is running and MySQL 8.4 container is healthy.
- [ ] Backend Maven build succeeds.
- [ ] Flyway applies V1 and V2 to a clean local database without modifying either migration.
- [ ] Application starts and JPA connects to MySQL.
- [ ] `/api/v1/health` returns HTTP 200.
- [ ] `/actuator/health` reports the database component as UP.
- [ ] Swagger UI and OpenAPI JSON load.
- [ ] Integration tests pass.
- [ ] GitHub Actions backend CI passes.
- [ ] Customer and Admin frontend source trees remain unchanged.

## Safety and migration rules
- Never commit `.env` or real credentials.
- Never expose internal exception details in API responses.
- Keep Hibernate DDL generation disabled.
- Never modify applied V1/V2 migrations; add V3+ for future schema changes.
- Do not implement authentication, payment or full product/order workflows in Phase 5.
- Do not modify Customer Website source without explicit approval.
