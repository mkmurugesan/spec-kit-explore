# Tasks: User Management UI

**Input**: Design documents from `/specs/002-user-management-ui/`  
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, quickstart.md ✅  
**Branch**: `002-user-management-ui`  
**Date**: 2026-04-19

**Tests**: None — the constitution prohibits unit, integration, and end-to-end tests. Manual verification only (see quickstart.md).

**Organization**: Tasks are grouped by user story. All three stories share a single foundational scaffold (index.html + styles.css skeleton), then each story adds its specific form section and JS handler independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths are included in every description

---

## Phase 1: Setup (Static File Scaffold)

**Purpose**: Create the directory and the three empty-but-valid files that Spring Boot will serve. This unblocks all three user stories immediately.

- [x] T001 Create directory `src/main/resources/static/` (mkdir if absent — Spring Boot auto-serves its contents at web root)
- [x] T002 Create `src/main/resources/static/index.html` with full HTML5 boilerplate: `<!DOCTYPE html>`, `<html lang="en">`, `<head>` (charset UTF-8, viewport meta, `<title>User Management</title>`, `<link rel="stylesheet" href="styles.css">`), `<body><main></main><script src="app.js"></script></body>` — four `<section>` placeholders inside `<main>`: `#view-signin` (no `.hidden`), `#view-signup.hidden`, `#view-forgot-step1.hidden`, `#view-forgot-step2.hidden`
- [x] T003 [P] Create `src/main/resources/static/styles.css` with the base rules required by all stories: `.hidden { display: none; }`, `*` box-sizing reset, `body` / `main` / `form` layout (centered column, max-width ~400 px), `input` full-width block, `button` full-width styles, `.error-banner { display:none; background:#fee2e2; color:#b91c1c; padding:.75rem 1rem; border-radius:4px; margin-bottom:1rem; }` (use `.error-banner.visible { display:block; }` instead of toggling `.hidden` on the banner so `.hidden` is reserved for section switching), `.field-error { color:#b91c1c; font-size:.85rem; margin-top:.25rem; min-height:1rem; }`, `button:disabled { opacity:.6; cursor:not-allowed; }`, `@keyframes spin { to { transform: rotate(360deg); } }`, `button:disabled::after { content:''; display:inline-block; width:.8em; height:.8em; border:2px solid currentColor; border-top-color:transparent; border-radius:50%; animation:spin .6s linear infinite; margin-left:.5em; vertical-align:middle; }`
- [x] T004 [P] Create `src/main/resources/static/app.js` with the shared constants and utility functions that every story's handler depends on:
  - Constants: `const API_BASE = '/v1/api';`, `const ENDPOINTS = { signup: API_BASE+'/auth/signup', signin: API_BASE+'/auth/signin', resetRequest: API_BASE+'/auth/password-reset/request', resetConfirm: API_BASE+'/auth/password-reset/confirm' };`, `const STORAGE_KEYS = { access: 'um_access_token', refresh: 'um_refresh_token' };`
  - `function showView(id)` — queries `document.querySelectorAll('main > section')`, adds `.hidden` to all, removes `.hidden` from `document.getElementById(id)`, calls `clearErrors` on the revealed section's form (if present)
  - `function clearErrors(formEl)` — hides `.error-banner` (removes class `visible`), sets `textContent = ''` on every `.field-error` inside `formEl`
  - `function renderErrors(formEl, apiError)` — if `apiError.fieldErrors`: for each key, find `[data-field="${key}"]` span inside `formEl` and set its `textContent`; show `.error-banner` (add class `visible`) and set its `textContent` to `apiError.globalMessage`
  - `function setLoading(formEl, isLoading)` — finds submit button inside `formEl`; sets `disabled = isLoading`; toggles button text between original label and `'Loading…'` (store original in `dataset.label`)
  - `async function apiPost(endpoint, payload)` — wraps `fetch(endpoint, { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload) })`; on network throw returns `{ ok:false, apiError:{ fieldErrors:null, globalMessage:'Could not reach the server. Please check your connection.' } }`; on HTTP response tries `response.json()` (catch → fallback message); if `response.ok` returns `{ ok:true, data }`; else returns `{ ok:false, apiError: parseApiError(body) }`
  - `function parseApiError(body)` — if `!body` returns fallback; `fieldErrors = (body.details && Object.keys(body.details).length) ? body.details : null`; `globalMessage = body.error ?? body.message ?? 'Something went wrong. Please try again later.'`; returns `{ fieldErrors, globalMessage }`
  - DOMContentLoaded bootstrap stub (empty — form listeners added per story in Phases 3-5)

