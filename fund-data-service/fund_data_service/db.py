"""数据库连接与 SQL 辅助函数。"""

from __future__ import annotations

import json
from datetime import datetime
from threading import Lock
from fastapi import HTTPException
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Connection, Engine

from fund_data_service.common import SETTINGS

_ENGINE: Engine | None = None
_ENGINE_LOCK = Lock()


def create_db_engine() -> Engine:
    """按进程单例方式构建数据库引擎，避免每次请求都重复创建 Engine 和连接池。"""
    if not SETTINGS.db_url:
        raise HTTPException(status_code=500, detail="FUNDCAT_DB_URL is required for ingestion operations")
    global _ENGINE
    if _ENGINE is not None:
        return _ENGINE
    with _ENGINE_LOCK:
        if _ENGINE is None:
            _ENGINE = create_engine(SETTINGS.db_url, pool_pre_ping=True)
    return _ENGINE


def record_job_start(
    connection: Connection,
    *,
    job_code: str,
    job_source: str,
    job_type: str,
    run_key: str,
    payload_summary: str,
) -> str | None:
    """开始一条任务运行记录；若相同任务今天已成功则返回 None。"""
    existing = connection.execute(
        text("select id, status, attempt_count from ops_job_runs where job_code = :job_code and run_key = :run_key"),
        {"job_code": job_code, "run_key": run_key},
    ).mappings().first()
    if existing and existing["status"] == "SUCCESS":
        return None

    job_id = existing["id"] if existing else __import__("uuid").uuid4().hex
    now = datetime.now()
    attempt_count = 1 if not existing else int(existing["attempt_count"]) + 1
    connection.execute(
        text(
            """
            insert into ops_job_runs (
                id, job_code, job_source, job_type, run_key, status, payload_summary,
                stats_total, stats_success, stats_failed, stats_skipped,
                started_at, finished_at, duration_ms, attempt_count, error_message, created_at, updated_at
            ) values (
                :id, :job_code, :job_source, :job_type, :run_key, 'RUNNING', :payload_summary,
                0, 0, 0, 0, :started_at, null, null, :attempt_count, null, :created_at, :updated_at
            )
            on duplicate key update
                status = values(status),
                payload_summary = values(payload_summary),
                started_at = values(started_at),
                finished_at = null,
                duration_ms = null,
                attempt_count = values(attempt_count),
                error_message = null,
                updated_at = values(updated_at)
            """
        ),
        {
            "id": job_id,
            "job_code": job_code,
            "job_source": job_source,
            "job_type": job_type,
            "run_key": run_key,
            "payload_summary": payload_summary,
            "started_at": now,
            "attempt_count": attempt_count,
            "created_at": now,
            "updated_at": now,
        },
    )
    return job_id


def record_job_finish(
    connection: Connection,
    job_id: str,
    *,
    status: str,
    total: int,
    success: int,
    failed: int,
    skipped: int,
    error: str | None = None,
) -> None:
    """结束一条任务运行记录。"""
    started_at = connection.execute(text("select started_at from ops_job_runs where id = :id"), {"id": job_id}).scalar_one()
    finished_at = datetime.now()
    duration_ms = int((finished_at - started_at).total_seconds() * 1000) if started_at else None
    connection.execute(
        text(
            """
            update ops_job_runs
            set status = :status,
                stats_total = :stats_total,
                stats_success = :stats_success,
                stats_failed = :stats_failed,
                stats_skipped = :stats_skipped,
                error_message = :error_message,
                finished_at = :finished_at,
                duration_ms = :duration_ms,
                updated_at = :updated_at
            where id = :id
            """
        ),
        {
            "id": job_id,
            "status": status,
            "stats_total": total,
            "stats_success": success,
            "stats_failed": failed,
            "stats_skipped": skipped,
            "error_message": error,
            "finished_at": finished_at,
            "duration_ms": duration_ms,
            "updated_at": finished_at,
        },
    )


def skipped_response(reason: str) -> dict[str, str]:
    """统一返回任务跳过结果。"""
    return {"status": "SKIPPED", "reason": reason}


def json_summary(payload: dict[str, object]) -> str:
    """把任务摘要压成单行 JSON，方便写入任务表。"""
    return json.dumps(payload, ensure_ascii=False)
