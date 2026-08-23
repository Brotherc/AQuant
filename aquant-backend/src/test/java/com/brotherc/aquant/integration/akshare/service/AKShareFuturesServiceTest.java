package com.brotherc.aquant.integration.akshare.service;

import com.brotherc.aquant.integration.akshare.model.FuturesDaily;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AKShareFuturesServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeFuturesDailyJsonCorrectly() throws Exception {
        String json = """
                [
                    {
                        "symbol": "PS2609",
                        "date": "20260821",
                        "open": 38800.0,
                        "high": 38865.0,
                        "low": 38020.0,
                        "close": 38280.0,
                        "volume": 15304,
                        "open_interest": 20172,
                        "turnover": 176620.9725,
                        "settle": 38465.0,
                        "pre_settle": 37765.0,
                        "variety": "PS"
                    }
                ]
                """;

        List<FuturesDaily> list = objectMapper.readValue(json, new TypeReference<>() {});

        assertThat(list).hasSize(1);
        FuturesDaily item = list.get(0);
        assertThat(item.getSymbol()).isEqualTo("PS2609");
        assertThat(item.getDate()).isEqualTo("20260821");
        assertThat(item.getOpen()).isEqualByComparingTo("38800.0");
        assertThat(item.getHigh()).isEqualByComparingTo("38865.0");
        assertThat(item.getLow()).isEqualByComparingTo("38020.0");
        assertThat(item.getClose()).isEqualByComparingTo("38280.0");
        assertThat(item.getVolume()).isEqualByComparingTo("15304");
        assertThat(item.getOpenInterest()).isEqualByComparingTo("20172");
        assertThat(item.getTurnover()).isEqualByComparingTo("176620.9725");
        assertThat(item.getSettle()).isEqualByComparingTo("38465.0");
        assertThat(item.getPreSettle()).isEqualByComparingTo("37765.0");
        assertThat(item.getVariety()).isEqualTo("PS");
    }

    @Test
    void shouldDeserializeFuturesMainSinaJsonCorrectly() throws Exception {
        String json = """
                [
                    {
                        "日期": "2026-08-21T00:00:00.000",
                        "开盘价": 152400,
                        "最高价": 159380,
                        "最低价": 151180,
                        "收盘价": 158680,
                        "成交量": 225444,
                        "持仓量": 353437,
                        "动态结算价": 156360
                    }
                ]
                """;

        List<com.brotherc.aquant.integration.akshare.model.FuturesMainSina> list = objectMapper.readValue(
                json, new TypeReference<>() {}
        );

        assertThat(list).hasSize(1);
        com.brotherc.aquant.integration.akshare.model.FuturesMainSina item = list.get(0);
        assertThat(item.getDate()).isEqualTo("2026-08-21T00:00:00.000");
        assertThat(item.getOpen()).isEqualByComparingTo("152400");
        assertThat(item.getHigh()).isEqualByComparingTo("159380");
        assertThat(item.getLow()).isEqualByComparingTo("151180");
        assertThat(item.getClose()).isEqualByComparingTo("158680");
        assertThat(item.getVolume()).isEqualByComparingTo("225444");
        assertThat(item.getOpenInterest()).isEqualByComparingTo("353437");
        assertThat(item.getDynamicSettle()).isEqualByComparingTo("156360");
    }

    @Test
    void shouldDeserializeFuturesSpotPriceDailyJsonCorrectly() throws Exception {
        String json = """
                [
                    {
                        "date": "20240415",
                        "symbol": "C",
                        "spot_price": 2318.57,
                        "near_contract": "c2405",
                        "near_contract_price": 2392.0,
                        "dominant_contract": "c2405",
                        "dominant_contract_price": 2392.0,
                        "near_month": "2405",
                        "dominant_month": "2405",
                        "near_basis": 73.43,
                        "dom_basis": 73.43,
                        "near_basis_rate": 0.031670383,
                        "dom_basis_rate": 0.031670383
                    }
                ]
                """;

        List<com.brotherc.aquant.integration.akshare.model.FuturesSpotPriceDaily> list = objectMapper.readValue(
                json, new TypeReference<>() {}
        );

        assertThat(list).hasSize(1);
        com.brotherc.aquant.integration.akshare.model.FuturesSpotPriceDaily item = list.get(0);
        assertThat(item.getDate()).isEqualTo("20240415");
        assertThat(item.getSymbol()).isEqualTo("C");
        assertThat(item.getSpotPrice()).isEqualByComparingTo("2318.57");
        assertThat(item.getNearContract()).isEqualTo("c2405");
        assertThat(item.getNearContractPrice()).isEqualByComparingTo("2392.0");
        assertThat(item.getDominantContract()).isEqualTo("c2405");
        assertThat(item.getDominantContractPrice()).isEqualByComparingTo("2392.0");
        assertThat(item.getNearMonth()).isEqualTo("2405");
        assertThat(item.getDominantMonth()).isEqualTo("2405");
        assertThat(item.getNearBasis()).isEqualByComparingTo("73.43");
        assertThat(item.getDomBasis()).isEqualByComparingTo("73.43");
        assertThat(item.getNearBasisRate()).isEqualByComparingTo("0.031670383");
        assertThat(item.getDomBasisRate()).isEqualByComparingTo("0.031670383");
    }
}