**Checkpoint**: `mvn spring-boot:run` → `http://localhost:8080/` loads a blank page with no console errors. Network tab shows 200 for `index.html`, `styles.css`, `app.js`.

---

## Phase 2: Foundational (No blocking prerequisites beyond Phase 1)

> This feature has no additional foundational infrastructure beyond the scaffold created in Phase 1. The shared JS utilities (`showView`, `apiPost`, `renderErrors`, etc.) created in T004 serve as the foundation for all three user stories. Proceed directly to Phase 3.

---

## Phase 3: User Story 1 — New User Registration via UI Form (Priority: P1) 🎯 MVP

**Goal**: A visitor can open the Sign-Up form, submit valid registration data, see a success message, and be navigated to the Sign-In form. All API validation errors (email in use, password too short, malformed email, empty fields) are displayed inline.

**Acceptance Scenarios covered**: AC1 through AC6 from spec.md §User Story 1.

**Independent Validation**:
1. Run `mvn spring-boot:run` and open `http://localhost:8080/`.
2. Click "Create account" → Sign-Up section appears.
3. Submit valid unique data → success banner → Sign-In view shown.
4. Submit duplicate email → inline error next to email field.
5. Submit password < 8 chars → inline error next to password field.
6. Submit malformed email → inline error next to email field.
7. Leave any field empty → inline errors only, no network request fires (check Network tab).
8. During submission: button shows "Loading…" and is disabled.

### Implementation for User Story 1

- [x] T005 [US1] Add Sign-Up `<section id="view-signup" class="hidden">` markup inside `<main>` in `src/main/resources/static/index.html`: heading `<h1>Create Account</h1>`, `<div class="error-banner"></div>`, `<form id="form-signup">` containing: `<label>` + `<input type="text" name="firstName" required>` + `<span class="field-error" data-field="firstName"></span>` (repeat pattern for `lastName`, `email` [type="email"], `password` [type="password"]), `<button type="submit">Create Account</button>`, paragraph with `<a data-view="view-signin">Already have an account? Sign in</a>`
- [x] T006 [P] [US1] Add Sign-Up CSS rules to `src/main/resources/static/styles.css`: `label { display:block; font-size:.9rem; margin-bottom:.25rem; margin-top:.75rem; }`, `input { width:100%; padding:.5rem .75rem; border:1px solid #d1d5db; border-radius:4px; font-size:1rem; box-sizing:border-box; }`, `input:focus { outline:none; border-color:#6366f1; box-shadow:0 0 0 2px rgba(99,102,241,.3); }`, `a[data-view] { color:#6366f1; cursor:pointer; text-decoration:underline; }`, `h1 { font-size:1.5rem; margin-bottom:1rem; }`
- [x] T007 [US1] Implement `handleSignup(e)` in `src/main/resources/static/app.js`:
  1. `e.preventDefault()`
  2. `clearErrors(form)`
  3. Read `firstName`, `lastName`, `email`, `password` from `form.elements`
  4. Client validate: collect empty fields → if any, `renderErrors(form, { fieldErrors: { <field>: 'Required' }, globalMessage: 'Please fill in all required fields.' })` and `return`
  5. `setLoading(form, true)`
  6. `result = await apiPost(ENDPOINTS.signup, { firstName, lastName, email, password })`
  7. `setLoading(form, false)`
  8. If `result.ok`: show success message in `.error-banner` styled as success (add class `success`; CSS: `.error-banner.success { background:#dcfce7; color:#166534; }`); after 1500 ms `showView('view-signin')`
  9. If `!result.ok`: `renderErrors(form, result.apiError)` (field values preserved — do NOT reset form)
- [x] T008 [US1] Add `handleSignup` event listener and `data-view` navigation listener to the DOMContentLoaded block in `src/main/resources/static/app.js`: `document.getElementById('form-signup').addEventListener('submit', handleSignup)` and generic click handler `document.addEventListener('click', e => { if (e.target.dataset.view) showView(e.target.dataset.view); })` (single delegated listener handles all `[data-view]` links across all stories)

