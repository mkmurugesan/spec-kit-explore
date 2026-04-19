# Phase 0 Research: User Management Setup

**Feature**: 001-user-management-setup  
**Date**: 2026-04-19  
**Status**: Complete — all NEEDS CLARIFICATION resolved

---

## 1. Spring Boot Version

**Decision**: Spring Boot **3.4.4**  
**Rationale**: 3.4.4 is the latest stable release in the 3.x line as of April 2026. It ships with Spring Framework 6.2.x, requires Java 17+ (Java 21 LTS chosen), and receives active OSS support.  
**Alternatives considered**: 3.3.x — superseded; 3.5.0-SNAPSHOT — pre-release, not stable.

---

## 2. Java Version

**Decision**: Java **21 LTS** (Eclipse Temurin / OpenJDK)  
**Rationale**: Java 21 is the current LTS release. Spring Boot 3.4.x requires Java 17+ and is fully tested with 21. Virtual threads (Project Loom) are GA in 21 and benefit low-overhead request handling.  
**Alternatives considered**: Java 17 LTS — supported but older; Java 22+ — not LTS, early access risk.

---

## 3. Maven Version

**Decision**: Maven **3.9.9** (latest stable as of April 2026)  
**Rationale**: Maven 3.9.x is the current production-ready series with improved reproducible builds and dependency-version resolution. The project uses the Spring Boot parent POM which works seamlessly with Maven 3.9.x.  
**Alternatives considered**: Maven 3.8.x — supported but older; Maven 4.x — not yet GA.

---

## 4. Key Dependency Versions (managed by Spring Boot BOM)

| Dependency | Artifact | Version (via BOM) | Justification |
|---|---|---|---|
| Spring Web | `spring-boot-starter-web` | managed by Boot 3.4.4 | REST controllers, HTTP layer |
| Spring Data JPA | `spring-boot-starter-data-jpa` | managed by Boot 3.4.4 | Repository layer, ORM |
| Spring Security | `spring-boot-starter-security` | managed by Boot 3.4.4 | JWT filter, Basic Auth for admin |
| PostgreSQL JDBC | `postgresql` | managed by Boot 3.4.4 | Production database driver |
| jjwt-api | `io.jsonwebtoken:jjwt-api` | **0.12.6** | JWT creation and validation |
| jjwt-impl | `io.jsonwebtoken:jjwt-impl` | **0.12.6** | JJWT runtime implementation |
| jjwt-jackson | `io.jsonwebtoken:jjwt-jackson` | **0.12.6** | JSON serialisation for JWT claims |
| Spring Validation | `spring-boot-starter-validation` | managed by Boot 3.4.4 | Bean Validation (`@Valid`, `@Email`, size constraints) |
| Liquibase | `liquibase-core` | managed by Boot 3.4.4 | Schema migration (changelog-based, versioned changesets) |
| Lombok | `lombok` | managed by Boot 3.4.4 | Boilerplate reduction (getters/setters/builders) |

> **No test dependencies included** — constitution Principle IV strictly prohibits JUnit, Mockito, Spring Boot Test, or any test framework.

---

## 5. Password Hashing

**Decision**: BCrypt via `BCryptPasswordEncoder` provided by Spring Security  
**Rationale**: BCrypt is the industry-standard adaptive hashing algorithm. Spring Security's implementation is battle-tested, requires no additional dependency, and satisfies FR-004.  
**Alternatives considered**: Argon2 — stronger but requires Bouncy Castle dependency (violates Principle III); SHA-256 — not suitable for passwords.

---

## 6. JWT Library

**Decision**: JJWT (io.jsonwebtoken) **0.12.6**  
**Rationale**: JJWT is the de-facto standard JWT library for Java, actively maintained, compact API, zero extra transitive dependencies beyond Jackson (already present via Spring Web). Version 0.12.x uses the modern builder API.  
**Alternatives considered**: Nimbus JOSE+JWT — more featureful but larger; Auth0 java-jwt — alternative but JJWT is more idiomatic with Spring.

---

## 7. Schema Migration

**Decision**: Liquibase (`liquibase-core`, managed by Spring Boot BOM)  
**Rationale**: User-specified constraint. Liquibase provides deterministic, versioned schema migrations that run automatically at startup. It is part of the Spring Boot BOM (no version pinning required). Liquibase uses a `db.changelog-master.yaml` (or XML/SQL) approach, which gives richer rollback support and change-set tracking compared to Flyway's plain SQL approach. It satisfies the assumption that "schema management is a decision for the plan phase."  
**Alternatives considered**: Flyway — simpler SQL-only migrations but less feature-rich rollback support; raw DDL on startup — fragile and non-idempotent.

---

## 8. Database

**Decision**: PostgreSQL 16 (Docker image `postgres:16-alpine`)  
**Rationale**: User-specified constraint. Alpine variant minimises image size. PostgreSQL 16 is the latest stable major version.  
**Alternatives considered**: PostgreSQL 15 — older; H2 in-memory — development only, not production-grade.

---

## 9. API Versioning Strategy

**Decision**: URI prefix `/v1` on all endpoints (e.g., `/v1/api/auth/signup`)  
**Rationale**: User-specified constraint. URI versioning is the simplest, most cache-friendly strategy and requires no custom media type or header negotiation.

---

## 10. Token Expiry Configuration

**Decision**: Custom `@ConfigurationProperties` bean reading `jwt.*` and `auth.*` properties  
**Rationale**: Externalises all expiry values to `application.properties` without code changes, satisfying FR-017. Spring Boot's type-safe `@ConfigurationProperties` is idiomatic and avoids raw `@Value` string parsing.

---

## 11. Admin Credentials Validation

**Decision**: `ApplicationListener<ApplicationStartedEvent>` (or `@PostConstruct` on a `@Configuration` class) that throws `IllegalStateException` if `admin.master.username` or `admin.master.password` is blank  
**Rationale**: Satisfies FR-012a. Fails fast at startup before any request is served. No additional dependency required.

---

## 12. Docker Compose Strategy

**Decision**: Single `docker-compose.yml` at project root with two services: `db` (PostgreSQL) and `app` (Spring Boot JAR built via `maven:3-eclipse-temurin-21` + `eclipse-temurin:21-jre`)  
**Rationale**: User-specified constraint. Multi-stage Dockerfile keeps the final image lean; Docker Compose orchestrates startup order (`depends_on: db`).

---

## Resolved Clarifications Summary

| Item | Resolution |
|---|---|
| JDK version | Java 21 LTS |
| Spring Boot version | 3.4.4 |
| Maven version | 3.9.9 |
| JWT library | JJWT 0.12.6 |
| Schema management | Liquibase Core (Boot-managed) |
| Database | PostgreSQL 16 (Docker) |
| Password hashing | BCrypt (Spring Security built-in) |
| Admin credential guard | Startup fail-fast via `@PostConstruct` |
| API prefix | `/v1` (URI versioning) |

