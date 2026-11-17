# Changelog

## 2026-11-17 — Phase 1 scaffold

- Created a separate full-stack project root and copied the audited Customer Website into `customer-frontend/`.
- Added scaffold directories for Admin frontend, Spring Boot backend, database documentation and OpenAPI contracts.
- Added root `.gitignore`, placeholder `.env.example`, and a local MySQL Docker Compose template.
- Added project plan, status, architecture, stack, database/API placeholders, GitHub workflow, phase handoff and known-issues documentation.
- No Customer Website source files were intentionally changed.
- Initialized local Git on `main` and configured `origin` to the supplied GitHub URL.
- Verified 55/55 copied Customer Website files are byte-identical to the ZIP source; Phase 6 static script passed 12/12; secret ignore patterns checked.
- GitHub remote verification, initial commit and push remain pending.

## 2026-11-17 — Phase 1 GitHub verification update

- Verified `musa390/My-project` as a private repository on `main` with push permission through the connected GitHub account.
- Read the existing repository README and recorded its blob SHA; preserve it during upload.
- Re-ran the Customer Website checksum manifest from `customer-frontend/`: all 52 entries passed.
- Full multi-file upload and initial project commit are still pending; no remote project commit is claimed.