**Checkpoint**: User Story 1 is fully functional. Manually run all 7 validation steps listed above before proceeding.

---

## Phase 4: User Story 2 — Sign-In via UI Form (Priority: P1)

**Goal**: A registered user can sign in, have tokens stored in `sessionStorage`, and see a welcome confirmation. Wrong credentials, blank fields, and server errors are all surfaced clearly.

**Acceptance Scenarios covered**: AC1 through AC4 from spec.md §User Story 2.

**Independent Validation**:
1. On the Sign-In form (default view at page load), submit correct credentials → welcome banner with email shown; DevTools → Application → Session Storage shows `um_access_token` and `um_refresh_token`.
2. Submit wrong password → error banner "Invalid credentials" (or API message).
3. Leave email or password blank → inline field errors; no network request.
4. Stop Spring Boot; submit → connection error banner; button re-enabled; fields retain values.

### Implementation for User Story 2

- [x] T009 [US2] Add Sign-In `<section id="view-signin">` markup (no `.hidden` — default view) inside `<main>` in `src/main/resources/static/index.html`: `<h1>Sign In</h1>`, `<div class="error-banner"></div>`, `<form id="form-signin">` with `email` (type="email") and `password` (type="password") fields each with `<span class="field-error" data-field="...">`, `<button type="submit">Sign In</button>`, links: `<a data-view="view-signup">Create account</a>` and `<a data-view="view-forgot-step1">Forgot password?</a>` — replace the placeholder `<section>` stub from T002
- [x] T010 [P] [US2] Add `.error-banner.success` CSS rule to `src/main/resources/static/styles.css`: `{ background:#dcfce7; color:#166534; }` (reused by sign-up success in US1 — add once here if not already present from T007 implementation)
- [x] T011 [US2] Implement `handleSignin(e)` in `src/main/resources/static/app.js`:
  1. `e.preventDefault()`
  2. `clearErrors(form)`
  3. Read `email`, `password` from `form.elements`
  4. Client validate empty fields → `renderErrors` + `return` if any blank
  5. `setLoading(form, true)`
  6. `result = await apiPost(ENDPOINTS.signin, { email, password })`
  7. `setLoading(form, false)`
  8. If `result.ok`: `sessionStorage.setItem(STORAGE_KEYS.access, result.data.accessToken)`, `sessionStorage.setItem(STORAGE_KEYS.refresh, result.data.refreshToken)`; show success banner: `"Welcome! You are signed in as ${email}."`
  9. If `!result.ok`: `renderErrors(form, result.apiError)` — field values NOT reset
- [x] T012 [US2] Register `handleSignin` in the DOMContentLoaded block in `src/main/resources/static/app.js`: `document.getElementById('form-signin').addEventListener('submit', handleSignin)` (the delegated `[data-view]` click listener from T008 already covers the "Create account" and "Forgot password?" links)

**Checkpoint**: User Story 2 fully functional. Manually validate all 4 scenarios before proceeding.

---

## Phase 5: User Story 3 — Forgotten Password via UI Form (Priority: P2)

**Goal**: A user can request a password reset (generic confirmation regardless of email existence), then use the token + new password to confirm the reset and be navigated back to Sign-In. All API validation errors and expired-token errors are shown inline.

**Acceptance Scenarios covered**: AC1 through AC5 from spec.md §User Story 3.

**Independent Validation**:
1. Click "Forgot password?" on Sign-In → Step 1 form appears.
2. Submit any valid-format email → generic confirmation banner shown (no field errors).
3. Submit malformed email → inline error next to email field.
4. Enter valid token + new password ≥ 8 chars → success → Sign-In view.
5. Re-use expired token → error message; "Request new token" link visible.
6. Submit new password < 8 chars in step 2 → inline error next to `newPassword` field.

### Implementation for User Story 3

