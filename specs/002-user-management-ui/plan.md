# Implementation Plan: User Management UI

**Branch**: `002-user-management-ui` | **Date**: 2026-04-19 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/002-user-management-ui/spec.md`

---

## Summary

Add a zero-dependency, plain HTML + CSS + Vanilla JS frontend to the existing Spring Boot User Management application. Three static files (`index.html`, `styles.css`, `app.js`) are placed in `src/main/resources/static/` and served automatically by Spring Boot's built-in resource handler — requiring no backend changes and no build tooling. The UI covers sign-up, sign-in, and forgot-password (two-step) flows, surfaces every API validation error inline and every generic/server error in a prominent banner, stores JWT tokens in `sessionStorage`, and prevents double-submission via submit-button disabling.

---

## Technical Context

**Language/Version**: Java 21 (LTS) — confirmed in `pom.xml`  
**Framework**: Spring Boot 3.4.4 — confirmed in `pom.xml`; latest stable as of 2026-04-19  
**Build Tool**: Maven (Spring Boot parent 3.4.4)  
**New Dependencies**: **None** — `spring-boot-starter-web` (already on classpath) auto-configures static resource serving from `classpath:/static/`  
**Storage**: PostgreSQL (existing, unchanged)  
**Testing**: Prohibited by constitution — manual verification only  
**Target Platform**: Browser (static files) served from the existing JVM backend  
**Project Type**: Static UI addition to an existing Spring Boot REST API  
**Performance Goals**: Page load < 1 s on localhost (3 small files, no external JS dependencies)  
**Constraints**: ≤ 3 files; no npm, no build tool, no JS framework; same-origin serving only  
**Scale/Scope**: Single-user manual workflows; no concurrent session requirements on the UI layer  

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Latest Spring Boot**: Confirmed `3.4.4` — latest stable at 2026-04-19 (matches existing `pom.xml`; no update needed).
- [x] **Latest Maven**: Maven wrapper version unchanged; Spring Boot parent manages plugin versions.
- [x] **Layered architecture**: No new backend layers introduced. Static files are served by Spring Boot's existing `ResourceHttpRequestHandler`; the controller → service → repository boundaries are untouched.
- [x] **Minimal dependencies**: Zero new Maven dependencies added. `spring-boot-starter-web` (already present) provides static resource serving.
- [x] **No test frameworks**: No JUnit, Mockito, or any test library added.
- [x] **No test artifacts**: No `src/test/` directories or test classes planned.
- [⚠] **Backend only / No frontend frameworks**: The constitution states "frontend/UI frameworks are out of scope." This feature introduces static HTML/CSS/JS — **not a UI framework**. No framework, no npm package, no build tool is used. The addition is classified as a **static resource placement** within the existing Spring Boot project. Formal justification is documented in the Complexity Tracking table below.

---

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Static HTML/CSS/JS files added to `resources/static/` (constitution: "frontend/UI frameworks are out of scope") | The product requires a browser-based interface for end users to exercise the existing auth API without using an HTTP client. No alternative UI delivery mechanism exists within this project's constraints. | (1) Postman/curl — not usable by non-technical users. (2) Thymeleaf — adds a new Maven dependency and server-side rendering complexity. (3) Separate frontend project — out of scope and requires CORS configuration. Plain static files served from the same origin are the absolute minimum viable UI surface. |

---

## Project Structure

### Documentation (this feature)

```text
specs/002-user-management-ui/
├── plan.md          ← this file
├── research.md      ← Phase 0 output
├── data-model.md    ← Phase 1 output
├── quickstart.md    ← Phase 1 output
└── tasks.md         ← Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code Changes

```text
src/main/resources/static/       ← NEW directory (Spring Boot serves this at web root automatically)
├── index.html                   ← NEW: all four views as hidden <section> elements
├── styles.css                   ← NEW: layout, form, error banner, loading spinner styles
└── app.js                       ← NEW: view switching, fetch wrappers, error parsing, token storage
```

**Backend support may be required**: While the primary feature work is in static UI files, supporting changes to backend Java or application configuration may be needed to enable the UI end-to-end; do not assume `src/main/java/` and related config files remain untouched.

**No frontend build step**: Static UI edits do not require a separate frontend build; refresh the browser for static resource changes, and restart Spring Boot whenever backend Java or configuration files are modified.

