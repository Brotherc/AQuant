package com.brotherc.aquant.integration.js.service;

import com.brotherc.aquant.integration.js.model.JSFundPurchaseLimit;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JSFundServiceTest {

    private final JSFundService service = new JSFundService(null);

    @Test
    void shouldParseCurrentNasdaq100LimitsForAllShares() {
        String html = """
                <table><tbody>
                  <tr>
                    <td>016532/016533/<br/>021838</td>
                    <td>嘉实纳斯达克100ETF发起联接（QDII）A/C/I人民币</td>
                    <td>暂停申购</td><td>暂未开通</td><td>暂停定投</td><td>2026/2/3</td>
                  </tr>
                  <tr>
                    <td>016534/016535</td>
                    <td>嘉实纳斯达克100ETF发起联接（QDII）A/C美元现汇</td>
                    <td>暂停申购</td><td>暂未开通</td><td>暂停定投</td><td>2026/2/3</td>
                  </tr>
                </tbody></table>
                """;

        List<JSFundPurchaseLimit> limits = service.parsePurchaseLimits(html.getBytes(StandardCharsets.UTF_8));

        assertThat(limits).hasSize(5).allSatisfy(limit -> {
            assertThat(limit.getSalesChannel()).isEqualTo("ALL_CHANNELS");
            assertThat(limit.getPurchaseStatus()).isEqualTo("SUSPENDED");
            assertThat(limit.getRecurringStatus()).isEqualTo("SUSPENDED");
            assertThat(limit.getPurchaseLimitAmount()).isNull();
            assertThat(limit.getRecurringLimitAmount()).isNull();
            assertThat(limit.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 2, 3));
        });
        assertThat(limits).extracting(JSFundPurchaseLimit::getFundCode)
                .containsExactly("016532", "016533", "021838", "016534", "016535");
        assertThat(limits.subList(0, 3)).allSatisfy(limit -> assertThat(limit.getCurrency()).isEqualTo("CNY"));
        assertThat(limits.subList(3, 5)).allSatisfy(limit -> assertThat(limit.getCurrency()).isEqualTo("USD"));
    }

    @Test
    void shouldParseLimitedAmount() {
        String html = """
                <table><tbody><tr>
                  <td>016532/016533/021838</td>
                  <td>嘉实纳斯达克100ETF发起联接（QDII）A/C/I人民币</td>
                  <td>100元</td><td>暂未开通</td><td>100元</td><td>2026/8/29</td>
                </tr></tbody></table>
                """;

        List<JSFundPurchaseLimit> limits = service.parsePurchaseLimits(html.getBytes(StandardCharsets.UTF_8));

        assertThat(limits).hasSize(3).allSatisfy(limit -> {
            assertThat(limit.getPurchaseStatus()).isEqualTo("LIMITED");
            assertThat(limit.getPurchaseLimitAmount()).isEqualByComparingTo("100");
            assertThat(limit.getRecurringStatus()).isEqualTo("LIMITED");
            assertThat(limit.getRecurringLimitAmount()).isEqualByComparingTo("100");
        });
    }

}
