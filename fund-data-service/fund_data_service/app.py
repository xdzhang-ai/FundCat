"""FastAPI 应用组装入口。"""

from __future__ import annotations

from fastapi import FastAPI

from fund_data_service.api.bridge import router as bridge_router
from fund_data_service.api.ops import router as ops_router


def create_app() -> FastAPI:
    """创建并注册全部路由。"""
    application = FastAPI(title="FundCat AKShare Bridge", version="0.2.0")
    application.include_router(bridge_router)
    application.include_router(ops_router)
    return application


app = create_app()
