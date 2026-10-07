import asyncio

import httpx
from pydantic import ValidationError

from aquant_ai.core.config import Settings
from aquant_ai.portfolio_diagnosis.schema import BackendResponse, PortfolioDiagnosisContext


class AQuantBackendError(RuntimeError):
    """Java 主服务调用失败或返回不符合约定时抛出的异常。"""


class AQuantBackendClient:
    """访问 AQuant Java 主服务，并校验持仓会诊数据契约。"""

    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None):
        self._settings = settings
        self._owns_client = client is None
        self._client = client or httpx.AsyncClient(
            base_url=settings.aquant_backend_base_url.rstrip("/"),
            timeout=httpx.Timeout(
                connect=settings.aquant_backend_connect_timeout,
                read=settings.aquant_backend_read_timeout,
                write=settings.aquant_backend_read_timeout,
                pool=settings.aquant_backend_connect_timeout,
            ),
        )

    async def get_portfolio_diagnosis_context(
        self,
        portfolio_id: int,
        authorization: str,
    ) -> PortfolioDiagnosisContext:
        """转发用户 JWT，读取并严格校验指定组合的会诊快照。"""
        if not authorization.lower().startswith("bearer "):
            raise ValueError("authorization 必须是 Bearer Token")

        response: httpx.Response | None = None
        retry_count = 0
        while True:
            try:
                response = await self._client.get(
                    "/portfolio/diagnosis/context",
                    params={"portfolioId": portfolio_id},
                    headers={"Authorization": authorization},
                )
                if response.status_code not in {502, 503, 504}:
                    break
            except (httpx.ConnectError, httpx.ConnectTimeout, httpx.ReadTimeout) as exc:
                if retry_count >= self._settings.aquant_backend_max_retries:
                    raise AQuantBackendError("AQuant 主服务暂时不可用") from exc
            if retry_count >= self._settings.aquant_backend_max_retries:
                break
            retry_count += 1
            await asyncio.sleep(0.1 * retry_count)

        if response is None:
            raise AQuantBackendError("AQuant 主服务未返回响应")
        try:
            response.raise_for_status()
        except httpx.HTTPStatusError as exc:
            raise AQuantBackendError(
                f"AQuant 主服务请求失败，HTTP {response.status_code}"
            ) from exc
        try:
            payload = BackendResponse.model_validate(response.json())
        except (ValueError, ValidationError) as exc:
            raise AQuantBackendError("AQuant 主服务返回的会诊数据格式无效") from exc
        if not payload.success or payload.data is None:
            detail = payload.message or f"AQuant 主服务业务请求失败，code={payload.code}"
            raise AQuantBackendError(detail)
        return payload.data

    async def aclose(self) -> None:
        """仅关闭由当前实例创建的 HTTP 客户端。"""
        if self._owns_client:
            await self._client.aclose()

    async def __aenter__(self) -> "AQuantBackendClient":
        return self

    async def __aexit__(self, *_: object) -> None:
        await self.aclose()
