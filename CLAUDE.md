# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot 3.5.x web application (Java 17, Maven). Package root: `com.bryant.denden_homework`. Currently a fresh scaffold — only the `@SpringBootApplication` entry point (`DendenHomeworkApplication`) exists, so most feature code (controllers, entities, repositories, services, security config) is yet to be written.

## Commands

Use the Maven wrapper (`./mvnw`) — no system Maven required.

```bash
./mvnw spring-boot:run                 # Run the app (default port 8080)
./mvnw clean package                   # Build the executable jar into target/
./mvnw test                            # Run all tests
./mvnw test -Dtest=ClassName           # Run a single test class
./mvnw test -Dtest=ClassName#method    # Run a single test method
```

After `clean package`, run the jar directly with `java -jar target/denden_homework-0.0.1-SNAPSHOT.jar`.

## Architecture & stack notes

- **Persistence**: Spring Data JPA. `h2` (runtime) is the in-memory dev/test database; `postgresql` (runtime) is the production driver. No datasource is configured in `application.properties` yet — JPA expects DB connection properties to be added there (or via profiles) before persistence works.
- **Security**: `spring-boot-starter-security` is on the classpath, which means **all endpoints are locked down by default** with HTTP Basic auth and a generated password until a `SecurityFilterChain` bean is defined. Expect to add a security config class when exposing endpoints.
- **API docs**: springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`) auto-serves Swagger UI at `/swagger-ui.html` and the spec at `/v3/api-docs` once controllers exist. Note security may block these paths until permitted.
- **Validation**: `spring-boot-starter-validation` (Jakarta Bean Validation) is available for `@Valid` request DTOs.
- **Mail**: `spring-boot-starter-mail` is present; SMTP properties must be added to `application.properties` to use `JavaMailSender`.
- **Lombok**: Enabled via annotation processor paths in `pom.xml` and excluded from the final jar. Use Lombok annotations (`@Data`, `@Builder`, `@Slf4j`, etc.) rather than hand-writing boilerplate to match project intent.

## Conventions

- All application code belongs under `com.bryant.denden_homework`. Component scanning starts from this root, so place new packages (e.g. `controller`, `service`, `repository`, `entity`, `config`) beneath it.
- Configuration lives in `src/main/resources/application.properties` (currently only sets `spring.application.name`). Prefer profile-specific files (`application-dev.properties`, etc.) when separating H2 vs PostgreSQL settings.
