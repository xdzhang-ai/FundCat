"""收拢 Python 数据服务的轻量公共能力。"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date
import json
import logging
import os
from typing import Any

from pydantic import BaseModel
import requests


def configure_logging() -> logging.Logger:
    """初始化服务日志并返回统一 logger。"""
    logging.basicConfig(level=logging.INFO)
    return logging.getLogger("fund_data_service")


LOG = configure_logging()


@dataclass(slots=True)
class Settings:
    """集中承载数据服务运行所需的环境配置。"""

    db_url: str = os.getenv("FUNDCAT_DB_URL", "")
    ops_base_url: str = os.getenv("FUNDCAT_OPS_BASE_URL", "http://127.0.0.1:8080/api/v1/ops")
    outbox_batch_size: int = int(os.getenv("FUNDCAT_OUTBOX_BATCH_SIZE", "200"))
    pending_query_limit: int = int(os.getenv("FUNDCAT_PENDING_QUERY_LIMIT", "500"))
    publish_mode: str = os.getenv("FUNDCAT_PUBLISH_MODE", "log")


SETTINGS = Settings()


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


def publish_batch_message(payload: dict[str, Any]) -> None:
    """发布一批基金净值 ready 消息；当前默认只记日志，后续可平滑替换成 RocketMQ。"""
    if SETTINGS.publish_mode == "log":
        LOG.info("Publishing fund.nav.ready.batch message, payload=%s", json.dumps(payload, ensure_ascii=False))
        return
    raise RuntimeError(f"Unsupported publish mode: {SETTINGS.publish_mode}")
