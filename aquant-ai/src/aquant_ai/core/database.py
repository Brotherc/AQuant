from collections.abc import AsyncIterator

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase

from aquant_ai.core.config import get_settings

settings = get_settings()


class Base(DeclarativeBase):
    """AQuant AI 所有 SQLAlchemy 实体的声明式基类。"""

    pass


# pool_pre_ping 会在复用连接前检查连接状态，减少数据库重启后的失效连接错误。
engine = create_async_engine(settings.database_url, pool_pre_ping=True)
async_session_factory = async_sessionmaker(engine, expire_on_commit=False)


async def get_db_session() -> AsyncIterator[AsyncSession]:
    """为单次 HTTP 请求提供独立的异步数据库会话。"""
    async with async_session_factory() as session:
        yield session
