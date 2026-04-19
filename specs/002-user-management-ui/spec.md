# Feature Specification: User Management UI

**Feature Branch**: `002-user-management-ui`  
**Created**: 2026-04-19  
**Status**: Draft  
**Input**: "Create a simple UI side to use this user management functions. UI should show all the validation errors and generic errors. This is for the existing User Management Setup project that has signup, signin, forgot password, and admin APIs built with Spring Boot. The UI should be a simple frontend (suggest the best minimal approach - could be plain HTML/CSS/JS or a lightweight framework) that covers: signup form, signin form, forgot password form, and displays all validation errors and generic errors from the API responses."

## Overview

A lightweight, self-contained frontend that gives users a browser-based interface to the existing User Management API. It covers registration, sign-in, and forgotten-password flows and faithfully surfaces every validation error and generic error returned by the API so users are never left guessing what went wrong.

**Recommended minimal approach**: Plain HTML + CSS + vanilla JavaScript (no build tool, no framework, no npm). A single `index.html` plus a small `app.js` and `styles.css` are enough for the scope. This keeps the implementation dependency-free, easy to serve from any static host or directly from the Spring Boot application's `resources/static/` directory, and trivially inspectable by developers.

---

## User Scenarios & Validation *(mandatory)*

### User Story 1 – New User Registration via UI Form (Priority: P1)

A visitor opens the application in their browser, navigates to the Sign-Up form, fills in their name, email address, and password, and submits. The UI sends the data to `POST /v1/api/auth/signup`. If the API returns validation errors (e.g., email already in use, password too short, malformed email), each error is displayed inline next to the relevant field. If the API returns a generic server error, a prominent banner is shown. On success, the user is shown a confirmation and redirected to the Sign-In form.

**Acceptance Scenarios**:

1. **Given** a visitor on the Sign-Up page, **When** they submit a valid unique email and a password of at least 8 characters, **Then** the UI shows a success confirmation and navigates to the Sign-In form.
2. **Given** a visitor submits an email already in use, **When** the API returns a conflict error, **Then** the UI displays the error message adjacent to the email field without reloading the page.
3. **Given** a visitor submits a password shorter than 8 characters, **When** the API returns a validation error, **Then** the UI displays the specific validation message adjacent to the password field.
4. **Given** a visitor submits a malformed email, **When** the API returns a validation error, **Then** the UI displays the specific validation message adjacent to the email field.
5. **Given** the API returns a 5xx server error, **When** the form is submitted, **Then** the UI displays a user-friendly generic error banner (e.g., "Something went wrong. Please try again later.") and does not lose the entered form data.
6. **Given** the UI is waiting for the API response, **When** the request is in flight, **Then** the submit button is disabled and a loading indicator is shown to prevent double submission.

---

### User Story 2 – Sign-In via UI Form (Priority: P1)

A registered user opens the Sign-In form, enters their email and password, and submits. The UI sends the credentials to `POST /v1/api/auth/signin`. On success, the access token and refresh token are stored in the browser for use in subsequent requests, and the user is shown a success state (e.g., a welcome message or redirection to a protected page). On failure, the error from the API (invalid credentials, validation errors) is shown clearly.

**Acceptance Scenarios**:

