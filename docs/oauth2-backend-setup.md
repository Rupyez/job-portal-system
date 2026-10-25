# Google OAuth2 backend setup

This configures the backend only. The frontend callback page is a separate step and is expected at `/oauth2/callback` on the configured frontend origin.

## Google Cloud configuration

1. Configure the OAuth consent screen in Google Cloud Console.
2. Create an OAuth client ID of type **Web application**.
3. Add this authorized redirect URI exactly: `http://localhost:5001/login/oauth2/code/google`.
4. Keep the client secret private; do not commit it to Git.

For deployment, register the matching HTTPS callback URL and set `GOOGLE_REDIRECT_URI` to that exact value.

## Environment variables

Configure these in the shell or secret manager used to run `user-service`:

- `GOOGLE_CLIENT_ID`: OAuth web client ID
- `GOOGLE_CLIENT_SECRET`: OAuth web client secret
- `GOOGLE_REDIRECT_URI`: optional; defaults to `http://localhost:5001/login/oauth2/code/google`
- `APP_FRONTEND_BASE_URL`: frontend origin; defaults to `http://localhost:3000`
- Existing required variables such as `DB_PASSWORD` and `JWT_SECRET` must also be configured.

Placeholder Google credentials allow the service to start locally without Google setup, but Google login will not work until real credentials are supplied.

## Database migration

For an existing MySQL database, review and run `services/user-service/src/main/resources/db/manual/allow_oauth_only_users.sql` once before deploying. It makes the password nullable for Google-only accounts, adds provider identity columns, and creates the short-lived authorization-code table.

Back up the database first. If OAuth columns or the code table already exist, adjust the SQL rather than running it unchanged. New local databases can also be created through the project's JPA schema configuration.

## Backend flow

1. Browser navigates to `GET http://localhost:5001/oauth2/authorization/google`.
2. Google redirects to `GET /login/oauth2/code/google` after authentication.
3. The backend verifies Google's verified email, creates or links the account, and rejects disabled accounts.
4. The backend redirects to `${APP_FRONTEND_BASE_URL}/oauth2/callback?code=...`. The random code is stored only as a SHA-256 hash, expires after two minutes, and is single-use.
5. The frontend will later exchange it with `POST /auth/oauth2/exchange` using JSON `{"code":"..." }`. The response uses the existing `AuthResponse` structure containing the app JWT, refresh token, expirations, and user.

Do not place app access or refresh tokens in redirect URLs.

## Notes

- Password login remains available for accounts with a local password.
- OAuth-only accounts have no local password; password login and change-password should not succeed for them unless a separate password-setting/reset flow is completed.
- Google identity is linked using the provider subject and a verified email.
- OAuth2 uses a short-lived server session to preserve authorization request state; normal API requests continue to use the app's JWT.
- Configure HTTPS, secure cookie settings, allowed origins, and a production-specific redirect URI before deployment.
