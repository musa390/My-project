# LUXE PERFUME — Full-Stack E-commerce Platform

A phased full-stack e-commerce project. The existing Customer Website is preserved in `customer-frontend/`; Admin UI, Spring Boot backend, database design and API contracts will be developed as separate deliverables.

## Repository structure

```text
luxe-perfume/
├── customer-frontend/   # Existing Next.js customer website (preserved baseline)
├── admin-frontend/      # Separate admin application (Phase 3)
├── backend/             # Java + Spring Boot REST API (Phases 5 onward)
├── database/            # ERD, schema notes and seed-data documentation
├── api/openapi/         # API contract and OpenAPI documentation
├── docs/                # Project plan, status, architecture and handoffs
├── .env.example         # Placeholder variables only; no secrets
├── .gitignore
└── docker-compose.yml   # Local MySQL service template
```

## Run the existing Customer Website

Requirements: Node.js 20.9+ and npm.

```bash
cd customer-frontend
npm install
npm run dev
```

Open `http://localhost:3000`.

## Local MySQL (for a later backend phase)

1. Copy `.env.example` to `.env`.
2. Replace the two local MySQL password placeholders with strong local development passwords.
3. Start Docker Desktop, then run `docker compose up -d mysql` from the repository root.

Do not use production credentials in local development. Never commit `.env`.

## Important project boundaries

- `customer-frontend/` is a protected baseline. Do not redesign or change its pages, components, styling, features or behavior without explicit project-owner approval.
- The original uploaded ZIP remains untouched; this repository is a separate working copy.
- The current storefront uses sample/demo data and browser Local Storage for several features. It does not yet have a production backend, database, real authentication or payment processing.
- `admin-frontend/`, `backend/`, `database/` and `api/openapi/` are scaffold directories at this stage, not completed applications.
- Payment providers will remain sandbox/demo until explicitly approved for production.
- Keep secrets out of Git. Commit `.env.example` placeholders only.

## Project documents

See `docs/PROJECT_MASTER_PLAN.md`, `docs/PROJECT_STATUS.md`, `docs/ARCHITECTURE.md`, `docs/TECH_STACK.md`, `docs/GITHUB_WORKFLOW.md` and `docs/PHASE_HANDOFF.md`.
