"""基金数据采集、pending 集合、outbox 与发布逻辑。"""

from __future__ import annotations

import json
import math
import uuid
from datetime import date, datetime, timedelta
from typing import Any, Iterable, Sequence

import akshare as ak
from sqlalchemy import text
from sqlalchemy.engine import Connection, Engine

from fund_data_service.common import SETTINGS, flag_enabled, publish_batch_message
from fund_data_service.service.bridge_service import dataframe_to_records
from fund_data_service.db import json_summary, record_job_finish, record_job_start, skipped_response


def chunked(items: Sequence[str], batch_size: int) -> Iterable[list[str]]:
    """把大列表切成定长批次，便于 outbox 和 MQ 批量发送。"""
    for start in range(0, len(items), batch_size):
        yield list(items[start : start + batch_size])


def safe_float(value: Any) -> float:
    """把 AKShare 常见的 '--'、百分号字符串等口径统一转成浮点数。"""
    if value is None:
        return 0.0
    raw = str(value).replace("%", "").replace(",", "").strip()
    if not raw or raw == "--":
        return 0.0
    return float(raw)


def sync_fund_basics(engine: Engine) -> dict[str, Any]:
    """全量同步基金索引，只维护基金基础元数据。"""
    if not flag_enabled("python_fund_ingest_enabled"):
        return skipped_response("python_fund_ingest_enabled is disabled")
    records = dataframe_to_records(ak.fund_name_em())
    run_key = date.today().isoformat()
    with engine.begin() as connection:
        job_id = record_job_start(
            connection,
            job_code="fund_basics_sync",
            job_source="AIRFLOW",
            job_type="INGEST",
            run_key=run_key,
            payload_summary=json_summary({"funds": len(records)}),
        )
        if job_id is None:
            return skipped_response("already succeeded")
        inserted = 0
        try:
            now = datetime.now()
            for row in records:
                connection.execute(
                    text(
                        """
                        insert into funds (code, name, tags, top_holdings, created_at)
                        values (:code, :name, :tags, :top_holdings, :created_at)
                        on duplicate key update
                            name = values(name)
                        """
                    ),
                    {
                        "code": row.get("基金代码"),
                        "name": row.get("基金简称"),
                        "tags": "",
                        "top_holdings": "[]",
                        "created_at": now,
                    },
                )
                inserted += 1
            record_job_finish(connection, job_id, status="SUCCESS", total=inserted, success=inserted, failed=0, skipped=0)
            return {"status": "SUCCESS", "count": inserted}
        except Exception as exc:  # noqa: BLE001
            record_job_finish(connection, job_id, status="FAILED", total=inserted, success=inserted, failed=1, skipped=0, error=str(exc))
            raise


def seed_nav_pending(engine: Engine, nav_day: date) -> dict[str, Any]:
    """按全量基金生成当天待抓净值集合。"""
    if not flag_enabled("python_fund_ingest_enabled"):
        return skipped_response("python_fund_ingest_enabled is disabled")
    with engine.begin() as connection:
        job_id = record_job_start(
            connection,
            job_code="fund_nav_seed_daily",
            job_source="AIRFLOW",
            job_type="INGEST",
            run_key=nav_day.isoformat(),
            payload_summary=json_summary({"navDate": nav_day.isoformat()}),
        )
        if job_id is None:
            return skipped_response("already succeeded")
        fund_codes = [row[0] for row in connection.execute(text("select code from funds order by code asc")).all()]
        inserted = 0
        try:
            now = datetime.now()
            for fund_code in fund_codes:
                connection.execute(
                    text(
                        """
                        insert into fund_nav_pending_keys (id, fund_code, nav_date, status, created_at, updated_at)
                        values (:id, :fund_code, :nav_date, 'PENDING', :created_at, :updated_at)
                        on duplicate key update
                            status = if(status = 'SYNCED', status, 'PENDING'),
                            updated_at = values(updated_at)
                        """
                    ),
                    {
                        "id": str(uuid.uuid4()),
                        "fund_code": fund_code,
                        "nav_date": nav_day,
                        "created_at": now,
                        "updated_at": now,
                    },
                )
                inserted += 1
            record_job_finish(connection, job_id, status="SUCCESS", total=inserted, success=inserted, failed=0, skipped=0)
            return {"status": "SUCCESS", "count": inserted, "navDate": nav_day.isoformat()}
        except Exception as exc:  # noqa: BLE001
            record_job_finish(connection, job_id, status="FAILED", total=inserted, success=inserted, failed=1, skipped=0, error=str(exc))
            raise


