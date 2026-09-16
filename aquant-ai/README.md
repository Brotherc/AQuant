# AQuant AI

AQuant 的独立 Python AI 服务脚手架，后续用于承载固定持仓会诊 Workflow、对话式问答和 Agent 工具编排。

## 技术栈

- Python 3.11+
- FastAPI
- Pydantic / Pydantic Settings
- OpenAI Agents SDK
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

`OPENAI_API_KEY` 和 `OPENAI_MODEL` 当前可以留空；接入 Agent 业务前再配置即可。

## 质量检查

```bash
pytest
ruff check .
```

## 目录说明

```text
src/aquant_ai/
├── agents/       # Agent 定义与编排
├── api/          # FastAPI 路由
├── clients/      # Java 后端及外部服务客户端
├── core/         # 配置和基础设施
├── schemas/      # Pydantic 请求与响应模型
├── workflows/    # 固定 AI Workflow
└── main.py       # 应用入口
```
