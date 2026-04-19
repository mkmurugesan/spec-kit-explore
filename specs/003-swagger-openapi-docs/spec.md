# Feature Specification: Swagger UI / OpenAPI Documentation

**Feature Branch**: `003-swagger-openapi-docs`  
**Created**: 2026-04-19  
**Status**: Draft  
**Input**: User description: "Adding Swagger UI / OpenAPI documentation to the existing Spring Boot User Management application."

## User Scenarios & Validation *(mandatory)*

### User Story 1 - Browse API Documentation in Browser (Priority: P1)

A developer or API consumer opens a web browser and navigates to the documentation URL of the running User Management application. They can see all available endpoints listed, grouped by category (auth, password reset, admin), along with each endpoint's HTTP method, path, description, and expected request/response schemas.

**Why this priority**: This is the primary value of the feature — making the API self-describing and explorable without needing external documentation. Everything else is secondary.

**Independent Validation**: Start the application locally, navigate to the Swagger UI URL in a browser, and confirm all endpoint groups and schemas are visible.

**Acceptance Scenarios**:

1. **Given** the application is running, **When** a user navigates to the Swagger UI URL in a browser, **Then** a fully rendered interactive API documentation page is displayed.
2. **Given** the Swagger UI page is loaded, **When** the user browses the endpoint list, **Then** all five endpoint groups are visible: `POST /v1/api/auth/signup`, `POST /v1/api/auth/signin`, `POST /v1/api/auth/password-reset/request`, `POST /v1/api/auth/password-reset/confirm`, and the admin endpoints under `/v1/api/admin/**`.
3. **Given** the Swagger UI page is loaded, **When** a user expands any endpoint, **Then** request body schema, field names, field types, and example response schemas are displayed.

---

### User Story 2 - Access OpenAPI JSON/YAML Spec Directly (Priority: P2)

A developer wants to consume the raw OpenAPI specification (e.g., to import into Postman, generate client SDKs, or validate against a schema registry). They can access the machine-readable OpenAPI document via a well-known URL.

**Why this priority**: Enables integration tooling and automation beyond browser-based exploration. Valuable but not the core experience.

**Independent Validation**: Issue a direct HTTP GET request to the OpenAPI spec URL and confirm a valid JSON document describing all endpoints is returned.

**Acceptance Scenarios**:

1. **Given** the application is running, **When** a user sends a GET request to the OpenAPI spec URL, **Then** a valid JSON document conforming to OpenAPI 3.x is returned with HTTP 200.
2. **Given** the returned OpenAPI JSON, **When** imported into Postman or equivalent, **Then** all endpoint collections are correctly populated.

---

### User Story 3 - Unauthenticated Access to Documentation (Priority: P3)

A developer or new team member who has not yet signed in can still access the Swagger UI and the OpenAPI spec URL without providing a JWT token. The application's security layer does not block these documentation paths.

**Why this priority**: Documentation should be accessible to anyone evaluating or integrating the API. Blocking it behind auth defeats its purpose.

**Independent Validation**: Access the Swagger UI URL and OpenAPI spec URL without any `Authorization` header and confirm HTTP 200 is returned (no redirect to login or 401/403 response).

**Acceptance Scenarios**:

1. **Given** no JWT token is provided, **When** the Swagger UI URL is requested, **Then** the response is HTTP 200 and the UI renders correctly.
2. **Given** no JWT token is provided, **When** the OpenAPI spec URL is requested, **Then** the response is HTTP 200 and the JSON spec is returned.

---

### Edge Cases

- What happens when the application starts but no endpoints are registered? The documentation page should still load but show an empty or minimal spec.
- How does the system handle requests to the Swagger UI path when the application is behind a reverse proxy with a context path? The documentation URLs should respect any configured context path.
- What appears for admin endpoints that require HTTP Basic authentication in the UI? The UI must display these endpoints with a clear indication that authentication is required, and provide a way to input HTTP Basic credentials.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The application MUST expose an interactive Swagger UI page accessible via a browser at a documented URL (e.g., `/swagger-ui.html` or `/swagger-ui/index.html`).
- **FR-002**: The application MUST expose a machine-readable OpenAPI 3.x specification document at a documented URL (e.g., `/v3/api-docs`).
- **FR-003**: The documentation MUST include all existing endpoints: `POST /v1/api/auth/signup`, `POST /v1/api/auth/signin`, `POST /v1/api/auth/password-reset/request`, `POST /v1/api/auth/password-reset/confirm`, and all routes under `/v1/api/admin/**`.
- **FR-004**: Each documented endpoint MUST include: HTTP method, full path, brief description, request body schema (where applicable), and expected success/error response schemas.
- **FR-005**: The Swagger UI URL and OpenAPI spec URL MUST be permitted by the application's security configuration so they are accessible without authentication.
- **FR-006**: Endpoints under `/v1/api/admin/**` MUST be annotated in the documentation to indicate that they require authentication using HTTP Basic auth, and the UI MUST provide a mechanism to supply those credentials for test requests.
- **FR-007**: The documentation MUST include at minimum a title, version, and brief description for the API as a whole.
- **FR-008**: No new business logic, data persistence, or backend processing MUST be introduced — this feature is documentation-only.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All existing API endpoints are listed and described in the Swagger UI within 5 seconds of the page loading in a standard browser.
- **SC-002**: The OpenAPI spec JSON document is returned with HTTP 200 in under 1 second when requested directly (measured via curl or equivalent on a local machine).
- **SC-003**: Navigating to the Swagger UI URL without any authentication token returns HTTP 200 and renders the documentation page correctly.
- **SC-004**: Navigating to the OpenAPI spec URL without any authentication token returns HTTP 200 and a valid JSON document.
- **SC-005**: All five endpoint groups (`signup`, `signin`, `password-reset/request`, `password-reset/confirm`, `admin/**`) appear in the UI — verified by manual inspection.

## API Behavior *(include if feature exposes REST endpoints)*

These are the documentation-serving endpoints introduced by this feature:

| Method | Path | Description | Success Response |
|--------|------|-------------|-----------------|
| GET | `/swagger-ui/index.html` (or `/swagger-ui.html`) | Renders the interactive Swagger UI | 200 + HTML page |
| GET | `/v1/api-docs` | Returns the OpenAPI 3.x JSON specification | 200 + JSON document |
| GET | `/v1/api-docs.yaml` | Returns the OpenAPI 3.x YAML specification | 200 + YAML document |

**Manual Validation**: Access each URL via browser and curl after application startup to confirm HTTP 200 responses.

## Assumptions

- The application is running locally or in a development/staging environment; exposing unauthenticated documentation in production is a separate security decision out of scope for this feature.
- The existing Spring Security configuration can be updated to add a whitelist entry for Swagger UI and OpenAPI doc paths without impacting any currently protected routes.
- No changes to existing endpoint behaviour, request validation, or response formats are needed — the documentation reflects what already exists.
- All existing endpoint request/response DTOs have sufficient field names to generate meaningful schema documentation without additional annotations.
- The `springdoc-openapi` library is used (not `springfox`), as it has active maintenance and native Spring Boot 3 / Java 21 support.
- Swagger UI will be enabled in all environments by default; disabling it per-environment is out of scope.

