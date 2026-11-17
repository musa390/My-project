# LUXE PERFUME Backend

Phase 5 adds the Java 21 / Spring Boot 3.5 backend foundation.

## Run with Docker Compose

From the repository root:

1. Copy `.env.example` to `.env` if you have not already done so.
2. Set local-only values for `MYSQL_PASSWORD` and `MYSQL_ROOT_PASSWORD`.
3. Start MySQL and the backend:

```powershell
docker compose up -d --build mysql backend
docker compose ps
docker compose logs -f backend
```

Compose waits for MySQL's health check before starting the backend. The backend connects to the Compose service hostname `mysql`; do not use `localhost` from inside the backend container.

## Verify the API

- Health: http://localhost:8080/actuator/health
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Swagger UI: http://localhost:8080/swagger-ui.html
- Sample categories: http://localhost:8080/api/v1/categories

Expected health response contains `"status":"UP"`. The categories endpoint reads active development categories from MySQL.

## Run Maven locally (optional)

If running Spring Boot directly on Windows rather than in Docker, set the database environment variables in the same PowerShell session first. The `.env` file is read by Docker Compose, not automatically by a normal Maven process.

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/luxe_perfume?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME = "luxe_app"
$env:SPRING_DATASOURCE_PASSWORD = "<your-local-MYSQL_PASSWORD>"
mvn -f backend/pom.xml spring-boot:run
```

Do not paste actual passwords into chat or commit them.

## Database migration note

Flyway manages the existing V1 and V2 SQL migration files. On a fresh database, Spring Boot applies them at startup. If you previously ran those SQL files manually against a non-empty local database, stop before starting the backend and review the Flyway baseline procedure rather than editing or rerunning applied migrations.

## Architecture

- `catalog/entity`: JPA persistence model
- `catalog/repository`: Spring Data repository
- `catalog/service`: application/service logic
- `catalog/dto`: API data transfer objects
- `catalog/controller`: REST endpoints
- `common`: standard API response and exception handling
- `config`: CORS and OpenAPI configuration

The category endpoint is a small vertical slice demonstrating the layers. Authentication, order processing, payment flows and full catalog operations remain in later phases.
