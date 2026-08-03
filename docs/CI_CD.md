# CI/CD

Pipelines run on GitHub Actions. Two workflows live in `.github/workflows/`: `ci.yml` and `cd.yml`.

## Continuous Integration (`ci.yml`)

Triggered on push to any branch except `main`, and on pull requests targeting any branch. Builds for the same ref are cancelled in favor of newer commits (`concurrency` + `cancel-in-progress`).

**Job 1 — `changes`:** uses `dorny/paths-filter` to detect whether `salon-back/**` and/or `salon-front/**` changed. The backend and frontend jobs below only run when their respective path changed, to save CI minutes.

**Job 2 — `ci-backend`** (runs if `salon-back/**` changed):
1. Checkout
2. Setup Java 21 (Temurin), Maven cache
3. Build & run tests in one step: `./mvnw clean verify -Dspring.profiles.active=test` — this single command also enforces the Jacoco coverage gate (see [TESTING.md](./TESTING.md))

**Job 3 — `ci-frontend`** (runs if `salon-front/**` changed):
1. Checkout
2. Setup Node 22, npm cache keyed on `salon-front/package-lock.json`
3. `npm ci`
4. Lint: `npm run lint`
5. Build check: `npm run build` (type-checking happens here — `tsc` runs as part of the build script, there is no separate typecheck step)
6. Tests with coverage: `npm run test:coverage`

Two more jobs (`security-backend`, `security-frontend` — CodeQL scans) are defined in the workflow but currently commented out.

## Continuous Deployment (`cd.yml`)

Triggered on push to `main` (and manually via `workflow_dispatch`).

**Job 1 — `build-and-push`:**
1. Checkout
2. Compute a lowercase image tag from `github.repository`
3. Log in to GHCR (`ghcr.io`) using `GITHUB_TOKEN`
4. Set up Docker Buildx
5. Build & push a **single unified image** (frontend + backend in one `Dockerfile` at the repo root) via `docker/build-push-action`, tagged `ghcr.io/<repo_lower>:latest`
   - Build arg `VITE_VAPID_PUBLIC_KEY` is passed from the repo **variable** `vars.VITE_VAPID_PUBLIC_KEY` (not a secret — it's a public key that ends up in every client's browser bundle anyway; using `vars` keeps it visible/auditable in the GitHub UI instead of masked like a secret). It must be injected at build time because Vite embeds `VITE_*` values into the bundle during `npm run build`, which happens inside the image build — passing it at container runtime would be too late.
   - GHA layer caching (`cache-from`/`cache-to: type=gha`)

**Job 2 — `deploy`** (needs `build-and-push`):
- SSHes into the deployment VPS (professor's server) using secrets `SSH_KEY`, `SSH_USER`, `SSH_HOST`, and passes `github.actor`/`GITHUB_TOKEN` for the remote side to authenticate and pull the new image.
- The workflow itself does **not** run `git pull`, `docker compose up`, or reload Nginx — that sequence happens on the remote server in response to this trigger, not as inline steps in `cd.yml`.

## Local development (`docker-compose.yml`)

Not a CI/CD workflow, but the local equivalent of the image build: `salon-app.build.args` also passes `VITE_VAPID_PUBLIC_KEY: ${VITE_VAPID_PUBLIC_KEY}` from the `.env` file, for the same reason as in `cd.yml` — without it, `docker compose up --build` produces an image where the VAPID key is `undefined` and push notifications silently fail. The compose stack also runs `otel-lgtm` (Grafana's all-in-one OpenTelemetry stack) alongside `db`, `mailpit`, and `salon-app` — see [opentelemetry.md](./opentelemetry.md).

## Secrets & Variables

| Name                     | Scope       | Used by                              |
|---------------------------|-------------|----------------------------------------|
| `GITHUB_TOKEN`            | Auto-provided | `cd.yml` — GHCR login, remote auth   |
| `SSH_KEY`                 | Repo secret | `cd.yml` — deploy job                 |
| `SSH_USER`                | Repo secret | `cd.yml` — deploy job                 |
| `SSH_HOST`                | Repo secret | `cd.yml` — deploy job                 |
| `VITE_VAPID_PUBLIC_KEY`   | Repo variable | `cd.yml` build-args (and `docker-compose.yml` locally) |

Backend/database configuration (`DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, etc.) lives in the `.env` file used by `docker-compose.yml` on the target host, not as GitHub Actions secrets — the CI workflow doesn't build or push backend-specific env vars.