def upsert_fund_nav(connection: Connection, fund_code: str, nav_day: date, unit_nav: float, accumulated_nav: float, day_growth: float) -> None:
    """把单只基金某日净值写入历史事实表。"""
    connection.execute(
        text(
            """
            insert into fund_nav_history (id, fund_code, nav_date, unit_nav, accumulated_nav, day_growth)
            values (:id, :fund_code, :nav_date, :unit_nav, :accumulated_nav, :day_growth)
            on duplicate key update
                unit_nav = values(unit_nav),
                accumulated_nav = values(accumulated_nav),
                day_growth = values(day_growth)
            """
        ),
        {
            "id": str(uuid.uuid4()),
            "fund_code": fund_code,
            "nav_date": nav_day,
            "unit_nav": unit_nav,
            "accumulated_nav": accumulated_nav,
            "day_growth": day_growth,
        },
    )


def compute_growths(connection: Connection, fund_code: str, nav_day: date, unit_nav: float) -> tuple[float, float, float]:
    """基于已落库的净值历史回算 7/30/365 天涨跌摘要。"""
    rows = connection.execute(
        text(
            """
            select nav_date, unit_nav
            from fund_nav_history
            where fund_code = :fund_code and nav_date <= :nav_date
            order by nav_date desc
            limit 366
            """
        ),
        {"fund_code": fund_code, "nav_date": nav_day},
    ).mappings().all()
    if not rows:
        return (0.0, 0.0, 0.0)
    by_date = {row["nav_date"]: float(row["unit_nav"]) for row in rows}

    def growth(days: int) -> float:
        cutoff = nav_day - timedelta(days=days)
        candidate_dates = [candidate for candidate in by_date if candidate <= cutoff]
        if not candidate_dates:
            return 0.0
        start_nav = by_date[max(candidate_dates)]
        if math.isclose(start_nav, 0.0):
            return 0.0
        return round(((unit_nav - start_nav) / start_nav) * 100, 4)

    return (growth(7), growth(30), growth(365))


def upsert_fund_nav_growth(connection: Connection, fund_code: str, nav_day: date, unit_nav: float) -> None:
    """把基金区间涨跌摘要写入 growth 表。"""
    week_growth, month_growth, year_growth = compute_growths(connection, fund_code, nav_day, unit_nav)
    now = datetime.now()
    connection.execute(
        text(
            """
            insert into fund_nav_growth (id, fund_code, nav_date, week_growth, month_growth, year_growth, updated_at)
            values (:id, :fund_code, :nav_date, :week_growth, :month_growth, :year_growth, :updated_at)
            on duplicate key update
                week_growth = values(week_growth),
                month_growth = values(month_growth),
                year_growth = values(year_growth),
                updated_at = values(updated_at)
            """
        ),
        {
            "id": str(uuid.uuid4()),
            "fund_code": fund_code,
            "nav_date": nav_day,
            "week_growth": week_growth,
            "month_growth": month_growth,
            "year_growth": year_growth,
            "updated_at": now,
        },
    )


