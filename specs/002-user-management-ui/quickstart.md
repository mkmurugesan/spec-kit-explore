# Quickstart: User Management UI

**Branch**: `002-user-management-ui`  
**Date**: 2026-04-19

---

## Prerequisites

- The Spring Boot application from `001-user-management-setup` is already running (or can be built with `mvn spring-boot:run`).
- A browser (Chrome, Firefox, Safari, Edge — any modern release).
- No Node.js, npm, or build tools needed.

---

## File Delivery

Place the three static files in the Spring Boot project at exactly these paths:

```
src/main/resources/static/
├── index.html
├── styles.css
└── app.js
```

Spring Boot's built-in static resource handler automatically serves `classpath:/static/` at the web root. No configuration changes are required.

---

## Running the Application

```bash
# From the project root
mvn spring-boot:run
```

Open `http://localhost:8080/` in a browser. The Sign-In form is shown by default.

> **Database**: PostgreSQL must be running (see `docker-compose.yml`). Start it with `docker compose up -d db` if needed.

---

## Manual Verification Steps

### Sign-Up Flow
1. Open `http://localhost:8080/`.
2. Click **Create account** → Sign-Up form appears.
3. Submit with a unique email and password ≥ 8 characters → success message → navigates to Sign-In.
4. Submit with the same email again → inline error adjacent to the email field: `"Email already in use"`.
5. Submit with a password of 7 characters → inline error adjacent to the password field.
6. Submit with a malformed email → inline error adjacent to the email field.
7. Leave any field empty and submit → inline error for each empty field; no network request made.

### Sign-In Flow
1. Submit correct credentials → welcome confirmation; check `sessionStorage` in DevTools (`um_access_token` and `um_refresh_token` keys present).
2. Submit wrong password → error banner: `"Invalid credentials"`.
3. Leave email or password empty → inline field errors; no network request.

### Forgot Password Flow
1. Click **Forgot password?** on the Sign-In form.
2. Submit any valid-format email → generic confirmation message regardless of registration status.
3. Submit a malformed email → inline error adjacent to the email field.
4. (Dev mode) Copy the `resetToken` from the API response (visible in browser DevTools → Network → Response).
5. Enter the token and a new password ≥ 8 characters → success → navigates to Sign-In.
6. Re-use the same token → error message displayed; "Request new token" link visible.

### Error Handling
1. Stop the Spring Boot application and submit any form → connection error banner: `"Could not reach the server. Please check your connection."`.
2. Submit button must be re-enabled after the error and form fields must retain their values.

---

## File Structure Reference

```
src/main/resources/static/
├── index.html    — all four views (sign-in, sign-up, forgot-step1, forgot-step2)
├── styles.css    — layout, form styles, error/loading states
└── app.js        — view switching, fetch wrappers, error parsing, sessionStorage management
```

---

## Key Implementation Notes for Developers

| Concern | Implementation |
|---|---|
| View switching | Show/hide `<section>` elements via `.hidden` CSS class |
| Loading state | Disable submit button + set `textContent = "Loading…"` on submit; restore on response |
| Field-level errors | Read `body.details` (object); render message in `<span class="field-error">` next to each input |
| Global errors | Read `body.error` or `body.message`; render in `<div class="error-banner">` |
| Unparseable response | Catch `response.json()` rejection; display static fallback string |
| Network failure | Catch `fetch()` rejection (no response object); display connection error string |
| Token storage | `sessionStorage.setItem('um_access_token', data.accessToken)` on sign-in success |
| No frameworks | Zero `import`, zero CDN JS — only CDN CSS (optional Google Fonts / reset) allowed |

