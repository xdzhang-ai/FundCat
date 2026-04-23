"""向 RocketMQ 手动发送一条 fund_nav_ready_batch 测试消息。

用途：
1. 在 Python 侧验证 producer 配置是否可用；
2. 配合 Java 本地服务日志，验证 RocketMQ -> Java consumer -> 业务确认链路是否打通；
3. 不依赖 Airflow，可作为最小联调入口。
"""

from __future__ import annotations

import argparse
from datetime import datetime
import json
from pathlib import Path
import sys
import uuid

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from fund_data_service.common import SETTINGS, publish_batch_message


def build_parser() -> argparse.ArgumentParser:
    """构造命令行参数解析器。"""
    parser = argparse.ArgumentParser(description="Publish a fund_nav_ready_batch test message to RocketMQ")
    parser.add_argument("--nav-date", required=True, help="消息里的净值日期，例如 2026-04-22")
    parser.add_argument("--fund-code", action="append", dest="fund_codes", required=True, help="基金代码，可重复传入多次")
    return parser


def main() -> None:
    """读取命令行参数并发送一条批量基金 ready 测试消息。"""
    parser = build_parser()
    args = parser.parse_args()
    payload = {
        "eventId": str(uuid.uuid4()),
        "navDate": args.nav_date,
        "fundCodes": args.fund_codes,
        "count": len(args.fund_codes),
        "publishedAt": datetime.now().isoformat(),
    }
    print(f"publish_mode={SETTINGS.publish_mode}")
    print(f"rocketmq_topic={SETTINGS.rocketmq_topic}")
    print("payload=" + json.dumps(payload, ensure_ascii=False))
    publish_batch_message(payload)
    print("message published successfully")


if __name__ == "__main__":
    main()