def create_outbox_batches(connection: Connection, nav_day: date, fund_codes: list[str]) -> int:
    """按批次创建 outbox 事件，后续由 publisher 扫描并真正发 MQ。"""
    if not fund_codes:
        return 0
    created = 0
    now = datetime.now()
    for index, chunk in enumerate(chunked(fund_codes, SETTINGS.outbox_batch_size), start=1):
        payload = json.dumps(
            {
                "eventId": str(uuid.uuid4()),
                "navDate": nav_day.isoformat(),
                "fundCodes": chunk,
                "count": len(chunk),
                "publishedAt": now.isoformat(),
            },
            ensure_ascii=False,
        )
        connection.execute(
            text(
                """
                insert into outbox_events (id, event_type, aggregate_key, payload, status, created_at, published_at)
                values (:id, 'fund.nav.ready.batch', :aggregate_key, :payload, 'PENDING', :created_at, null)
                on duplicate key update
                    payload = values(payload),
                    status = 'PENDING',
                    created_at = values(created_at),
                    published_at = null
                """
            ),
            {
                "id": str(uuid.uuid4()),
                "aggregate_key": f"{nav_day.isoformat()}#batch#{index:04d}",
                "payload": payload,
                "created_at": now,
            },
        )
        created += 1
    return created


def poll_nav_batch(engine: Engine, *, nav_day: date, job_code: str, run_key: str, limit: int) -> dict[str, Any]:
    """轮询一批待完成基金净值，并把成功结果写入历史表和 outbox。"""
    with engine.begin() as connection:
        job_id = record_job_start(
            connection,
            job_code=job_code,
            job_source="AIRFLOW",
            job_type="RETRY" if "retry" in job_code else "INGEST",
            run_key=run_key,
            payload_summary=json_summary({"navDate": nav_day.isoformat(), "limit": limit}),
        )
        if job_id is None:
            return skipped_response("already succeeded")
        pending_rows = connection.execute(
            text(
                """
                select fund_code
                from fund_nav_pending_keys
                where nav_date = :nav_date and status = 'PENDING'
                order by fund_code asc
                limit :limit
                """
            ),
            {"nav_date": nav_day, "limit": limit},
        ).all()
        pending_codes = [row[0] for row in pending_rows]
        if not pending_codes:
            record_job_finish(connection, job_id, status="SUCCESS", total=0, success=0, failed=0, skipped=0)
            return {"status": "SUCCESS", "navDate": nav_day.isoformat(), "pending": 0, "synced": 0, "outbox": 0}

        daily_rows = {
            str(row["基金代码"]): row
            for _, row in ak.fund_open_fund_daily_em().iterrows()
            if str(row.get("基金代码")) in pending_codes
        }

        synced_codes: list[str] = []
        now = datetime.now()
        try:
            for fund_code in pending_codes:
                row = daily_rows.get(fund_code)
                if row is None:
                    continue
                unit_nav = safe_float(row.get("单位净值"))
                if math.isclose(unit_nav, 0.0):
                    continue
                upsert_fund_nav(
                    connection,
                    fund_code,
                    nav_day,
                    unit_nav,
                    safe_float(row.get("累计净值")),
                    safe_float(row.get("日增长率")),
                )
                upsert_fund_nav_growth(connection, fund_code, nav_day, unit_nav)
                connection.execute(
                    text(
                        """
                        update fund_nav_pending_keys
                        set status = 'SYNCED', updated_at = :updated_at
                        where fund_code = :fund_code and nav_date = :nav_date
                        """
                    ),
                    {"fund_code": fund_code, "nav_date": nav_day, "updated_at": now},
                )
                synced_codes.append(fund_code)

            outbox_count = create_outbox_batches(connection, nav_day, synced_codes)
            record_job_finish(
                connection,
                job_id,
                status="SUCCESS",
                total=len(pending_codes),
                success=len(synced_codes),
                failed=0,
                skipped=len(pending_codes) - len(synced_codes),
            )
            return {"status": "SUCCESS", "navDate": nav_day.isoformat(), "pending": len(pending_codes), "synced": len(synced_codes), "outbox": outbox_count}
        except Exception as exc:  # noqa: BLE001
            record_job_finish(
                connection,
                job_id,
                status="FAILED",
                total=len(pending_codes),
                success=len(synced_codes),
                failed=1,
                skipped=0,
                error=str(exc),
            )
            raise