- [x] T013 [US3] Add Forgot Password Step 1 `<section id="view-forgot-step1" class="hidden">` markup inside `<main>` in `src/main/resources/static/index.html`: `<h1>Forgot Password</h1>`, `<div class="error-banner"></div>`, `<form id="form-forgot-step1">` with `email` field + `<span class="field-error" data-field="email">`, `<button type="submit">Send Reset Link</button>`, `<a data-view="view-signin">Back to Sign In</a>` — replace the placeholder stub from T002
- [x] T014 [P] [US3] Add Forgot Password Step 2 `<section id="view-forgot-step2" class="hidden">` markup inside `<main>` in `src/main/resources/static/index.html`: `<h1>Reset Password</h1>`, `<div class="error-banner"></div>`, `<form id="form-forgot-step2">` with `token` (type="text", placeholder="Paste reset token") + `<span class="field-error" data-field="token">`, `newPassword` (type="password") + `<span class="field-error" data-field="newPassword">`, `<button type="submit">Reset Password</button>`, `<a data-view="view-forgot-step1">Request new token</a>` — replace the placeholder stub from T002
- [x] T015 [US3] Implement `handleForgotStep1(e)` in `src/main/resources/static/app.js`:
  1. `e.preventDefault()`
  2. `clearErrors(form)`
  3. Read `email` from `form.elements`
  4. Client validate: if blank → `renderErrors(form, { fieldErrors:{ email:'Required' }, globalMessage:'Please enter your email address.' })` + `return`
  5. `setLoading(form, true)`
  6. `result = await apiPost(ENDPOINTS.resetRequest, { email })`
  7. `setLoading(form, false)`
  8. If `result.ok` OR (`!result.ok` AND API returns 404-style — treat as success per spec): show generic banner `"If that email is registered, you will receive reset instructions."` (to match API's generic behaviour, always show this message on any non-network success/error response to avoid email enumeration); field values preserved
  9. If network failure (`!result.ok` with `fieldErrors === null` and no HTTP response): `renderErrors(form, result.apiError)` — show connection error
  10. If `result.ok`: after success banner, show a "Proceed to enter your token" button that calls `showView('view-forgot-step2')` OR auto-navigate after 2000 ms
- [x] T016 [US3] Implement `handleForgotStep2(e)` in `src/main/resources/static/app.js`:
  1. `e.preventDefault()`
  2. `clearErrors(form)`
  3. Read `token`, `newPassword` from `form.elements`
  4. Client validate: collect empty fields → `renderErrors` + `return` if any blank
  5. `setLoading(form, true)`
  6. `result = await apiPost(ENDPOINTS.resetConfirm, { token, newPassword })`
  7. `setLoading(form, false)`
  8. If `result.ok`: show success banner `"Password reset successfully."` + `showView('view-signin')` after 1500 ms
  9. If `!result.ok`: `renderErrors(form, result.apiError)` — field values preserved; "Request new token" `<a>` link already in markup (T014)
- [x] T017 [US3] Register both forgot-password handlers in the DOMContentLoaded block in `src/main/resources/static/app.js`: `document.getElementById('form-forgot-step1').addEventListener('submit', handleForgotStep1)` and `document.getElementById('form-forgot-step2').addEventListener('submit', handleForgotStep2)`

**Checkpoint**: User Story 3 fully functional. Manually validate all 6 scenarios before proceeding.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: UI refinements that benefit all three stories.

- [x] T018 [P] Add page-level layout polish to `src/main/resources/static/styles.css`: `body { font-family: system-ui, sans-serif; background:#f9fafb; color:#111827; margin:0; min-height:100vh; display:flex; align-items:center; justify-content:center; }`, `main { background:#fff; border-radius:8px; box-shadow:0 1px 3px rgba(0,0,0,.12); padding:2rem; width:100%; max-width:400px; }`, `button[type="submit"] { margin-top:1.25rem; background:#6366f1; color:#fff; border:none; border-radius:4px; padding:.65rem 1rem; font-size:1rem; width:100%; cursor:pointer; }`, `button[type="submit"]:hover:not(:disabled) { background:#4f46e5; }`, `p { font-size:.9rem; text-align:center; margin-top:1rem; }`
- [x] T019 [P] Add `<meta name="description" content="User Management">` and `<meta name="theme-color" content="#6366f1">` to `<head>` in `src/main/resources/static/index.html`; ensure section headings use semantic `<h1>` (already planned); add `autocomplete` attributes to all inputs (`autocomplete="given-name"`, `autocomplete="family-name"`, `autocomplete="email"`, `autocomplete="new-password"` / `"current-password"`) for browser autofill support
- [x] T020 Run full quickstart.md manual validation: sign-up → sign-in → forgot-password end-to-end in browser; confirm `sessionStorage` tokens present; confirm all inline and global error paths work; confirm loading state visually; confirm connection-error path with server stopped

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: N/A — merged into Phase 1 for this UI-only feature
- **Phases 3, 4, 5 (User Stories)**: All depend on Phase 1 (T001–T004 must be complete)
  - US1 (Phase 3) and US2 (Phase 4) are both P1 — can be worked in parallel by different developers once T001–T004 are done
  - US3 (Phase 5) is P2 — can start after Phase 1; no hard dependency on US1 or US2 completing first (the shared utilities cover it)
- **Phase 6 (Polish)**: Can start after any Phase 3+ story is complete; T018 and T019 are safe to apply at any time

### User Story Dependencies

| Story | Depends on | Independent? |
|-------|-----------|-------------|
| US1 (Sign-Up) | T001–T004 (scaffold + shared JS) | ✅ Yes |
| US2 (Sign-In) | T001–T004 (scaffold + shared JS) | ✅ Yes |
| US3 (Forgot PW) | T001–T004 (scaffold + shared JS) | ✅ Yes |

### Within Each User Story (Phase 3/4/5)

- HTML section markup task first (`T005`, `T009`, `T013`/`T014`)
- CSS polish task in parallel (`T006`, `T010`)
- JS handler implementation after markup is in place (`T007`, `T011`, `T015`/`T016`)
- Event-listener registration after handler is implemented (`T008`, `T012`, `T017`)

### Parallel Opportunities

- T003 and T004 (Phase 1) — different files, run in parallel after T001+T002
- T006, T007 within Phase 3 — CSS and JS handler can be written simultaneously (different files)
- T013 and T014 within Phase 5 — two separate `<section>` stubs, different sections of same file but independent edits
- T018 and T019 in Phase 6 — different files

---

## Parallel Execution Examples

### Phase 1

```
T001 → T002 → [T003 ‖ T004]
```

### User Story 1 (Phase 3)

```
T005 → [T006 ‖ T007] → T008
```

### User Stories 1 + 2 in parallel (with two developers)

```
Dev A: T005 → T006 → T007 → T008   (US1)
Dev B: T009 → T010 → T011 → T012   (US2)
```

### User Story 3 (Phase 5)

```
[T013 ‖ T014] → [T015 ‖ T016] → T017
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2, both P1)

1. Complete Phase 1 (T001–T004) — scaffold and shared utilities
2. Complete Phase 3 (T005–T008) — Sign-Up form (US1)
3. Complete Phase 4 (T009–T012) — Sign-In form (US2)
4. **STOP and VALIDATE**: Manually walk through quickstart.md Sign-Up and Sign-In sections
5. Deploy / demo — users can register and log in

### Incremental Delivery

1. Phase 1 → scaffold ready
2. Phase 3 → US1 (Sign-Up) functional → validate → demo
3. Phase 4 → US2 (Sign-In) functional → validate → demo
4. Phase 5 → US3 (Forgot Password) functional → validate → demo
5. Phase 6 → polish applied

### Suggested File Edit Order for a Single Developer

```
T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008
→ T009 → T010 → T011 → T012
→ T013 → T014 → T015 → T016 → T017
→ T018 → T019 → T020
```

---

## Notes

- **Backend support may be required**: While the primary feature work is in static UI files, supporting changes to backend Java or application configuration may be needed to enable the UI end-to-end; do not assume `src/main/java/` and related config files remain untouched.
- **No frontend build step**: After editing static files, simply refresh the browser (or restart Spring Boot if the classpath cache needs invalidation — unlikely for static resources in dev mode with `spring.web.resources.static-locations` default).
- **[P] tasks** = different files, no incomplete-task dependencies — safe to parallelise.
- **[Story] labels** map each task to its user story for traceability to spec.md acceptance scenarios.
- **Commit after each checkpoint** to keep history granular and reversible.
- **No test framework, no test files, no CI test steps** — all verification is manual per quickstart.md.
- **`data-view` delegation**: The single delegated click listener added in T008 handles ALL `[data-view]` anchor links across all four sections — no per-story navigation wiring needed in T012 or T017.
- **`.error-banner` visibility**: Controlled via adding/removing the `visible` class (not `.hidden`) to avoid collision with section-switching logic that uses `.hidden`.

