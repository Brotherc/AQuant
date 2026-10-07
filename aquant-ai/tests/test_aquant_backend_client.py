import asyncio

import httpx
import pytest

from aquant_ai.core.config import Settings
from aquant_ai.portfolio_diagnosis.client import AQuantBackendClient, AQuantBackendError


def diagnosis_response() -> dict:
    return {
        "success": True,
        "code": 0,
        "message": None,
        "data": {
            "schemaVersion": "1.0",
            "generatedAt": "2026-10-06T10:00:00",
            "dataAsOf": "2026-10-06",
            "tradeRangeStart": "2025-10-06",
            "tradeLimit": 500,
            "portfolio": {
                "id": 5,
                "name": "长期组合",
                "baseCurrency": "CNY",
                "benchmarkCode": None,
            },
            "overview": {
                "portfolioId": 5,
                "portfolioName": "长期组合",
                "baseCurrency": "CNY",
                "cashAmount": 1000,
                "marketValue": 0,
                "totalAsset": 1000,
                "costAmount": 0,
                "unrealizedProfit": 0,
                "unrealizedProfitRate": None,
                "positionCount": 0,
                "unpricedAssetCount": 0,
                "unsupportedCurrencyCount": 0,
                "calculateTime": "2026-10-06T10:00:00",
                "positions": [],
            },
            "accounts": [],
            "cashBalances": [],
            "positions": [],
            "assetSnapshots": [],
            "recentTrades": [],
            "dataQuality": {
                "latestQuoteDate": None,
                "missingPriceAssetCodes": [],
                "stalePriceAssetCodes": [],
                "unsupportedCurrencies": [],
                "warnings": ["当前组合没有持仓数据"],
            },
        },
    }


def test_client_forwards_authorization_and_validates_context() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.headers["Authorization"] == "Bearer user-token"
        assert request.url.params["portfolioId"] == "5"
        return httpx.Response(200, json=diagnosis_response())

    http_client = httpx.AsyncClient(
        base_url="http://backend.test",
        transport=httpx.MockTransport(handler),
    )
    client = AQuantBackendClient(Settings(aquant_backend_max_retries=0), http_client)

    result = asyncio.run(client.get_portfolio_diagnosis_context(5, "Bearer user-token"))

    assert result.portfolio.name == "长期组合"
    assert result.overview.total_asset == 1000
    assert result.data_quality.warnings == ["当前组合没有持仓数据"]
    asyncio.run(http_client.aclose())


def test_client_rejects_invalid_backend_contract() -> None:
    http_client = httpx.AsyncClient(
        base_url="http://backend.test",
        transport=httpx.MockTransport(
            lambda _: httpx.Response(200, json={"success": True, "code": 0, "data": {}})
        ),
    )
    client = AQuantBackendClient(Settings(aquant_backend_max_retries=0), http_client)

    with pytest.raises(AQuantBackendError, match="数据格式无效"):
        asyncio.run(client.get_portfolio_diagnosis_context(5, "Bearer user-token"))
    asyncio.run(http_client.aclose())


def test_client_retries_a_temporary_backend_failure() -> None:
    request_count = 0

    def handler(_: httpx.Request) -> httpx.Response:
        nonlocal request_count
        request_count += 1
        if request_count == 1:
            return httpx.Response(503)
        return httpx.Response(200, json=diagnosis_response())

    http_client = httpx.AsyncClient(
        base_url="http://backend.test",
        transport=httpx.MockTransport(handler),
    )
    client = AQuantBackendClient(Settings(aquant_backend_max_retries=1), http_client)

    result = asyncio.run(client.get_portfolio_diagnosis_context(5, "Bearer user-token"))

    assert result.portfolio.id == 5
    assert request_count == 2
    asyncio.run(http_client.aclose())
