# LUXE PERFUME Backend — Phase 5

Java 21 + Spring Boot 4.1.2 + Maven REST API foundation.

## Prerequisites
- JDK 21 (`java -version`)
- Apache Maven 3.6.3 or later (`mvn -version`)
- Docker Desktop with Docker Engine running
- Repository-root `.env` copied from `.env.example`, with local MySQL passwords set

## Start MySQL
From the repository root:
```powershell
docker compose up -d mysql
docker compose ps
```
Wait until the MySQL container is healthy.

## Load environment and run backend
From the repository root:
```powershell
cd backend
. .\scripts\load-env.ps1
java -version
mvn -version
mvn clean verify
mvn spring-boot:run
```
The environment script reads root `.env` into this PowerShell session without printing secrets. Never paste real `.env` values into chat or commit the file.

## Local endpoints
- API health: http://localhost:8080/api/v1/health
- Database-aware health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Flyway migration policy
Flyway applies the current Phase 4 schema and development seed:
- `V1__create_core_schema.sql`
- `V2__insert_development_sample_data.sql`

The generated columns used alongside cascading foreign keys are VIRTUAL for MySQL 8.4 compatibility. The CI database is disposable. If your local database already recorded an earlier V1 checksum, stop and reconcile the Flyway history before starting the updated backend; do not drop the database or erase data. Hibernate schema generation is disabled; Hibernate validates mapped entities rather than creating or changing tables. Future schema changes should use new versioned migrations.

## Layered architecture
- `config`: CORS and OpenAPI configuration
- `common/controller`: shared endpoints such as health
- `common/dto`: standard success and error response records
- `common/entity`: shared JPA mapped superclass
- `common/repository`: reusable Spring Data JPA repository base
- `common/service`: CRUD service contract and transactional abstract implementation
- `common/exception`: centralized API exception handling

Domain-specific controllers, services, repositories and entities will be added in the corresponding feature phases.

Authentication, customer/product business endpoints, payment processing and real customer data are not implemented in this foundation phase.
