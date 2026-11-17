# Known Issues and Pending Decisions

1. The supplied GitHub repository URL could not be verified from this environment. A browser fetch returned 404 and direct network access was unavailable; this does not establish whether the repository is private, missing or inaccessible for another reason.
2. Git initialization, initial commit and push are pending repository access and commit-author identity confirmation.
3. The existing Customer Website report records original storefront Phase 6 as in progress; full runtime, typecheck, lint, build and browser verification remain unconfirmed.
4. Admin frontend, Spring Boot backend, MySQL ERD/migrations and OpenAPI contract are not implemented.
5. Owner-supplied brand details, contact information, social links, policy text, image rights and production domain must be verified before launch.
6. Payment providers remain sandbox/demo only; no real payment processing is configured.
7. Local MySQL Docker Compose template has been authored but not started or tested.
