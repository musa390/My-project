# Technology Stack

| Layer | Technology | Planned use |
|---|---|---|
| Customer frontend | Next.js 15, React 19, TypeScript, Tailwind CSS 4 | Existing storefront; preserve current baseline |
| Admin frontend | Next.js, React, TypeScript, Tailwind CSS | Separate admin application |
| Backend | Java, Spring Boot, Maven | REST API and business logic |
| Security | Spring Security, password hashing, secure HttpOnly cookies, Google OAuth 2.0 / OIDC | Authentication and role/permission checks |
| Persistence | Spring Data JPA, Hibernate | Data access and ORM |
| Database | MySQL | Relational persistence |
| Migrations | Flyway | Versioned schema changes in backend resources |
| API contract | REST, JSON, OpenAPI | Frontend/backend integration contract |
| Media | Cloudinary | Product and collection images |
| Email | Resend | Transactional email after owner configuration |
| Payments | COD + SSLCommerz/bKash/Nagad sandbox | Sandbox/demo only until production approval |
| Hosting | Vercel frontends; Java-compatible backend host; managed MySQL | Deployment phase |

Do not upgrade the existing Customer Website dependencies without a specific compatibility reason and approval. Confirm exact dependency versions in each app's lockfile.
