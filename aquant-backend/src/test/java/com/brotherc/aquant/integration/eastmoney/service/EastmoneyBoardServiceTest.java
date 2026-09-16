package com.brotherc.aquant.integration.eastmoney.service;

import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EastmoneyBoardServiceTest {

    @Mock
    private EastmoneyQuoteGateway quoteGateway;

    @InjectMocks
    private EastmoneyBoardService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fetchBoardDetailParsesSnapshotFields() throws Exception {
        // 90.BK1201 电子 2026-09-14 收盘后的 stock/get 实测响应（截取用到的字段）
        String json = """
                {"rc":0,"data":{"f43":12878.49,"f44":12956.03,"f45":12464.95,"f46":12591.8,
                "f47":101849492,"f48":429962094959.0,"f49":51550720,"f50":0.98,"f57":"BK1201",
                "f58":"电子","f60":12730.57,"f85":366681878528.0,"f117":15683607296000.0,
                "f168":2.78,"f169":147.92,"f170":1.16,"f171":3.86}}
                """;
        when(quoteGateway.executeQuote(any())).thenReturn(objectMapper.readTree(json));

        EastmoneyBoardDetail detail = service.fetchBoardDetail("BK1201");

        assertThat(detail.getSectorCode()).isEqualTo("BK1201");
        assertThat(detail.getSectorName()).isEqualTo("电子");
        assertThat(detail.getLatestPrice()).isEqualByComparingTo("12878.49");
        assertThat(detail.getOpenPrice()).isEqualByComparingTo("12591.8");
        assertThat(detail.getPreClosePrice()).isEqualByComparingTo("12730.57");
        assertThat(detail.getHighPrice()).isEqualByComparingTo("12956.03");
        assertThat(detail.getLowPrice()).isEqualByComparingTo("12464.95");
        assertThat(detail.getChangeAmount()).isEqualByComparingTo("147.92");
        assertThat(detail.getChangePercent()).isEqualByComparingTo("1.16");
        assertThat(detail.getTurnoverRate()).isEqualByComparingTo("2.78");
        assertThat(detail.getVolumeRatio()).isEqualByComparingTo("0.98");
        assertThat(detail.getOuterDisc()).isEqualByComparingTo("51550720");
        assertThat(detail.getCirculatingMarketValue()).isEqualByComparingTo(new BigDecimal("15683607296000"));
        assertThat(detail.getCirculatingShares()).isEqualByComparingTo(new BigDecimal("366681878528"));
    }
}
