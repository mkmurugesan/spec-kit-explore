# Research: User Management UI

**Phase**: 0  
**Branch**: `002-user-management-ui`  
**Date**: 2026-04-19

---

## 1. Frontend Approach

**Decision**: Plain HTML + CSS + Vanilla JS — single page, no build tool, no framework, no npm.

**Rationale**:
- The spec explicitly mandates this approach (FR-UI-016, FR-UI-017, SC-UI-006).
- The feature scope is four small forms with no complex state management.
- Serving static files from `src/main/resources/static/` requires zero backend changes and avoids CORS issues by co-locating UI and API on the same origin.
- No framework means no CVE surface from third-party JS libraries.

**Alternatives considered**:
- React / Vue / Angular — rejected; build tooling required, conflicts with FR-UI-017.
- Alpine.js / htmx — rejected; external runtime dependency, additional CDN fetch, not needed at this scope.
- Thymeleaf server-side templates — rejected; adds server-rendering complexity and new Spring dependency; static files are simpler.

---

## 2. Static File Serving from Spring Boot

**Decision**: Place all UI files under `src/main/resources/static/`. Spring Boot's `ResourceHttpRequestHandler` (part of `spring-boot-starter-web`, already on the classpath) serves `/static/**` automatically as root-relative URLs.

**Rationale**: No new dependency. Spring MVC's default static resource handler maps `classpath:/static/` to `/`. `index.html` becomes the root document at `http://localhost:8080/`.

**Alternatives considered**:
- Dedicated Nginx container — rejected; out of scope for this feature.
- Separate Spring controller to serve HTML — rejected; unnecessary code when Spring auto-configures static serving.

---

## 3. API Error Response Parsing

**Decision**: Parse API error responses following the schema documented in `specs/001-user-management-setup/contracts/auth-contract.md`.

Two formats observed:
- **Validation errors (400)**: `{ "status": 400, "error": "Validation Failed", "details": { "<field>": "<message>" }, "timestamp": "..." }`
- **Domain / auth errors (401, 409, 5xx)**: `{ "status": <n>, "error": "<summary>", "timestamp": "..." }` or `{ "error": "<summary>", "message": "<details>" }`

**Rationale**: The `details` map provides field-keyed errors for inline display. The top-level `error` / `message` fields cover generic banners. The JS layer will attempt `response.json()` and fall back to a generic string if parsing fails (FR-UI-014).

---

## 4. JWT Token Storage

**Decision**: Store `accessToken` and `refreshToken` in `sessionStorage` using keys `um_access_token` and `um_refresh_token`.

**Rationale**: `sessionStorage` is scoped to the browser tab and cleared on tab close (FR-UI-009, FR-UI-010). This reduces exposure on shared devices compared to `localStorage`. XSS risk exists for both storages; mitigated here by same-origin serving (no third-party scripts loaded at runtime).

**Alternatives considered**:
- `localStorage` — rejected; persists across sessions, higher exposure risk on shared devices.
- HttpOnly cookies — rejected; requires backend change (out of scope).
- In-memory JS variable — rejected; lost on page refresh, poor UX.

---

## 5. Single-Page Form Switching Strategy

**Decision**: All four views (sign-up, sign-in, forgot-password-step1, forgot-password-step2) are rendered as `<section>` elements in a single `index.html`. JavaScript shows/hides sections by toggling a CSS class (`hidden`). No client-side router or History API needed.

**Rationale**: Simplest implementation matching the spec's requirement to avoid client-side routing (spec Assumptions). Page reload is not required between form transitions; form state is reset on navigation.

---

## 6. Loading State & Double-Submission Prevention

**Decision**: On form submit, disable the submit button and replace its text with "Loading…". Re-enable and restore text after the `fetch` promise resolves (success or error).

**Rationale**: Satisfies FR-UI-007 and FR-UI-008. No external spinner library needed; a CSS animation on a pseudo-element can provide a simple visual indicator.

---

## 7. Client-Side Validation

**Decision**: Check for empty required fields before issuing any `fetch` request. Display inline error messages adjacent to blank fields. No regex email validation on the client; rely on API for format errors to stay DRY with backend constraints.

**Rationale**: FR-UI-004 requires empty-field detection. Email format validation on the client would duplicate backend logic and risk drift; a simple "field is required" check is sufficient and reduces code complexity.

---

## 8. File Layout

**Decision**: Three files, no subdirectories under `static/`:

```
src/main/resources/static/
├── index.html    — markup for all four views
├── styles.css    — layout, form styling, error states, loading indicator
└── app.js        — all JS: view switching, fetch wrappers, error parsing, token storage
```

**Rationale**: SC-UI-006 explicitly limits delivery to at most 3 files. A flat layout is easiest to serve and inspect.

---

## 9. Constitution Compliance Note

The constitution (Additional Constraints) states "frontend/UI frameworks are out of scope." This feature introduces static HTML/CSS/JS — not a UI framework. The approach uses zero JS frameworks, zero npm packages, and zero build tooling. It is classified as a **static resource addition** to the existing Spring Boot project, not a frontend framework. This is the minimum possible UI surface to satisfy the product requirement. The violation is formally documented in the plan's Complexity Tracking table.

