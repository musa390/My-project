# Phase 12 — Additional Integrations

## Environment
Copy .env.example to .env and keep real secrets out of Git. Activate the Google profile with SPRING_PROFILES_ACTIVE=google. Register the exact callback URL in Google Cloud Console: http://localhost:8080/login/oauth2/code/google for local development.

## Resend
Set RESEND_API_KEY, AUTH_EMAIL_FROM (a sender/domain verified by Resend), and CONTACT_ADMIN_EMAIL. Password-reset email uses the existing AuthService flow. Order confirmation is sent after the order transaction commits. Provider errors must not roll back committed orders or contact submissions.

## Cloudinary
Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET. Staff upload endpoint: POST /api/v1/staff/catalog/images/upload?productId={id}&altText={text}&sortOrder=0&primary=true with multipart field file. Allowed JPEG/PNG/WebP, maximum 10 MB. API secret stays on the backend.

## Google Identity
The Google OIDC client configuration is in application-google.yml. Activate only when GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are configured. Spring Security validates the OIDC identity and the application additionally requires the verified email claim. Google OAuth uses a short-lived server session for the authorization request, then discards it after issuing the application's HttpOnly JWT cookies.

## Newsletter
POST /api/v1/newsletter/subscribe starts double opt-in. GET /api/v1/newsletter/confirm?token=... confirms a time-limited token. Confirmation returns a one-time unsubscribe token to the storefront; POST /api/v1/newsletter/unsubscribe consumes it.

## Browser security
CSRF protection remains enabled. Browser POST requests to contact/newsletter endpoints should first GET /api/v1/auth/csrf and send the returned token in X-XSRF-TOKEN together with the CSRF cookie.

## Contact
POST /api/v1/contact validates and stores the message, applies a per-IP hourly limit, and creates admin in-app notifications. Optional email alert uses CONTACT_ADMIN_EMAIL. Staff can list/filter and update status through GET /api/v1/staff/contact-messages and PATCH /api/v1/staff/contact-messages/{id}/status (ADMIN/MANAGER only).

## Verification boundary
Automated tests do not use live provider credentials. External-provider success/failure requires authorized Google, Cloudinary and Resend test accounts. Never commit .env, OAuth client secrets, Cloudinary API secret, or Resend API keys.
