"""数据采集与消息发布 HTTP 接口，主要供 Airflow 或运维脚本调度。

推荐执行顺序如下：
1. `/ops/funds/sync-basics`
   先同步基金基础索引，保证 `funds` 表里有完整基金清单。
   调度频率：低频，可按每日一次或按需人工触发。
   大致时间点：建议放在日间较早时段，例如 08:30 ~ 09:00。
   数据量级：通常是全市场约 1 万到 1.5 万只基金，属于低频全量元数据同步。
2. `/ops/nav/seed`
   再为当天生成全量基金待抓集合，写入 `fund_nav_pending_keys`。
   调度频率：每日一次。
   大致时间点：09:00。
   数据量级：每天约 1 万到 1.5 万条 pending 记录，属于轻量中间表初始化。
3. `/ops/nav/poll-evening`
   晚间按批轮询当天尚未拿到净值的基金，成功后写净值事实表和 growth 表，并创建 outbox。
   调度频率：晚间固定多次执行。
   大致时间点：18:15、20:15、22:15。
   数据量级：单轮通常处理几百到几千只尚未完成基金，整晚累计覆盖全量基金。
4. `/ops/outbox/publish`
   把上一阶段已经落库的 ready 事件批量发到消息系统，通知 Java 后台开始业务确认与回算。
   调度频率：与晚间主轮询和次日补偿轮询配套执行，可在每轮抓取后追加一次。
   大致时间点：18:20、20:20、22:20，以及次日补偿后。
   数据量级：每批建议 100 到 500 只基金，整晚通常是几十到几百个 batch 事件。
5. `/ops/nav/poll-retry`
   次日早盘补偿前一交易日仍未完成的基金净值抓取。
   调度频率：每日两次补偿。
   大致时间点：次日 07:30、09:00。
   数据量级：正常情况下应明显小于晚间主轮询，通常只剩尾部未完成基金。
6. `/ops/nav/pending/cleanup`
   每周清理 7 天前的待抓中间数据，避免 pending 表持续堆积。
   调度频率：每周一次。
   大致时间点：建议周末凌晨或低峰时段。
   数据量级：按 7 天窗口估算，大约是 7 万到 10 万级中间记录清理。
"""

from __future__ import annotations

from datetime import date, timedelta

from fastapi import APIRouter

from fund_data_service.common import PublishRequest, TaskRequest
from fund_data_service.db import create_db_engine
from fund_data_service.service.ingestion_service import cleanup_pending_keys, poll_evening_navs, poll_retry_navs, publish_outbox, seed_nav_pending, sync_fund_basics

router = APIRouter(prefix="/ops")


@router.post("/funds/sync-basics")
def sync_funds() -> dict[str, object]:
    """步骤 1：全量同步基金基础索引，为后续 seed 和净值轮询准备基金主数据。

    调度频率：低频，可每日一次或按需人工触发。
    建议时间：08:30 ~ 09:00。
    量级参考：通常处理全市场约 1 万到 1.5 万只基金。
    """
    return sync_fund_basics(create_db_engine())


@router.post("/nav/seed")
def seed_navs(request: TaskRequest) -> dict[str, object]:
    """步骤 2：生成当天全量基金待抓集合，但不向净值事实表插入占位数据。

    调度频率：每日一次。
    建议时间：09:00。
    量级参考：每天约新增 1 万到 1.5 万条待抓键。
    """
    return seed_nav_pending(create_db_engine(), request.nav_date or date.today())


@router.post("/nav/poll-evening")
def poll_evening(request: TaskRequest) -> dict[str, object]:
    """步骤 3：执行晚间主轮询，抓取当天净值并把成功结果写入 outbox。

    调度频率：晚间固定多次执行。
    建议时间：18:15、20:15、22:15。
    量级参考：单轮通常是几百到几千只基金，整晚累计覆盖当天全量基金。
    """
    return poll_evening_navs(create_db_engine(), request.nav_date or date.today(), request.limit)


@router.post("/outbox/publish")
def publish(request: PublishRequest) -> dict[str, object]:
    """步骤 4：把待发 outbox 事件批量发送到消息系统，驱动 Java 后台消费。

    调度频率：与轮询任务配套执行，可在每轮抓取后立即触发一次。
    建议时间：18:20、20:20、22:20，以及次日补偿完成后。
    量级参考：建议 100 到 500 只基金聚成一个 batch 事件。
    """
    return publish_outbox(create_db_engine(), request.limit)


@router.post("/nav/poll-retry")
def poll_retry(request: TaskRequest) -> dict[str, object]:
    """步骤 5：执行次日早盘补偿轮询，补齐前一交易日遗漏的基金净值。

    调度频率：每日两次补偿。
    建议时间：次日 07:30、09:00。
    量级参考：正常情况下只处理晚间主轮询剩余的小部分尾部基金。
    """
    return poll_retry_navs(create_db_engine(), request.nav_date or (date.today() - timedelta(days=1)), request.limit)


@router.post("/nav/pending/cleanup")
def cleanup() -> dict[str, object]:
    """步骤 6：清理 7 天前的待抓中间数据，控制中间表体量。

    调度频率：每周一次。
    建议时间：周末凌晨或低峰时段。
    量级参考：7 天窗口下通常是 7 万到 10 万级中间记录。
    """
    return cleanup_pending_keys(create_db_engine())
