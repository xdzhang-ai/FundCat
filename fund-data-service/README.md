# AKShare Fund Data Service

这个目录提供 FundCat 的 Python 数据侧能力，职责分成两块：

- `bridge`：把 `akshare` 的基金数据能力包装成稳定的 JSON API，供 Java 后台长历史回源
- `ingestion`：负责基金基础同步、待抓集合生成、晚间净值轮询、outbox 批量发布

## 目录说明

- `main.py`：Uvicorn 启动入口
- `fund_data_service/`：按功能拆分后的 Python 包
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

```bash
cd /Users/winter/zxd/projects/CodexApps/FundCat/fund-data-service
source .venv/bin/activate
uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```

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