**Structure Decision**: All UI files live flat under `src/main/resources/static/`. Spring Boot's `WebMvcAutoConfiguration` maps this classpath location to `/` automatically via `ResourceHttpRequestHandler`. `index.html` is served at `http://localhost:8080/`. No Spring MVC controller or configuration change is needed.

---

## Phase 0 — Research Summary

All unknowns resolved. See [`research.md`](research.md) for full decision records.

| Topic | Decision |
|---|---|
| Frontend approach | Plain HTML + CSS + Vanilla JS — no framework, no build tool |
| Static serving | `src/main/resources/static/` — Spring Boot auto-serves, zero config |
| API error parsing | `body.details` for field errors; `body.error`/`body.message` for global; fallback to static string |
| Token storage | `sessionStorage` keys `um_access_token` and `um_refresh_token` |
| View switching | Show/hide `<section>` elements via `.hidden` CSS class — no router |
| Loading state | Disable submit button + restore on response |
| Client-side validation | Empty-field checks only; format validation delegated to API |
| File layout | 3 files: `index.html`, `styles.css`, `app.js` |

---

## Phase 1 — Design & Contracts

### Data Model

See [`data-model.md`](data-model.md).

UI-level state entities:
- **`FormState`** — `{ loading, fieldErrors, globalError, values }` — per-form runtime state.
- **`ApiError`** — `{ fieldErrors, globalMessage }` — derived from API response body by `parseApiError()`.
- **`SessionTokenPair`** — `um_access_token` / `um_refresh_token` in `sessionStorage`.

### Interface Contracts

No new API contracts are defined. The UI consumes existing endpoints documented in:
- [`specs/001-user-management-setup/contracts/auth-contract.md`](../001-user-management-setup/contracts/auth-contract.md)

No `contracts/` directory is created for this feature (UI-only; no new backend surface exposed).

### View / Section Map

| Section ID | View | Entry Trigger |
|---|---|---|
| `view-signin` | Sign-In form | Page load (default), after sign-up success, after reset-confirm success |
| `view-signup` | Sign-Up form | "Create account" link |
| `view-forgot-step1` | Forgot Password – email | "Forgot password?" link |
| `view-forgot-step2` | Forgot Password – token + new password | After step-1 API success |

### `index.html` Structure

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>User Management</title>
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <main>
    <section id="view-signin">                   <!-- default visible -->
      <h1>Sign In</h1>
      <div class="error-banner hidden"></div>
      <form id="form-signin">
        <!-- email field + field-error span -->
        <!-- password field + field-error span -->
        <!-- submit button -->
        <!-- links: "Create account", "Forgot password?" -->
      </form>
    </section>

    <section id="view-signup" class="hidden">
      <h1>Create Account</h1>
      <div class="error-banner hidden"></div>
      <form id="form-signup">
        <!-- firstName, lastName, email, password fields -->
        <!-- submit button -->
        <!-- link: "Already have an account? Sign in" -->
      </form>
    </section>

    <section id="view-forgot-step1" class="hidden">
      <h1>Forgot Password</h1>
      <div class="error-banner hidden"></div>
      <form id="form-forgot-step1">
        <!-- email field -->
        <!-- submit button -->
        <!-- link: "Back to Sign In" -->
      </form>
    </section>

    <section id="view-forgot-step2" class="hidden">
      <h1>Reset Password</h1>
      <div class="error-banner hidden"></div>
      <form id="form-forgot-step2">
        <!-- token field (paste from API response / dev tools) -->
        <!-- newPassword field -->
        <!-- submit button -->
        <!-- link: "Request new token" (shows step1 again) -->
      </form>
    </section>
  </main>
  <script src="app.js"></script>
</body>
</html>
```

### `styles.css` Key Rules

| Selector | Purpose |
|---|---|
| `.hidden` | `display: none` — controls section and error element visibility |
| `.error-banner` | Full-width red/orange banner above the form for global errors |
| `.field-error` | Red inline text directly below an input for field-level errors |
| `button[disabled]` | Reduced opacity + `cursor: not-allowed` — loading state |
| `@keyframes spin` | CSS spinner animation on `button[disabled]::after` |

### `app.js` Module Outline

```
Constants
  API_BASE = '/v1/api'
  ENDPOINTS = { signup, signin, resetRequest, resetConfirm }
  STORAGE_KEYS = { access: 'um_access_token', refresh: 'um_refresh_token' }

