<!--
Sync Impact Report
- Version change: template-placeholder -> 1.0.0
- Modified principles:
  - [PRINCIPLE_1_NAME] -> I. Clean Code Is Non-Negotiable
  - [PRINCIPLE_2_NAME] -> II. Spring Boot Backend Simplicity
  - [PRINCIPLE_3_NAME] -> III. Minimal Dependency Footprint
  - [PRINCIPLE_4_NAME] -> IV. No Automated Tests or Test Artifacts
  - [PRINCIPLE_5_NAME] -> V. Latest Spring Boot and Maven Required
- Added sections:
  - Additional Constraints
  - Development Workflow
- Removed sections:
  - None
- Templates requiring updates:
  - ✅ .specify/templates/plan-template.md
  - ✅ .specify/templates/spec-template.md
  - ✅ .specify/templates/tasks-template.md
  - ⚠ pending .specify/templates/commands/*.md (directory not present; no updates applied)
- Deferred items:
  - None
-->

# spec-kit-explore Constitution

## Core Principles

### I. Clean Code Is Non-Negotiable
All production code MUST be clear, cohesive, and maintainable. Classes and methods MUST
have a single responsibility, names MUST express intent, and dead code MUST be removed
instead of commented out. Large methods SHOULD be split into small units when complexity
obscures behavior. Rationale: readability and low cognitive load are required for fast,
safe iteration in a small backend codebase.

### II. Spring Boot Backend Simplicity
The system MUST be implemented as a simple backend Java Spring Boot application with a
layered architecture: controller, service, and repository (when persistence exists).
Business rules MUST live in services, HTTP concerns MUST stay in controllers, and cross-
layer shortcuts SHOULD be avoided unless explicitly documented in the feature plan.
Rationale: predictable structure reduces defects and onboarding time.

### III. Minimal Dependency Footprint
Every new dependency MUST have documented necessity and no practical standard-library or
existing-project alternative. Dependencies SHOULD be avoided for trivial utilities and MUST
be removed when no longer used. Starter bundles MUST be selected conservatively to keep
transitive libraries low. Rationale: fewer dependencies reduce supply-chain risk, startup
overhead, and upgrade burden.

### IV. No Automated Tests or Test Artifacts
The repository MUST NOT include unit, integration, end-to-end, contract, or performance
test code, test frameworks, or test pipelines. Plans, specs, and tasks MUST use manual
validation steps only and MUST NOT prescribe test-first or test-writing activities. Rationale:
this project intentionally optimizes for direct implementation speed under explicit no-testing
constraints.

### V. Latest Spring Boot and Maven Required
All implementation MUST target the latest stable Spring Boot release and latest stable Maven
release available at implementation time. Version pinning to older releases MUST be treated
as non-compliant unless explicitly approved as a governance amendment. Build files SHOULD
be updated promptly when a new stable release is adopted for active work. Rationale:
staying current improves security posture and long-term maintainability.

## Additional Constraints

- Language and runtime MUST be Java with Spring Boot.
- Build and dependency management MUST use Maven.
- Project structure MUST remain backend-focused; frontend/UI frameworks are out of scope.
- Manual verification evidence (for example API call logs or endpoint responses) SHOULD be
  captured in feature docs when behavior changes are introduced.
- Any proposal that introduces automated testing, non-Maven builds, or non-Spring runtime
  MUST be rejected unless the constitution is amended first.

## Development Workflow

1. Define or update feature spec with scope, API behavior, and manual validation steps.
2. Create implementation plan that passes Constitution Check gates for architecture,
   dependency minimization, no-testing compliance, and latest-version compliance.
3. Implement in small commits that preserve controller-service-repository boundaries.
4. Validate behavior manually via local application runs and endpoint-level checks.
5. Conduct review focused on clean code, dependency impact, and constitutional compliance.
6. Merge only when reviewer confirms there are no automated tests or test tasks added.

## Governance

- This constitution is the highest-priority engineering policy for this repository; all plans,
  specs, tasks, and implementation artifacts MUST comply.
- Amendments MUST be documented in a pull request that includes rationale, migration impact,
  and explicit semantic version bump justification.
- Versioning policy: MAJOR for incompatible governance changes or principle removals,
  MINOR for new principles/sections or materially expanded mandates, PATCH for clarifications.
- Compliance review MUST occur during planning and code review; violations MUST be fixed
  before merge or formally waived through an approved amendment.
- `README.md` and `.specify/templates/*` SHOULD be kept aligned with this constitution after
  each amendment.

**Version**: 1.0.0 | **Ratified**: 2026-04-19 | **Last Amended**: 2026-04-19
