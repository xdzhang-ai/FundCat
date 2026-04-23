# FundCat

[English](./README.md) | 简体中文

FundCat 是一个面向基金观察、组合模拟、OCR 导入流程、周报生成和运营开关控制的研究型平台脚手架。

仓库采用单仓结构，包含 Java 21 后端、React + TypeScript 前端、共享契约包，以及为后续多端扩展预留的设计令牌。

## 仓库结构

```text
.
├── apps/
│   ├── admin/              # 后台管理端（React + TypeScript + Tailwind CSS）
│   └── web/                # 用户主站（React + TypeScript + Tailwind CSS）
├── packages/
│   ├── contracts/          # 共享 TypeScript DTO 契约
│   └── design-tokens/      # 共享颜色、间距、字体等设计令牌
├── app/
│   └── api/                # Java 21 + Spring Boot API
├── docker-compose.yml      # MySQL + Redis 基础设施编排
└── .env.example            # 本地环境变量示例
```

## 当前包含的能力

- 登录鉴权与用户资料初始化
- 基金搜索、详情和估值快照
- 自选、组合、持仓与模拟交易流程
- 定投计划与周报占位流程
- OCR 导入任务建模
- Feature Flag 与数据源健康状态后台总览

## 演示账号

- 用户名：`demo_analyst`
- 密码：`ChangeMe123!`

以上仅用于本地开发演示，任何共享环境或公开部署前都应替换。

## 环境要求

- Node.js 22+
- npm 11+
- Java 21
- Maven 3.9+

如果你当前 shell 默认的 Java 不是 21，启动 API 或执行测试前请先显式设置：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

## 本地开发

### 1. 安装前端依赖

```bash
npm_config_cache=.npm-cache npm install
```

### 2. 使用默认 MySQL 配置启动 API

```bash
sh ./scripts/run-api.sh mysql
```

### 3. 启动主站

```bash
cd frontend
npm_config_cache=./.npm-cache npm run dev:web
```

### 4. 启动后台

```bash
cd frontend
npm_config_cache=./.npm-cache npm run dev:admin
```

## Docker 基础设施

当前仓库里的 `docker compose` 只负责拉起基础设施服务：

- MySQL 8.4
- Redis 8.0
- RocketMQ 5.3.2（NameServer + Broker + Proxy + Dashboard）

API、主站和后台默认仍从源码启动，这样本地开发反馈更快。

### 1. 准备环境变量

```bash
cp .env.example .env
```

重点变量如下：

- `MYSQL_DATABASE`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_ROOT_PASSWORD`
- `MYSQL_PORT`
- `REDIS_PASSWORD`、`REDIS_PORT`
- `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`

其中：

- `docker-compose.yml` 读取 `MYSQL_*` 和 `REDIS_*`
- API 的 MySQL profile 读取 `DB_*`

### 2. 启动本地 Docker 基础设施

```bash
sh ./scripts/start-docker.sh
```

这个统一入口现在会一起启动并检查：

- MySQL
- Redis
- RocketMQ NameServer
- RocketMQ Broker（同容器内开启 Proxy）
- RocketMQ Dashboard

常用命令：

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

如果只想单独重启 RocketMQ，也可以继续使用：

```bash
sh ./scripts/start-rocketmq.sh
sh ./scripts/stop-rocketmq.sh
```

### 3. 让 API 连接 Docker Redis

```bash
npm run dev:api:redis
```

这个模式会自动使用：

- `.env` 中的 `REDIS_*` 和 `AUTH_SESSION_STORE`
- Docker 中的 Redis
- `application.yml` 中默认的 MySQL 数据源

### 4. 让 API 连接 MySQL + Redis

```bash
npm run dev:api:mysql
```

如果你在 `.env` 里修改了端口或账号密码，这些辅助脚本会自动加载对应的 `DB_*`、`REDIS_*` 和 `AUTH_SESSION_STORE` 变量。

如需本地启用 RocketMQ consumer，可在 `.env` 中加入：

```bash
FUNDCAT_ROCKETMQ_ENABLED=true
FUNDCAT_ROCKETMQ_CONSUMER_MODE=legacy
FUNDCAT_ROCKETMQ_ENDPOINTS=127.0.0.1:19876
FUNDCAT_ROCKETMQ_TOPIC=fund_nav_ready_batch
FUNDCAT_ROCKETMQ_TAG=fund-nav-ready
FUNDCAT_ROCKETMQ_CONSUMER_GROUP=fundcat-fund-nav-consumer
```

本地 Docker Compose 下，Java consumer 现在默认使用通过 NameServer 的 `legacy` 模式。
如果你想显式切到 v5 gRPC consumer，需要手动设置 `FUNDCAT_ROCKETMQ_CONSUMER_MODE=v5`，并保证 proxy 环境稳定。

## 本地访问地址

- 主站：`http://localhost:5173`
- 后台：`http://localhost:5174`
- API：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- 健康检查：`http://localhost:8080/actuator/health`

## 验证命令

- 主站构建：`cd frontend && npm_config_cache=./.npm-cache npm run build:web`
- 后台构建：`cd frontend && npm_config_cache=./.npm-cache npm run build:admin`
- API 测试：`cd app && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && mvn test`

## 配置说明

- 本地与开发环境现在默认使用 MySQL 作为主数据源。
- H2 控制台默认关闭，也不再需要单独的 Spring `mysql` profile。
- OCR 目前是流程占位，没有写死第三方 OCR 服务商。
- 买入、卖出、定投均为模拟流程，不涉及真实交易。
- 高风险研究能力通过 Feature Flag 隔离。

## 生产环境注意事项

- 替换所有演示账号、默认密码和本地示例配置。
- 数据库与 Redis 凭证应放入密钥管理系统或部署平台密文环境变量。
- 在共享环境中关闭或限制演示 seed 数据。
- 对外开放前审查估值、榜单等高风险能力开关。
- API 应放在 TLS 终止层之后，并补齐 CORS、限流和审计日志。
- 为 MySQL 与 Redis 配置持久化、备份、监控和告警。
- 上线 OCR 导入前，需要补齐真实对象存储和 OCR 服务集成。

## 部署检查清单

- 确认 CI 和部署镜像中的 Java / Node 版本正确。
- 设置生产环境安全的 `DB_*`、`MYSQL_*`、`REDIS_*` 变量。
- 确认 Flyway 首次迁移执行成功。
- 检查目标环境中的 `/actuator/health` 与 Swagger 可用性。
- 部署后回归登录、仪表盘加载和后台摘要接口。
- 确认日志、指标和告警已经接入观测平台。

## 仓库清洁规则

- `.env`、`.env.*`、`PLAN.md`、`tmp/` 都仅供本地使用，已加入 Git ignore。
- `PLAN.md` 仅用于本地规划，不建议作为正式产品文档对外分发。
