# Architecture Overview

```text
Customer Browser ──> Customer Frontend (Next.js)
                         │ REST / JSON (future integration)
                         ▼
                    Spring Boot API ──> MySQL
                         │
                         ├── Payment provider sandbox adapters
                         ├── Cloudinary (media)
                         ├── Resend (email)
                         └── Google OAuth / OIDC

Admin Browser ─────> Admin Frontend (separate Next.js app)
                         │ REST / JSON (future integration)
                         └──────────────> Spring Boot API
```

## Boundaries

- Frontends present UI and call the backend; core business rules and provider secrets stay on the server.
- `api/openapi/` documents the contract; Spring Boot controllers implement it.
- `database/` holds ERD and design documentation. Flyway migrations live only in the backend resources.
- Payment provider callbacks and payment verification are handled server-side. A frontend redirect is never proof of payment.
- The existing Customer Website is protected. Any source change needed for API integration requires explicit approval first and must preserve the existing design and behavior.
