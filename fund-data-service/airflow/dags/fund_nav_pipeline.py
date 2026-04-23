"""FundCat 基金净值采集与发布 DAG。

这个 DAG 把 fund-data-service 当前已经提供的 ops API 串成正式调度编排：
- 08:30 同步基金基础信息
- 09:00 生成当天待抓集合
- 18:15 / 20:15 / 22:15 执行晚间主轮询，并在轮询后立刻发布 outbox
- 次日 07:30 / 09:00 做前一交易日补偿轮询，并在轮询后立刻发布 outbox
- 每周六 03:00 清理 7 天前 pending 中间表

说明：
- 这里采用 requests 直接调用 fund-data-service 的 HTTP 接口，不要求 Airflow 与服务进程同机部署。
- 这份 DAG 文件默认只是一份可落地模板；真正启用前，请在 Airflow 环境里补齐环境变量。
"""

from __future__ import annotations

import os
from datetime import datetime, timedelta

import requests
from airflow import DAG
from airflow.operators.python import PythonOperator

BASE_URL = os.getenv("FUNDCAT_DATA_SERVICE_BASE_URL", "http://127.0.0.1:8000")
REQUEST_TIMEOUT = int(os.getenv("FUNDCAT_DATA_SERVICE_TIMEOUT_SECONDS", "30"))
OUTBOX_LIMIT = int(os.getenv("FUNDCAT_OUTBOX_BATCH_LIMIT", "200"))


def post_json(path: str, payload: dict | None = None) -> dict:
    """向 fund-data-service 的 ops API 发送 POST 请求并返回 JSON。"""
    response = requests.post(f"{BASE_URL}{path}", json=payload or {}, timeout=REQUEST_TIMEOUT)
    response.raise_for_status()
    return response.json()


def sync_basics() -> dict:
    """同步全量基金基础索引。"""
    return post_json("/ops/funds/sync-basics")


def seed_today_navs() -> dict:
    """为当天生成全量基金待抓集合。"""
    return post_json("/ops/nav/seed")


def poll_evening_navs() -> dict:
    """执行当日晚间主轮询。"""
    return post_json("/ops/nav/poll-evening")


def poll_retry_navs() -> dict:
    """执行前一交易日净值补偿轮询。"""
    return post_json("/ops/nav/poll-retry")


def publish_outbox() -> dict:
    """发布当前 outbox 中待发送的 ready 事件。"""
    return post_json("/ops/outbox/publish", {"limit": OUTBOX_LIMIT})


def cleanup_pending() -> dict:
    """清理 7 天前的基金待抓中间数据。"""
    return post_json("/ops/nav/pending/cleanup")


default_args = {
    "owner": "fundcat",
    "depends_on_past": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=10),
}


with DAG(
    dag_id="fundcat_sync_basics_daily",
    description="每日同步基金基础信息",
    start_date=datetime(2026, 4, 1),
    schedule="30 8 * * *",
    catchup=False,
    default_args=default_args,
    tags=["fundcat", "fund", "ingestion"],
) as sync_basics_dag:
    PythonOperator(task_id="sync_basics", python_callable=sync_basics)


with DAG(
    dag_id="fundcat_nav_seed_daily",
    description="每日生成基金净值待抓集合",
    start_date=datetime(2026, 4, 1),
    schedule="0 9 * * *",
    catchup=False,
    default_args=default_args,
    tags=["fundcat", "fund", "seed"],
) as nav_seed_dag:
    PythonOperator(task_id="seed_today_navs", python_callable=seed_today_navs)


with DAG(
    dag_id="fundcat_nav_poll_evening",
    description="晚间轮询基金净值并发布 ready 事件",
    start_date=datetime(2026, 4, 1),
    schedule="15 18,20,22 * * *",
    catchup=False,
    default_args=default_args,
    tags=["fundcat", "fund", "poll"],
) as evening_poll_dag:
    poll_task = PythonOperator(task_id="poll_evening_navs", python_callable=poll_evening_navs)
    publish_task = PythonOperator(task_id="publish_outbox", python_callable=publish_outbox)
    poll_task >> publish_task


with DAG(
    dag_id="fundcat_nav_poll_retry",
    description="次日补偿前一交易日遗漏净值并发布 ready 事件",
    start_date=datetime(2026, 4, 1),
    schedule="30 7,9 * * *",
    catchup=False,
    default_args=default_args,
    tags=["fundcat", "fund", "retry"],
) as retry_poll_dag:
    retry_task = PythonOperator(task_id="poll_retry_navs", python_callable=poll_retry_navs)
    retry_publish_task = PythonOperator(task_id="publish_outbox", python_callable=publish_outbox)
    retry_task >> retry_publish_task


with DAG(
    dag_id="fundcat_nav_pending_cleanup_weekly",
    description="每周清理基金待抓中间表",
    start_date=datetime(2026, 4, 1),
    schedule="0 3 * * 6",
    catchup=False,
    default_args=default_args,
    tags=["fundcat", "fund", "cleanup"],
) as cleanup_dag:
    PythonOperator(task_id="cleanup_pending_keys", python_callable=cleanup_pending)
