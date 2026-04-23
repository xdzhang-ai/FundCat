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
├── app/
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

### 2. Start the API with the default MySQL configuration

```bash
sh ./scripts/run-api.sh mysql
```

### 3. Start the web app

```bash
cd frontend
npm_config_cache=./.npm-cache npm run dev:web
```

### 4. Start the admin app

```bash
cd frontend
npm_config_cache=./.npm-cache npm run dev:admin
```

## Docker Infrastructure

This repository uses `docker compose` for infrastructure services only:

- MySQL 8.4
- Redis 8.0
- RocketMQ 5.3.2 (NameServer + Broker + Proxy + Dashboard)

The API, web app, and admin app run from source by default.

### 1. Prepare environment variables

```bash
cp .env.example .env
```

Key variables:

- `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`
- `MYSQL_PORT`
- `REDIS_PASSWORD`, `REDIS_PORT`
- `ROCKETMQ_NAMESRV_PORT`, `ROCKETMQ_PROXY_PORT_8081`, `ROCKETMQ_DASHBOARD_PORT`
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `FUNDCAT_ROCKETMQ_ENABLED`, `FUNDCAT_ROCKETMQ_ENDPOINTS`
- `FUNDCAT_PUBLISH_MODE`, `FUNDCAT_ROCKETMQ_TOPIC`, `FUNDCAT_ROCKETMQ_TAG`

`docker-compose.yml` consumes the `MYSQL_*` and `REDIS_*` variables.  
The API MySQL profile consumes the `DB_*` variables.
RocketMQ local stack consumes the `ROCKETMQ_*` and `FUNDCAT_ROCKETMQ_*` variables.

### 2. Start local Docker infrastructure

```bash
sh ./scripts/start-docker.sh
```

This unified entrypoint now starts and checks all required local containers:

- MySQL
- Redis
- RocketMQ NameServer
- RocketMQ Broker (with proxy enabled in the same container)
- RocketMQ Dashboard

Useful commands:

```bash
docker compose ps
docker compose logs -f mysql
docker compose logs -f redis
docker compose logs -f rocketmq-namesrv
docker compose logs -f rocketmq-broker
docker compose logs -f rocketmq-dashboard
npm run infra:down
sh ./scripts/infra-down.sh -v
```

If you only want to restart RocketMQ, you can still use:

```bash
sh ./scripts/start-rocketmq.sh
sh ./scripts/stop-rocketmq.sh
```

Default local endpoints:

- RocketMQ proxy: `127.0.0.1:18081`
- RocketMQ dashboard: `http://127.0.0.1:18088`
- RocketMQ broker 本地默认消息保留时间：`24h`

### 3. Run the API against Docker Redis

```bash
npm run dev:api:redis
```

This mode uses:

- `.env` for `REDIS_*` and `AUTH_SESSION_STORE`
- Docker Redis on port `6379`
- the default MySQL datasource from `application.yml`

### 4. Run the API against MySQL + Redis

```bash
npm run dev:api:mysql
```

If you changed ports or credentials in `.env`, the helper scripts will load matching `DB_*`, `REDIS_*`, and `AUTH_SESSION_STORE` values automatically.

To enable RocketMQ consumer locally, also set in `.env`:

```bash
FUNDCAT_ROCKETMQ_ENABLED=true
FUNDCAT_ROCKETMQ_CONSUMER_MODE=legacy
FUNDCAT_ROCKETMQ_ENDPOINTS=127.0.0.1:19876
FUNDCAT_ROCKETMQ_TOPIC=fund_nav_ready_batch
FUNDCAT_ROCKETMQ_TAG=fund-nav-ready
FUNDCAT_ROCKETMQ_CONSUMER_GROUP=fundcat-fund-nav-consumer
```

For local Docker Compose, Java consumer defaults to `legacy` mode via NameServer.
If you explicitly want the v5 gRPC consumer, set `FUNDCAT_ROCKETMQ_CONSUMER_MODE=v5` and provide a stable proxy environment.

For the Python data service, set:

```bash
FUNDCAT_PUBLISH_MODE=rocketmq
FUNDCAT_ROCKETMQ_PUBLISH_BACKEND=mqadmin
FUNDCAT_ROCKETMQ_ENDPOINTS=127.0.0.1:19876
FUNDCAT_ROCKETMQ_TOPIC=fund_nav_ready_batch
FUNDCAT_ROCKETMQ_TAG=fund-nav-ready
FUNDCAT_ROCKETMQ_PRODUCER_GROUP=fundcat-python-producer
```

## Useful Local Endpoints

- Web: `http://localhost:5173`
- Admin: `http://localhost:5174`
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Verification

- Web build: `cd frontend && npm_config_cache=./.npm-cache npm run build:web`
- Admin build: `cd frontend && npm_config_cache=./.npm-cache npm run build:admin`
- API tests: `cd app && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && mvn test`

## Configuration Notes

- MySQL is now the default local and development datasource.
- The H2 console is disabled by default and no separate MySQL Spring profile is required.
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
