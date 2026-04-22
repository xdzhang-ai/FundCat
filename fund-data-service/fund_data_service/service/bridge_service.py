"""AKShare bridge 查询能力。"""

from __future__ import annotations

from typing import Any

import akshare as ak
import pandas as pd
from fastapi import HTTPException


def normalize_value(value: Any) -> Any:
    """把 pandas / numpy / Timestamp 等对象转成可 JSON 序列化的普通值。"""
    if pd.isna(value):
        return None
    if isinstance(value, pd.Timestamp):
        return value.strftime("%Y-%m-%d")
    if hasattr(value, "item"):
        return value.item()
    return value


def dataframe_to_records(df: pd.DataFrame) -> list[dict[str, Any]]:
    """统一清洗 DataFrame 行记录。"""
    return [{str(key): normalize_value(value) for key, value in row.items()} for row in df.to_dict(orient="records")]


def get_unit_nav_history(code: str) -> list[dict[str, Any]]:
    """读取单只基金单位净值历史。"""
    df = ak.fund_open_fund_info_em(symbol=code, indicator="单位净值走势")
    if df.empty:
        raise HTTPException(status_code=404, detail=f"Unit nav history for fund {code} was not found")
    return dataframe_to_records(df)


def get_acc_nav_history(code: str) -> list[dict[str, Any]]:
    """读取单只基金累计净值历史。"""
    df = ak.fund_open_fund_info_em(symbol=code, indicator="累计净值走势")
    if df.empty:
        raise HTTPException(status_code=404, detail=f"Accumulated nav history for fund {code} was not found")
    return dataframe_to_records(df)
