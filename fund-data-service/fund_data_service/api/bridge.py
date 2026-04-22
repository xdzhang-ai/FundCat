"""AKShare bridge HTTP 接口。供Java侧调用"""

from __future__ import annotations

from fastapi import APIRouter

from fund_data_service.service.bridge_service import get_acc_nav_history, get_unit_nav_history

router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    """健康检查接口。"""
    return {"status": "ok"}


@router.get("/funds/{code}/unit-nav-history")
def fund_unit_nav_history(code: str) -> list[dict[str, object]]:
    """返回单只基金单位净值历史。"""
    return get_unit_nav_history(code)


@router.get("/funds/{code}/acc-nav-history")
def fund_acc_nav_history(code: str) -> list[dict[str, object]]:
    """返回单只基金累计净值历史。"""
    return get_acc_nav_history(code)
