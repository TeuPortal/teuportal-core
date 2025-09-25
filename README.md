# teuportal-core

Code-first tenant-aware portal skeleton. The Spring Boot API defines the contract, generates OpenAPI, and the Nuxt app consumes the generated TypeScript client.

## Repository layout

- `apps/core-api-spring` – Spring Boot 3 (Maven) API, Flyway migrations, session security.
- `apps/core-web` – Nuxt 3 SSR shell wired for session cookies.
- `packages/*` – TypeScript workspaces (client generation, config, feature flags, UI tokens).

## Prerequisites

- Java 21
- Node.js 18+ with pnpm 8+
- Docker (for local Postgres)

## Environment variables

`apps/core-api-spring`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SERVER_PORT` (optional, defaults to 8080)
- `SESSION_COOKIE_SECURE` (set `false` locally)
- `APP_BASE_URL` (Nuxt origin, defaults to `http://localhost:3000`)

- Optional: copy `apps/core-api-spring/config/api-local.example.properties` to `apps/core-api-spring/config/api-local.properties` to load defaults when env vars are missing.

`apps/core-web`
- `NUXT_PUBLIC_API_BASE` (default `/api`, proxy points at Spring Boot)
- `NUXT_PUBLIC_REQUIRE_SETUP`
- `NUXT_PUBLIC_APP_NAME`

Examples live in each app's `.env.example`.

## Local development

1. Install workspace dependencies: `pnpm install`
2. Start Postgres (see command below) and apply Flyway on boot.
3. Run the API: `cd apps/core-api-spring && ./mvnw spring-boot:run`
4. Run the web: `pnpm -C apps/core-web dev`

Swagger UI is available at `http://localhost:8080/swagger-ui.html`; `/health` remains anonymously accessible.

OpenAPI JSON is emitted under `apps/core-api-spring/target` (`springdoc-openapi`). A follow-up script will translate it into the `packages/contracts-client` package for the Nuxt app.
