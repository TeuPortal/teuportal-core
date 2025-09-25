# Core web (Nuxt 3)

SSR shell for the TeuPortal Core experience. It consumes the session-based API and shares tokens/components through the `packages/*` workspaces.

## Environment

Copy `.env.example` to `.env` and adjust as needed:

```
NUXT_PUBLIC_API_BASE=/api
NUXT_PUBLIC_REQUIRE_SETUP=false
NUXT_PUBLIC_APP_NAME=teuportal
```

The `apiBase` is proxied through the reverse proxy in production; locally point it at `http://localhost:8080/api` if you serve the API on 8080.

## Scripts

```bash
pnpm install          # run from repo root once
pnpm dev              # start the Nuxt dev server
pnpm build            # generate the production build
pnpm preview          # preview the production bundle
```

`@nuxt/ui`, Pinia, and VueUse are pre-wired. The generated OpenAPI client will be published into `packages/contracts-client` and imported directly from components/stores.