def poll_evening_navs(engine: Engine, nav_day: date, limit: int | None = None) -> dict[str, Any]:
    """执行晚间主轮询。"""
    if not flag_enabled("python_fund_ingest_enabled"):
        return skipped_response("python_fund_ingest_enabled is disabled")
    run_key = f"{nav_day.isoformat()}#evening#{datetime.now().strftime('%H%M')}"
    return poll_nav_batch(engine, nav_day=nav_day, job_code="fund_nav_poll_evening", run_key=run_key, limit=limit or SETTINGS.pending_query_limit)


def poll_retry_navs(engine: Engine, nav_day: date, limit: int | None = None) -> dict[str, Any]:
    """执行前一交易日净值补偿轮询。"""
    if not flag_enabled("python_fund_backfill_enabled"):
        return skipped_response("python_fund_backfill_enabled is disabled")
    run_key = f"{nav_day.isoformat()}#retry#{datetime.now().strftime('%H%M')}"
    return poll_nav_batch(engine, nav_day=nav_day, job_code="fund_nav_poll_retry", run_key=run_key, limit=limit or SETTINGS.pending_query_limit)


def publish_outbox(engine: Engine, limit: int | None = None) -> dict[str, Any]:
    """扫描 outbox 并批量发布基金净值 ready 事件。"""
    if not flag_enabled("python_fund_publish_enabled"):
        return skipped_response("python_fund_publish_enabled is disabled")
    with engine.begin() as connection:
        job_id = record_job_start(
            connection,
            job_code="fund_nav_publish_outbox",
            job_source="AIRFLOW",
            job_type="INGEST",
            run_key=datetime.now().strftime("%Y-%m-%dT%H:%M"),
            payload_summary=json_summary({"limit": limit or SETTINGS.outbox_batch_size}),
        )
        if job_id is None:
            return skipped_response("already succeeded")
        rows = connection.execute(
            text(
                """
                select id, payload
                from outbox_events
                where status in ('PENDING', 'FAILED')
                order by created_at asc
                limit :limit
                """
            ),
            {"limit": limit or SETTINGS.outbox_batch_size},
        ).mappings().all()
        published = 0
        failed = 0
        for row in rows:
            try:
                publish_batch_message(json.loads(row["payload"]))
                connection.execute(
                    text("update outbox_events set status = 'PUBLISHED', published_at = :published_at where id = :id"),
                    {"id": row["id"], "published_at": datetime.now()},
                )
                published += 1
            except Exception:  # noqa: BLE001
                connection.execute(text("update outbox_events set status = 'FAILED' where id = :id"), {"id": row["id"]})
                failed += 1
        status = "FAILED" if failed else "SUCCESS"
        record_job_finish(connection, job_id, status=status, total=len(rows), success=published, failed=failed, skipped=0, error=None if not failed else "publish errors")
        return {"status": status, "published": published, "failed": failed}


def cleanup_pending_keys(engine: Engine) -> dict[str, Any]:
    """每周清理一次过老的基金待抓集合。"""
    cutoff = date.today() - timedelta(days=7)
    run_key = f"{date.today().isocalendar().year}-W{date.today().isocalendar().week}"
    with engine.begin() as connection:
        job_id = record_job_start(
            connection,
            job_code="fund_nav_pending_cleanup",
            job_source="AIRFLOW",
            job_type="MAINTENANCE",
            run_key=run_key,
            payload_summary=json_summary({"cutoff": cutoff.isoformat()}),
        )
        if job_id is None:
            return skipped_response("already succeeded")
        deleted = connection.execute(text("delete from fund_nav_pending_keys where nav_date < :cutoff"), {"cutoff": cutoff}).rowcount or 0
        record_job_finish(connection, job_id, status="SUCCESS", total=deleted, success=deleted, failed=0, skipped=0)
        return {"status": "SUCCESS", "deleted": deleted, "cutoff": cutoff.isoformat()}
