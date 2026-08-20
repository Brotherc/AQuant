package com.brotherc.aquant.integration.akshare.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StockZcfzEmTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should correctly deserialize AKShare stock_zcfz_em JSON response")
    void shouldDeserializeStockZcfzEmJson() throws Exception {
        String json = """
                [{
                    "序号": 1,
                    "股票代码": "688609",
                    "股票简称": "九联科技",
                    "资产-货币资金": 220421574.88,
                    "资产-应收账款": 843716196.37,
                    "资产-存货": 827464086.74,
                    "资产-总资产": 3771689158.17,
                    "资产-总资产同比": 14.9855777823,
                    "负债-应付账款": 1185021910.59,
                    "负债-预收账款": null,
                    "负债-总负债": 2803733873.9,
                    "负债-总负债同比": 14.1312985272,
                    "资产负债率": 74.3362922108,
                    "股东权益合计": 967955284.27,
                    "公告日期": "2026-08-12T00:00:00.000"
                }]
                """;

        List<StockZcfzEm> result = objectMapper.readValue(json, new TypeReference<>() {});

        assertThat(result).hasSize(1);
        StockZcfzEm item = result.get(0);
        assertThat(item.getSeq()).isEqualTo(1);
        assertThat(item.getStockCode()).isEqualTo("688609");
        assertThat(item.getStockName()).isEqualTo("九联科技");
        assertThat(item.getAssetMonetaryFunds()).isEqualByComparingTo(new BigDecimal("220421574.88"));
        assertThat(item.getAssetAccountsReceivable()).isEqualByComparingTo(new BigDecimal("843716196.37"));
        assertThat(item.getAssetInventory()).isEqualByComparingTo(new BigDecimal("827464086.74"));
        assertThat(item.getAssetTotalAssets()).isEqualByComparingTo(new BigDecimal("3771689158.17"));
        assertThat(item.getAssetTotalAssetsYoY()).isEqualByComparingTo(new BigDecimal("14.9855777823"));
        assertThat(item.getLiabilityAccountsPayable()).isEqualByComparingTo(new BigDecimal("1185021910.59"));
        assertThat(item.getLiabilityAdvanceReceipts()).isNull();
        assertThat(item.getLiabilityTotalLiabilities()).isEqualByComparingTo(new BigDecimal("2803733873.9"));
        assertThat(item.getLiabilityTotalLiabilitiesYoY()).isEqualByComparingTo(new BigDecimal("14.1312985272"));
        assertThat(item.getAssetLiabilityRatio()).isEqualByComparingTo(new BigDecimal("74.3362922108"));
        assertThat(item.getTotalEquity()).isEqualByComparingTo(new BigDecimal("967955284.27"));
        assertThat(item.getNoticeDate()).isEqualTo("2026-08-12T00:00:00.000");
    }

    @Test
    @DisplayName("Should correctly deserialize AKShare stock_zcfz_bj_em JSON response")
    void shouldDeserializeStockZcfzBjEmJson() throws Exception {
        String json = """
                [{
                    "序号": 1,
                    "股票代码": "920703",
                    "股票简称": "广厦环能",
                    "资产-货币资金": 650850572.03,
                    "资产-应收账款": 205345721.1,
                    "资产-存货": 101050621.71,
                    "资产-总资产": 1435081668.5,
                    "资产-总资产同比": 11.4599829262,
                    "负债-应付账款": 117675704.61,
                    "负债-预收账款": null,
                    "负债-总负债": 295128281.24,
                    "负债-总负债同比": 32.9289528433,
                    "资产负债率": 20.5652603415,
                    "股东权益合计": 1139953387.26,
                    "公告日期": "2026-08-11T00:00:00.000"
                }]
                """;

        List<StockZcfzEm> result = objectMapper.readValue(json, new TypeReference<>() {});

        assertThat(result).hasSize(1);
        StockZcfzEm item = result.get(0);
        assertThat(item.getSeq()).isEqualTo(1);
        assertThat(item.getStockCode()).isEqualTo("920703");
        assertThat(item.getStockName()).isEqualTo("广厦环能");
        assertThat(item.getAssetMonetaryFunds()).isEqualByComparingTo(new BigDecimal("650850572.03"));
        assertThat(item.getAssetAccountsReceivable()).isEqualByComparingTo(new BigDecimal("205345721.1"));
        assertThat(item.getAssetInventory()).isEqualByComparingTo(new BigDecimal("101050621.71"));
        assertThat(item.getAssetTotalAssets()).isEqualByComparingTo(new BigDecimal("1435081668.5"));
        assertThat(item.getAssetTotalAssetsYoY()).isEqualByComparingTo(new BigDecimal("11.4599829262"));
        assertThat(item.getLiabilityAccountsPayable()).isEqualByComparingTo(new BigDecimal("117675704.61"));
        assertThat(item.getLiabilityAdvanceReceipts()).isNull();
        assertThat(item.getLiabilityTotalLiabilities()).isEqualByComparingTo(new BigDecimal("295128281.24"));
        assertThat(item.getLiabilityTotalLiabilitiesYoY()).isEqualByComparingTo(new BigDecimal("32.9289528433"));
        assertThat(item.getAssetLiabilityRatio()).isEqualByComparingTo(new BigDecimal("20.5652603415"));
        assertThat(item.getTotalEquity()).isEqualByComparingTo(new BigDecimal("1139953387.26"));
        assertThat(item.getNoticeDate()).isEqualTo("2026-08-11T00:00:00.000");
    }
}
