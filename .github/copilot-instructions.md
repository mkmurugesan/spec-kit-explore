# spec-kit-explore Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-04-19

## Active Technologies
- Java 21 LTS (confirmed in `pom.xml`) (003-swagger-openapi-docs)
- PostgreSQL (existing; not touched by this feature) (003-swagger-openapi-docs)

- Java 21 (LTS) — confirmed in `pom.xml` (002-user-management-ui)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Java 21 (LTS) — confirmed in `pom.xml`

## Code Style

Java 21 (LTS) — confirmed in `pom.xml`: Follow standard conventions

## Recent Changes
- 003-swagger-openapi-docs: Added Java 21 LTS (confirmed in `pom.xml`)

- 002-user-management-ui: Added Java 21 (LTS) — confirmed in `pom.xml`

<!-- MANUAL ADDITIONS START -->
## 002-user-management-ui additions

- UI is plain HTML + CSS + Vanilla JS — zero frameworks, zero npm, zero build tool.
- Static files live in `src/main/resources/static/` (served automatically by Spring Boot).
- Three files only: `index.html`, `styles.css`, `app.js`.
- JWT tokens stored in `sessionStorage` under keys `um_access_token` and `um_refresh_token`.
- View switching via `.hidden` CSS class on `<section>` elements — no client-side router.
- API errors parsed from `body.details` (field-level) and `body.error`/`body.message` (global).
- No backend Java files added or modified for this feature.
<!-- MANUAL ADDITIONS END -->
