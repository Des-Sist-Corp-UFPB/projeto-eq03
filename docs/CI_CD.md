# CI/CD

Pipelines run on GitHub Actions. All workflows live in `.github/workflows/`.

## Backend (`backend-ci.yml`)

Triggered on push/PR to `main` affecting `salon-back/`.

1. Checkout
2. Setup Java 21 (Temurin)
3. Cache Maven (`~/.m2`)
4. Run tests: `./mvnw test`
5. Build JAR: `./mvnw package -DskipTests`
6. Build Docker image: `docker build -t salon-back .`

## Frontend (`frontend-ci.yml`)

Triggered on push/PR to `main` affecting `salon-front/`.

1. Checkout
2. Setup Node 20
3. `npm ci`
4. Lint: `npm run lint`
5. Type-check: `npm run typecheck`
6. Tests: `npm run test`
7. Build: `npm run build`

## Deploy (`deploy.yml`)

Triggered on push to `main` (after CI passes). Deploys to Linux VPS via SSH.

1. SSH into VPS
2. `git pull origin main`
3. `docker compose pull`
4. `docker compose up -d --build`
5. Nginx reloads automatically via Docker network

## Environment Variables (prod)

Set as GitHub Actions secrets and injected into `docker-compose.yml`:

| Variable          | Used by  |
|-------------------|----------|
| `DB_URL`          | Backend  |
| `DB_USER`         | Backend  |
| `DB_PASS`         | Backend  |
| `JWT_SECRET`      | Backend  |
| `VPS_HOST`        | Deploy   |
| `VPS_SSH_KEY`     | Deploy   |
| `VITE_API_URL`    | Frontend |