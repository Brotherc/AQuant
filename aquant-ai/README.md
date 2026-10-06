# AQuant AI

AQuant 的独立 Python AI 服务脚手架，后续用于承载固定持仓会诊 Workflow、对话式问答和 Agent 工具编排。

## 技术栈

- Python 3.11+
- FastAPI
- Pydantic / Pydantic Settings
- OpenAI Agents SDK
- SQLAlchemy
- AES-GCM 模型密钥加密
- Pytest / Ruff

## 本地启动

### macOS / Linux

```bash
cd aquant-ai
python3.11 -m venv .venv
source .venv/bin/activate
pip install -e ".[dev]"
cp .env.example .env
uvicorn aquant_ai.main:app --reload --host 0.0.0.0 --port 8085
```

### Windows PowerShell

```powershell
cd aquant-ai
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -e ".[dev]"
Copy-Item .env.example .env
uvicorn aquant_ai.main:app --reload --host 0.0.0.0 --port 8085
```

如果 PowerShell 禁止执行激活脚本，可以仅对当前窗口临时放开：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\.venv\Scripts\Activate.ps1
```

使用完毕后，两个平台都可以执行 `deactivate` 退出虚拟环境。关闭当前终端窗口也会自动退出。

启动后可以访问：

- 健康检查：`http://localhost:8085/api/health`
- OpenAPI 文档：`http://localhost:8085/docs`

## 模型配置

模型配置由 `aquant-ai` 独立保存。启动前至少需要在 `.env` 中配置：

```dotenv
# 与 aquant-backend 的 jwt.secret 保持一致，用于识别当前登录用户
AQUANT_JWT_SECRET=replace-with-backend-jwt-secret

# 普通字符串形式的 AES-256 密钥，按 UTF-8 编码后必须正好为 32 字节
MODEL_CREDENTIAL_MASTER_KEY=AQuant@2025!ModelCredentialKey#1

# 与 Java 服务连接同一个 MySQL 数据库
DATABASE_URL=mysql+asyncmy://test:testtest@127.0.0.1:3306/nocobase?charset=utf8mb4
```

生产环境可以使用下面的命令生成由 32 个可打印字符组成的随机主密钥：

```bash
python -c "import secrets,string; chars=string.ascii_letters+string.digits; print(''.join(secrets.choice(chars) for _ in range(32)))"
```

主密钥一旦用于加密模型 API Key 就不能随意更换，否则已有密文将无法解密。

生产环境请通过环境变量替换真实账号和密码，例如：

```dotenv
DATABASE_URL=mysql+asyncmy://user:password@127.0.0.1:3306/nocobase?charset=utf8mb4
```

服务不会自动创建或修改数据库表，使用模型配置功能前需要手工创建所需表。登录
AQuant 后，可从右上角用户头像菜单进入“模型配置”，添加供应商、模型、能力标记并测试连接。
API Key 使用 AES-GCM 加密保存，接口只返回末四位提示，不回显明文。

前端开发环境通过 `/ai-api` 代理访问 `http://localhost:8085/api`，因此使用模型配置页面时
需要同时启动 `aquant-backend`、`aquant-ai` 和 `aquant-frontend`。

生产环境需要由网关或 Nginx 将浏览器的 `/ai-api/*` 请求转发到 `aquant-ai:8085/api/*`。
模型配置相关表结构发生变化时，需要配套执行手工 DDL。

## 质量检查

```bash
pytest
ruff check .
```

## 目录说明

```text
src/aquant_ai/
├── agents/       # 跨业务 Agent 定义与编排
├── api/          # 公共 API 路由装配
├── clients/      # Java 后端及外部服务客户端
├── core/         # 配置和基础设施
├── health/       # 健康检查业务
├── model_config/ # 模型配置业务的路由、服务、实体、Schema 和运行时工厂
├── workflows/    # 固定 AI Workflow
└── main.py       # 应用入口
```
