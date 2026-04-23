"""收拢 Python 数据服务的轻量公共能力。"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date
import json
import logging
import os
from pathlib import Path
import atexit
import subprocess
from threading import Lock
from typing import Any

from pydantic import BaseModel
from dotenv import load_dotenv
import requests


def load_local_env() -> None:
    """在进程启动早期自动加载 fund-data-service 根目录下的 .env。

    这里不覆盖已经存在的系统环境变量，这样本地调试可以直接依赖 `.env`，
    部署环境仍然可以通过真实环境变量覆盖默认值。
    """
    service_root = Path(__file__).resolve().parents[1]
    load_dotenv(service_root / ".env", override=False)


def configure_logging() -> logging.Logger:
    """初始化服务日志并返回统一 logger。"""
    logging.basicConfig(level=logging.INFO)
    return logging.getLogger("fund_data_service")


load_local_env()
LOG = configure_logging()


@dataclass(slots=True)
class Settings:
    """集中承载数据服务运行所需的环境配置。"""

    db_url: str = os.getenv("FUNDCAT_DB_URL", "")
    ops_base_url: str = os.getenv("FUNDCAT_OPS_BASE_URL", "http://127.0.0.1:8080/api/v1/ops")
    outbox_batch_size: int = int(os.getenv("FUNDCAT_OUTBOX_BATCH_SIZE", "200"))
    pending_query_limit: int = int(os.getenv("FUNDCAT_PENDING_QUERY_LIMIT", "500"))
    publish_mode: str = os.getenv("FUNDCAT_PUBLISH_MODE", "log")
    rocketmq_endpoints: str = os.getenv("FUNDCAT_ROCKETMQ_ENDPOINTS", "")
    rocketmq_topic: str = os.getenv("FUNDCAT_ROCKETMQ_TOPIC", "fund_nav_ready_batch")
    rocketmq_tag: str = os.getenv("FUNDCAT_ROCKETMQ_TAG", "fund-nav-ready")
    rocketmq_producer_group: str = os.getenv("FUNDCAT_ROCKETMQ_PRODUCER_GROUP", "fundcat-python-producer")
    rocketmq_publish_backend: str = os.getenv("FUNDCAT_ROCKETMQ_PUBLISH_BACKEND", "sdk")
    rocketmq_namespace: str = os.getenv("FUNDCAT_ROCKETMQ_NAMESPACE", "")
    rocketmq_access_key: str = os.getenv("FUNDCAT_ROCKETMQ_ACCESS_KEY", "")
    rocketmq_secret_key: str = os.getenv("FUNDCAT_ROCKETMQ_SECRET_KEY", "")
    rocketmq_mqadmin_namesrv_addr: str = os.getenv("FUNDCAT_ROCKETMQ_MQADMIN_NAMESRV_ADDR", "rocketmq-namesrv:9876")
    rocketmq_broker_container_name: str = os.getenv("FUNDCAT_ROCKETMQ_BROKER_CONTAINER_NAME", "fundcat-rmq-broker")


SETTINGS = Settings()
_ROCKETMQ_PRODUCER = None
_ROCKETMQ_PRODUCER_LOCK = Lock()


class TaskRequest(BaseModel):
    """按交易日触发的任务请求。"""

    nav_date: date | None = None
    limit: int | None = None


class PublishRequest(BaseModel):
    """Outbox 发布请求。"""

    limit: int | None = None


def flag_enabled(code: str) -> bool:
    """读取 Java 后台运维开关；若后台暂时不可达，则默认放行。"""
    try:
        response = requests.get(f"{SETTINGS.ops_base_url}/feature-flags", timeout=5)
        response.raise_for_status()
        flags = response.json()
    except Exception as exc:  # noqa: BLE001
        LOG.warning("Unable to fetch feature flags from Java ops endpoint: %s", exc)
        return True
    for flag in flags:
        if flag.get("code") == code:
            return bool(flag.get("enabled"))
    return False


def build_rocketmq_producer():
    """懒加载 RocketMQ producer，避免每条 outbox 都重复建立长连接。"""
    from rocketmq import ClientConfiguration, Credentials, Producer

    credentials = Credentials(SETTINGS.rocketmq_access_key, SETTINGS.rocketmq_secret_key)
    configuration = ClientConfiguration(
        endpoints=SETTINGS.rocketmq_endpoints,
        credentials=credentials,
        namespace=SETTINGS.rocketmq_namespace,
    )
    producer = Producer(
        configuration,
        topics=[SETTINGS.rocketmq_topic],
    )
    producer.startup()
    return producer


def publish_batch_message_via_mqadmin(payload: dict[str, Any]) -> None:
    """通过 broker 容器内置 mqadmin 发送批量消息，作为本地开发的稳定发布通道。

    这条路径主要服务本地 Docker Compose 环境：
    - Python 仍然负责把消息真实推入 RocketMQ；
    - 但消息发送动作委托给 broker 容器内的官方 mqadmin，避开本地 Python SDK 与
      local proxy 组合时的兼容性问题。
    """
    event_id = str(payload.get("eventId", "fund-nav-ready-batch"))
    command = [
        "docker",
        "exec",
        SETTINGS.rocketmq_broker_container_name,
        "sh",
        "-lc",
        " ".join(
            [
                "/home/rocketmq/rocketmq-5.3.2/bin/mqadmin sendMessage",
                f"-n {SETTINGS.rocketmq_mqadmin_namesrv_addr}",
                f"-t {SETTINGS.rocketmq_topic}",
                f"-c {SETTINGS.rocketmq_tag}",
                f"-k {event_id}",
                f"-p '{json.dumps(payload, ensure_ascii=False)}'",
            ]
        ),
    ]
    result = subprocess.run(
        command,
        check=True,
        capture_output=True,
        text=True,
    )
    LOG.info(
        "Published fund.nav.ready.batch message via mqadmin, eventId=%s, topic=%s, stdout=%s",
        event_id,
        SETTINGS.rocketmq_topic,
        result.stdout.strip(),
    )


def get_rocketmq_producer():
    """返回进程级单例 producer，给批量消息发布复用同一个 RocketMQ 连接。"""
    global _ROCKETMQ_PRODUCER
    if _ROCKETMQ_PRODUCER is not None:
        return _ROCKETMQ_PRODUCER
    with _ROCKETMQ_PRODUCER_LOCK:
        if _ROCKETMQ_PRODUCER is None:
            if not SETTINGS.rocketmq_endpoints:
                raise RuntimeError("FUNDCAT_ROCKETMQ_ENDPOINTS is required when publish mode is rocketmq")
            _ROCKETMQ_PRODUCER = build_rocketmq_producer()
    return _ROCKETMQ_PRODUCER


def shutdown_rocketmq_producer() -> None:
    """在进程退出前主动关闭 producer，避免本地开发时残留连接。"""
    global _ROCKETMQ_PRODUCER
    if _ROCKETMQ_PRODUCER is None:
        return
    try:
        _ROCKETMQ_PRODUCER.shutdown()
    except Exception as exc:  # noqa: BLE001
        LOG.warning("Failed to shutdown RocketMQ producer cleanly: %s", exc)
    finally:
        _ROCKETMQ_PRODUCER = None


atexit.register(shutdown_rocketmq_producer)


def publish_batch_message(payload: dict[str, Any]) -> None:
    """发布一批基金净值 ready 消息，支持 log 与 RocketMQ 两种模式。"""
    if SETTINGS.publish_mode == "log":
        LOG.info("Publishing fund.nav.ready.batch message, payload=%s", json.dumps(payload, ensure_ascii=False))
        return
    if SETTINGS.publish_mode == "rocketmq":
        if SETTINGS.rocketmq_publish_backend == "mqadmin":
            publish_batch_message_via_mqadmin(payload)
            return
        if SETTINGS.rocketmq_publish_backend != "sdk":
            raise RuntimeError(
                f"Unsupported FUNDCAT_ROCKETMQ_PUBLISH_BACKEND: {SETTINGS.rocketmq_publish_backend}"
            )
        from rocketmq import Message

        producer = get_rocketmq_producer()
        message = Message()
        message.topic = SETTINGS.rocketmq_topic
        if SETTINGS.rocketmq_tag:
            message.tag = SETTINGS.rocketmq_tag
        event_id = str(payload.get("eventId", "fund-nav-ready-batch"))
        message.keys = event_id
        message.body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        receipt = producer.send(message)
        LOG.info(
            "Published fund.nav.ready.batch message to RocketMQ, eventId=%s, messageId=%s, topic=%s",
            event_id,
            getattr(receipt, "message_id", None),
            SETTINGS.rocketmq_topic,
        )
        return
    raise RuntimeError(f"Unsupported publish mode: {SETTINGS.publish_mode}")
