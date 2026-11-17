# GitHub Workflow

## Repository

- Repository: `https://github.com/musa390/My-project`
- Visibility: Private
- Default branch: `main`
- Connected account permission: push available
- Existing `README.md` must be preserved; do not overwrite it as part of the project upload.

## Safety rules

- Keep the repository private during development unless the owner explicitly chooses otherwise.
- Never share passwords, access tokens, recovery codes or private keys in chat.
- Use GitHub's own sign-in/authorization flow locally when pushing.
- Never commit `.env`, credentials, API keys, payment secrets, database dumps or customer data.
- Commit `.env.example` placeholders only.
- Review `git status` and `git diff --cached` before every commit.
- Preserve the Customer Website baseline unless the project owner explicitly approves a change.

## Suggested commit sequence

1. `chore: establish full-stack monorepo structure`
2. `docs: add project architecture and phase roadmap`
3. Later phases: small, reviewable commits with tests and verification notes.

## Branching

- `main`: stable verified baseline.
- `feature/<short-task>`: focused feature work.
- Merge only after relevant checks pass and the Customer Website regression scope is reviewed.