1. **Given** a registered user on the Sign-In form, **When** they submit correct credentials, **Then** the UI stores the returned access token and refresh token and displays a logged-in confirmation (e.g., welcome message with the user's email).
2. **Given** a user submits incorrect credentials, **When** the API returns a 401, **Then** the UI displays a clear error message (e.g., "Invalid email or password.") without revealing which field is wrong.
3. **Given** a user submits a blank email or password, **When** client-side validation fires before submission, **Then** the UI prevents the network request and highlights the empty fields.
4. **Given** the API returns a 5xx error, **When** sign-in is attempted, **Then** a generic error banner is shown.

---

### User Story 3 – Forgotten Password via UI Form (Priority: P2)

A user who cannot sign in clicks a "Forgot password?" link on the Sign-In form, is taken to the Forgot Password form, enters their email address, and submits. The UI sends the request to `POST /v1/api/auth/password-reset/request`. The user is shown a confirmation that instructions have been sent (generic, regardless of whether the email exists). A second step allows the user to enter the reset token (displayed in dev mode by the API) and choose a new password, sending the data to `POST /v1/api/auth/password-reset/confirm`.

**Acceptance Scenarios**:

1. **Given** a user on the Forgot Password form, **When** they submit any valid-format email, **Then** the UI shows a generic confirmation message regardless of whether the email is registered (matching API behaviour).
2. **Given** the API returns a validation error for a malformed email, **When** the form is submitted, **Then** the UI shows the specific error adjacent to the email field.
3. **Given** a user on the Reset Password confirmation step, **When** they submit a valid token and a new password of at least 8 characters, **Then** the UI shows a success confirmation and navigates to the Sign-In form.
4. **Given** a user submits an expired or already-used reset token, **When** the API returns an error, **Then** the UI shows the specific error message and allows the user to request a new token.
5. **Given** a user submits a new password shorter than 8 characters, **When** the API returns a validation error, **Then** the UI shows the specific validation message adjacent to the password field.

---

### Edge Cases

- **Network timeout / no connection**: If the fetch request fails due to a network error (not an HTTP error), the UI shows a connection error banner and re-enables the submit button.
- **Token storage on shared device**: Tokens are stored in `sessionStorage` (not `localStorage`) by default so they are cleared when the browser tab is closed, reducing exposure on shared devices.
- **Multiple rapid submissions**: The submit button is disabled on first click and re-enabled only after the API response is received, preventing duplicate requests.
- **Empty API error body**: If the API returns an error status but an unparseable or empty body, the UI falls back to a generic error message rather than crashing.
- **Form field persistence on error**: When the API returns an error, all form fields retain their current values so the user does not have to re-enter data.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-UI-001**: The UI MUST provide a Sign-Up form with fields for name, email address, and password.
- **FR-UI-002**: The UI MUST provide a Sign-In form with fields for email address and password.
- **FR-UI-003**: The UI MUST provide a Forgot Password form with a field for email address and a separate step for token + new password entry.
- **FR-UI-004**: All forms MUST perform basic client-side validation (empty field detection) before sending a network request.
- **FR-UI-005**: The UI MUST display field-level validation error messages returned by the API adjacent to the relevant input field.
- **FR-UI-006**: The UI MUST display generic/server errors (4xx non-validation and 5xx) in a prominent error banner visible without scrolling.
- **FR-UI-007**: The UI MUST disable the submit button and show a loading indicator while a request is in flight.
- **FR-UI-008**: The UI MUST re-enable the submit button and preserve form field values after an error response.
- **FR-UI-009**: On successful sign-in, the UI MUST store the access token and refresh token in `sessionStorage`.
- **FR-UI-010**: The UI MUST clear stored tokens on explicit sign-out or browser tab close (using `sessionStorage` semantics).
- **FR-UI-011**: On successful sign-up, the UI MUST navigate the user to the Sign-In form.
- **FR-UI-012**: On successful password reset confirmation, the UI MUST navigate the user to the Sign-In form.
- **FR-UI-013**: The Forgot Password flow MUST always display a generic success message after email submission, regardless of whether the email is registered (mirroring API behaviour).
- **FR-UI-014**: If the API response body cannot be parsed, the UI MUST fall back to a predefined generic error message.
- **FR-UI-015**: The UI MUST handle network-level failures (no response received) and show a connection error message.
- **FR-UI-016**: The UI MUST be servable as static files (no server-side rendering required) and MUST function when placed in the Spring Boot `resources/static/` directory.
- **FR-UI-017**: The UI MUST NOT require any build tool, package manager, or compilation step to run.

### Key Entities (UI-Level)

- **Form State**: Tracks current field values, loading status, and error messages for each form independently.
- **Error Message**: A structured object containing either field-level errors (keyed by field name) or a global message string, derived from the API error response body.
- **Session Token Pair**: Access token and refresh token stored in `sessionStorage` after successful sign-in.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-UI-001**: A user can complete the sign-up → sign-in flow end-to-end in a browser without using an HTTP client, verified manually in under 2 minutes.
- **SC-UI-002**: Every validation error from the existing API (email in use, password too short, malformed email, invalid token) is visible to the user in the browser without inspecting the network tab — confirmed by manually triggering each error condition.
- **SC-UI-003**: A generic error banner appears for any 5xx response and for network failures — confirmed by temporarily pointing the UI at an unavailable server.
- **SC-UI-004**: The submit button is visibly disabled during request flight — confirmed by observing the UI during a slow network simulation.
- **SC-UI-005**: No build step is required; opening `index.html` directly in a browser (or via the Spring Boot static resource path) renders and functions correctly.
- **SC-UI-006**: The complete UI is delivered in at most 3 files (`index.html`, `styles.css`, `app.js`) with no external runtime dependencies fetched at runtime (fonts and resets may be loaded from a CDN for styling convenience only).
- **SC-UI-007**: The forgotten password flow correctly shows a generic confirmation after email submission regardless of whether the email exists — confirmed by testing with both a registered and an unregistered email.

---

## API Behaviour Consumed *(reference)*

The following existing endpoints are consumed by the UI. No changes to the backend are required.

| Method | Path | UI Form |
|--------|------|---------|
| POST | `/v1/api/auth/signup` | Sign-Up form |
| POST | `/v1/api/auth/signin` | Sign-In form |
| POST | `/v1/api/auth/password-reset/request` | Forgot Password – Step 1 |
| POST | `/v1/api/auth/password-reset/confirm` | Forgot Password – Step 2 |

**Error response format assumed** (based on existing backend): The API returns JSON error bodies. Field-level validation errors are expected under a `details` object keyed by field name; generic messages are expected under a top-level `message` field. The UI gracefully handles deviations from this structure by falling back to a generic message.

---

## Assumptions

- The UI is a frontend-only addition; no changes to the Spring Boot backend are in scope.
- Plain HTML + CSS + vanilla JavaScript is sufficient for the feature scope and is the chosen approach.
- The UI will be served from the same origin as the Spring Boot API (placed in `src/main/resources/static/`) to avoid CORS configuration; cross-origin deployment is out of scope for v1.
- `sessionStorage` is used for token storage (cleared on tab close) rather than `localStorage`; persistent login across sessions is a post-v1 enhancement.
- The password reset token is presented to the user via the API response in development mode (as per the existing backend spec); the UI includes a visible input field for the user to paste the token received from the API response or developer tools.
- No admin UI is in scope; the admin endpoints are for developer use via HTTP clients.
- Accessibility (WCAG compliance), internationalisation (i18n), and responsive/mobile-first design are best-effort in v1; semantic HTML and readable font sizes are the baseline.
- No automated UI testing (Selenium, Playwright) is in scope; manual verification is sufficient for v1.
- Client-side routing (history API) is not required; form switching is handled by showing/hiding sections on a single HTML page.

