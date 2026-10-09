# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose of this project

This is a learning project. The app itself (a commute planner) is simple on purpose, and the stack is heavier than it needs to be. The goal is to understand the whole Spring Boot + PostgreSQL backend flow from start to end, in a Java environment the author is new to. When making changes, explain the Spring and Java concepts involved: what wires what together, and why. Don't just drop in code.

## Rules

- Never commit `.env` files (they are in `.gitignore`).

## Stack

- Java 25, Spring Boot 4.1.1, Maven (use the wrapper `./mvnw`)
- Web: `spring-boot-starter-webmvc` (servlet-based Spring MVC, not WebFlux)
- Persistence: `spring-boot-starter-jdbc` (plain JDBC / `JdbcClient`, **no JPA or Hibernate**), PostgreSQL driver
- Schema: Flyway. Migrations go in `src/main/resources/db/migration/` as `V<n>__<description>.sql`. Flyway runs them on startup, so change the schema through new migrations, never by editing applied ones.
- Actuator for health and metrics endpoints

Base package: `io.github.hindih3.commuteplanner`. Component scanning starts from `CommutePlannerApplication`, so new beans must live in this package or its subpackages.

## Commands

```bash
./mvnw spring-boot:run                     # run the app
./mvnw clean package                       # build the jar (runs tests)
./mvnw clean package -DskipTests           # build without tests
./mvnw test                                # all tests
./mvnw test -Dtest=ClassName               # one test class
./mvnw test -Dtest=ClassName#methodName    # one test method
```

No linter or formatter is configured.

## Current state

This is a fresh Spring Initializr scaffold with no domain code yet. `application.properties` only sets the app name, and no datasource is configured. Because Flyway and JDBC are on the classpath, the app and the `@SpringBootTest` `contextLoads` test will both fail to start until `spring.datasource.*` points at a reachable Postgres instance.
