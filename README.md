# FundCat

[简体中文](./README.zh-CN.md) | English

FundCat is a research-mode fund analysis platform scaffold for fund discovery, portfolio simulation, OCR import workflows, weekly reports, and operational feature controls.

It is organized as a monorepo with a Java 21 backend, React + TypeScript frontends, shared contracts, and reusable design tokens for future multi-platform work.

## Repository Layout

```text
.
├── apps/
│   ├── admin/              # Admin console (React + TypeScript + Tailwind CSS)
│   └── web/                # User-facing dashboard (React + TypeScript + Tailwind CSS)
├── packages/
│   ├── contracts/          # Shared TypeScript DTO contracts
│   └── design-tokens/      # Shared color, spacing, typography tokens
├── services/
│   └── api/                # Java 21 + Spring Boot API
├── docker-compose.yml      # MySQL + Redis infrastructure services
└── .env.example            # Example local infrastructure variables
```

## Included Capabilities

- Authentication and profile bootstrap
- Fund search, detail views, and estimate snapshots
- Watchlist, portfolio, holding, and paper-trading workflows
- SIP plan creation and weekly report placeholders
- OCR import job modeling
- Feature flags and provider-health admin overview

## Demo Credentials

- Username: `demo_analyst`
- Password: `ChangeMe123!`

These values are seeded for local development only. Replace them before sharing the environment or publishing any deployment.

## Requirements

- Node.js 22+
- npm 11+
- Java 21
- Maven 3.9+

If your shell default Java is older than 21, set `JAVA_HOME` explicitly before starting the API or running tests:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

## Local Development

### 1. Install frontend dependencies

```bash
npm_config_cache=.npm-cache npm install
```

### 2. Start the API with the default H2 profile

```bash
npm run dev:api:h2
```

### 3. Start the web app

```bash
cd apps/web
npm_config_cache=../../.npm-cache npm run dev
```

### 4. Start the admin app

```bash
cd apps/admin
npm_config_cache=../../.npm-cache npm run dev
```

## Docker Infrastructure

This repository uses `docker compose` for infrastructure services only:

- MySQL 8.4
- Redis 7.4

The API, web app, and admin app run from source by default.

### 1. Prepare environment variables

```bash
cp .env.example .env
```

Key variables:

- `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`
- `MYSQL_PORT`
- `REDIS_PASSWORD`, `REDIS_PORT`
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`

`docker-compose.yml` consumes the `MYSQL_*` and `REDIS_*` variables.  
The API MySQL profile consumes the `DB_*` variables.

### 2. Start MySQL and Redis

```bash
npm run infra:up
```

Useful commands:

```bash
docker compose ps
docker compose logs -f mysql
docker compose logs -f redis
npm run infra:down
sh ./scripts/infra-down.sh -v
```

### 3. Run the API against Docker Redis

```bash
npm run dev:api:redis
```

This mode uses:

- `.env` for `REDIS_*` and `AUTH_SESSION_STORE`
- Docker Redis on port `6379`
- the default H2 in-memory database

### 4. Run the API against MySQL + Redis

```bash
npm run dev:api:mysql
```

If you changed ports or credentials in `.env`, the helper scripts will load matching `DB_*`, `REDIS_*`, and `AUTH_SESSION_STORE` values automatically.

## Useful Local Endpoints

- Web: `http://localhost:5173`
- Admin: `http://localhost:5174`
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Verification

- Web build: `cd apps/web && npm_config_cache=../../.npm-cache npm run build`
- Admin build: `cd apps/admin && npm_config_cache=../../.npm-cache npm run build`
- API tests: `cd services/api && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && mvn test`

## Configuration Notes

- H2 is used by default for local development as an in-memory database holding seeded demo business data, including users, funds, watchlists, portfolios, orders, SIP plans, OCR jobs, reports, alerts, and feature flags.
- MySQL is activated with the `mysql` Spring profile.
- OCR is modeled as a workflow placeholder; no third-party OCR vendor is hardcoded.
- Buy, sell, and SIP flows are simulated only.
- High-risk research capabilities are isolated behind feature flags.

## Production Considerations

- Replace all demo credentials, secrets, and seeded local accounts.
- Move database and Redis credentials to a secrets manager or deployment platform secret store.
- Disable or restrict demo seed data in shared environments.
- Review feature flags before exposing estimate or ranking capabilities publicly.
- Put the API behind TLS termination and apply CORS, rate limiting, and audit logging policies.
- Configure persistent storage, backups, and monitoring for MySQL and Redis.
- Add real object storage and OCR providers before enabling import flows in production.

## Deployment Checklist

- Confirm Java 21 and Node.js versions in CI and deployment images.
- Set production-safe `DB_*`, `MYSQL_*`, and `REDIS_*` values.
- Verify Flyway migrations complete successfully on first boot.
- Check `/actuator/health` and Swagger reachability in the target environment.
- Validate login, dashboard loading, and admin summary after deployment.
- Confirm logs, metrics, and alerting are wired into your observability stack.

## Repository Hygiene

- `.env`, `.env.*`, `PLAN.md`, and `tmp/` are ignored by Git for local-only use.
- `PLAN.md` is intended for local planning and should not be treated as product documentation.
