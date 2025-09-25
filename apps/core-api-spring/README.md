# Core API (Spring Boot)

Code-first REST API backed by Postgres, Flyway migrations, Spring Session JDBC, and SpringDoc OpenAPI. Tenant context is resolved per request and applied via Postgres session variables so RLS can enforce isolation.

## Configure

Copy `.env.example` to `.env` (or export the variables in PowerShell) before starting the service. Use environment variables for `SPRING_DATASOURCE_*`, or copy `config/api-local.example.properties` to `config/api-local.properties` so the API can read the same keys from disk when env vars are absent. The OSS build expects exactly one active `company` row; the context resolver loads it during startup and fails fast if missing.

Flyway SQL migrations live under `src/main/resources/db/migration`. Two placeholder migrations (`V1__baseline_schema.sql`, `V2__rls_bootstrap.sql`) are in place so Flyway runs successfully until the real schema arrives.

## Run locally\r\n\r\nBefore starting the API, ensure a company row exists (for example: `INSERT INTO company (id, name, slug) VALUES (gen_random_uuid(), 'Local Co', 'local');`). The resolver caches this single-company id and applies it for every request.\r\n\r\n```powershell
cd apps/core-api-spring
# optional: set environment variables inline before the command
./mvnw spring-boot:run
```

The application runs on port 8080 by default, applies Flyway on startup, and provisions Spring Session JDBC tables automatically (`spring.session.jdbc.initialize-schema=always`).

- Health probe: `GET http://localhost:8080/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`\r\n- DB health (RLS aware): `http://localhost:8080/health/db`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Actuator health/info endpoints remain accessible for unauthenticated probes. All other routes require a session (authentication flows arrive later).

## Next steps

- Implement real Flyway migrations for the tenant schema.
- Add middleware to set `app.company_id`/`app.user_id` via `SET LOCAL` within request transactions.
- Generate the TypeScript client (`packages/contracts-client`) from the OpenAPI artifact in `target/`.



