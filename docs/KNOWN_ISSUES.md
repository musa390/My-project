# Known Issues and Pending Decisions

## Verified blockers

1. Render service `luxe-perfume-staging-api` is live but its latest deployment failed with TiDB SQLSTATE 28000 / error 1045 because the configured staging database username or password was rejected.
2. TiDB cluster `luxe-perfume-db` is active, but the staging database user, JDBC host details and migrations have not been verified end-to-end.
3. Vercel Customer and Admin projects have not been authenticated/deployed successfully in this session.
4. Google OAuth, Resend, Cloudinary and bKash/Nagad live/sandbox provider credentials are not configured or verified.
5. Browser end-to-end acceptance and responsive QA against deployed live services remain pending.

## Verified requirements still needing owner approval

1. Brand copy, business contact details, policy text, social links, verified domain and image/media rights.
2. Payment provider sandbox merchant accounts and callback specifications.
3. Any real-money, live SSLCommerz, bKash or Nagad activation.