View management
  showView(id)              — hides all sections, shows target, clears its form errors
  navigateTo(id)            — public alias for showView; resets errors and loading state

Error rendering
  parseApiError(resp, body) → { fieldErrors, globalMessage }
  renderErrors(formEl, apiError)
    — populates .field-error spans by matching data-field attr; shows .error-banner
  clearErrors(formEl)       — hides .error-banner, empties all .field-error spans

Loading state
  setLoading(formEl, bool)  — disables/enables submit button; toggles button text

Fetch wrapper
  apiPost(endpoint, payload)
    → Promise<{ ok: true, data } | { ok: false, apiError }>
    — catches JSON parse failure → fallback ApiError
    — catches fetch() rejection (network) → connection ApiError

Form submit handlers
  handleSignup(e)
  handleSignin(e)
  handleForgotStep1(e)
  handleForgotStep2(e)

Bootstrap
  DOMContentLoaded
    → attach 'submit' listeners to all four forms
    → attach 'click' listeners to all [data-view] navigation links
    → call showView('view-signin')
```

### `handleSignin` Detailed Flow (representative of all handlers)

```
1.  e.preventDefault()
2.  clearErrors(form)
3.  Read email, password from form inputs
4.  Client-validate: if email or password empty →
        renderErrors(form, { fieldErrors: { email/password: 'Required' }, globalMessage: null })
        return
5.  setLoading(form, true)
6.  result = await apiPost(ENDPOINTS.signin, { email, password })
7.  setLoading(form, false)
8a. if result.ok:
        sessionStorage.setItem(STORAGE_KEYS.access, result.data.accessToken)
        sessionStorage.setItem(STORAGE_KEYS.refresh, result.data.refreshToken)
        show success confirmation in banner (or navigate to a welcome section)
8b. if !result.ok:
        renderErrors(form, result.apiError)   ← field values are preserved (not reset)
```

### API Error Parsing Logic (`parseApiError`)

```
Input: Response object, parsed body (or null if parse failed)

If body is null:
  return { fieldErrors: null, globalMessage: 'Something went wrong. Please try again later.' }

If body.details is a non-empty object:
  fieldErrors = body.details
Else:
  fieldErrors = null

globalMessage = body.error ?? body.message ?? 'Something went wrong. Please try again later.'

return { fieldErrors, globalMessage }
```

Network failure (fetch rejects — no Response):
```
return { fieldErrors: null, globalMessage: 'Could not reach the server. Please check your connection.' }
```

### Constitution Check (Post-Design)

- **No new Maven dependencies**: Static file serving is handled by existing `spring-boot-starter-web`. ✅
- **No backend code changes**: Zero Java files added or modified. ✅
- **No test artifacts**: No test classes, directories, or pipeline steps planned. ✅
- **No JS framework**: `app.js` uses only browser-native APIs (`fetch`, `sessionStorage`, `document.querySelector`, `addEventListener`). ✅
- **Constitution violation**: documented and justified in Complexity Tracking above. ✅

---

## Manual Validation Checklist (preview — full detail in `tasks.md`)

| Scenario | Expected Outcome |
|---|---|
| Sign-up with valid unique data | Success message → navigates to sign-in form |
| Sign-up with duplicate email | Inline error next to email field |
| Sign-up with password < 8 chars | Inline error next to password field |
| Sign-up with malformed email | Inline error next to email field |
| Sign-up with any empty field | Inline errors for each empty field; no network request fired |
| Sign-in with correct credentials | Welcome confirmation; `sessionStorage` keys `um_access_token` and `um_refresh_token` present |
| Sign-in with wrong password | Global error banner: "Invalid credentials" |
| Sign-in with empty email or password | Inline errors; no network request |
| Forgot password – any valid-format email | Generic confirmation message displayed |
| Forgot password – malformed email | Inline error next to email field |
| Reset confirm – valid token + new password | Success message → navigates to sign-in |
| Reset confirm – expired/used token | Error message; "Request new token" link visible |
| Any form submitted with server down | Connection error banner; button re-enabled; field values preserved |
| Submit button state during in-flight request | Button disabled; "Loading…" text visible |
