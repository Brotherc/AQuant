import os

# 单元测试与开发数据库隔离，避免测试启动时连接或建表到 MySQL。
os.environ["DATABASE_URL"] = "sqlite+aiosqlite:///:memory:"
