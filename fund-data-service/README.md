# AKShare Fund Data Service

这个目录提供 FundCat 的 Python 数据侧能力，职责分成两块：

- `bridge`：把 `akshare` 的基金数据能力包装成稳定的 JSON API，供 Java 后台长历史回源
- `ingestion`：负责基金基础同步、待抓集合生成、晚间净值轮询、outbox 批量发布

## 目录说明

- `main.py`：Uvicorn 启动入口
- `fund_data_service/`：按功能拆分后的 Python 包
- `scripts/publish_test_message.py`：手动向 RocketMQ 发送一条 `fund_nav_ready_batch` 测试消息
- `airflow/dags/fund_nav_pipeline.py`：Airflow 调度模板
- `requirements.txt`：Python 依赖
- `akshare.ipynb`：AKShare 调研 notebook

## 安装依赖

```bash
cd /Users/winter/zxd/projects/CodexApps/FundCat/fund-data-service
python3 -m venv .venv
source .venv/bin/activate
python3 -m pip install -r requirements.txt
```

## 启动服务

服务启动时会自动读取当前目录下的 `.env`，显式环境变量会优先覆盖 `.env` 中的同名值。

```bash
cd /Users/winter/zxd/projects/CodexApps/FundCat/fund-data-service
source .venv/bin/activate
uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```

## 环境变量

默认会自动加载 [`.env`](/Users/winter/zxd/projects/CodexApps/FundCat/fund-data-service/.env)，当前主要用到：

- `FUNDCAT_DB_URL`
- `FUNDCAT_OPS_BASE_URL`
- `FUNDCAT_PUBLISH_MODE`
- `FUNDCAT_ROCKETMQ_PUBLISH_BACKEND`
- `FUNDCAT_OUTBOX_BATCH_SIZE`
- `FUNDCAT_PENDING_QUERY_LIMIT`
- `FUNDCAT_ROCKETMQ_ENDPOINTS`
- `FUNDCAT_ROCKETMQ_TOPIC`
- `FUNDCAT_ROCKETMQ_TAG`
- `FUNDCAT_ROCKETMQ_PRODUCER_GROUP`
- `FUNDCAT_ROCKETMQ_NAMESPACE`
- `FUNDCAT_ROCKETMQ_ACCESS_KEY`
- `FUNDCAT_ROCKETMQ_SECRET_KEY`

当 `FUNDCAT_PUBLISH_MODE=rocketmq` 时：

- 本地 Docker Compose 默认建议 `FUNDCAT_ROCKETMQ_PUBLISH_BACKEND=mqadmin`
  - Python 仍然会把消息真实发到 RocketMQ
  - 只是发送动作委托给 broker 容器内的官方 `mqadmin`，绕开本地 Python SDK 与 local proxy 的兼容问题
- 如需继续验证 Python SDK，可把 backend 改成 `sdk`
- `outbox/publish` 会把 `fund_nav_ready_batch` 真正发到 RocketMQ
- `FUNDCAT_ROCKETMQ_ENDPOINTS` 必填

## 手动联调 RocketMQ

在补齐 `.env` 或环境变量后，可以直接发送一条测试消息：

```bash
cd /Users/winter/zxd/projects/CodexApps/FundCat/fund-data-service
source /Users/winter/zxd/projects/CodexApps/FundCat/.venv-jupyter/bin/activate
python3 scripts/publish_test_message.py --nav-date 2026-04-22 --fund-code 000001 --fund-code 005827
```

联调验证建议：

1. 启动 Java 后台，并打开 RocketMQ consumer 配置：
   - `FUNDCAT_ROCKETMQ_ENABLED=true`
   - `FUNDCAT_ROCKETMQ_CONSUMER_MODE=legacy`
   - `FUNDCAT_ROCKETMQ_ENDPOINTS=127.0.0.1:19876`
2. 启动 Python 数据服务，并把：
   - `FUNDCAT_PUBLISH_MODE=rocketmq`
   - `FUNDCAT_ROCKETMQ_PUBLISH_BACKEND=mqadmin`
   - `FUNDCAT_ROCKETMQ_ENDPOINTS=127.0.0.1:19876`
3. 执行上面的测试脚本
4. 观察 Java 日志里是否出现 `RocketMQ legacy fund_nav_ready_batch consumed`

## Airflow DAG

Airflow 调度模板位于：

- [fund_nav_pipeline.py](/Users/winter/zxd/projects/CodexApps/FundCat/fund-data-service/airflow/dags/fund_nav_pipeline.py)

当前包含 5 条 DAG：

- `fundcat_sync_basics_daily`
- `fundcat_nav_seed_daily`
- `fundcat_nav_poll_evening`
- `fundcat_nav_poll_retry`
- `fundcat_nav_pending_cleanup_weekly`

## 可用接口

- `GET /health`
- `GET /funds/{code}/unit-nav-history`
- `GET /funds/{code}/acc-nav-history`
- `POST /ops/funds/sync-basics`
- `POST /ops/nav/seed`
- `POST /ops/nav/poll-evening`
- `POST /ops/nav/poll-retry`
- `POST /ops/outbox/publish`
- `POST /ops/nav/pending/cleanup`

## 对应 Java 调用示例

本仓库里的 Java 示例入口：

- `/api/v1/funds/bridge/{code}/unit-nav-history`
- `/api/v1/funds/bridge/{code}/acc-nav-history`

配置项位于：

- `fundcat.market-data.akshare-bridge.base-url`
