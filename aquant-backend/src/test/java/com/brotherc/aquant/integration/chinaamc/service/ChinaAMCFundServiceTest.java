package com.brotherc.aquant.integration.chinaamc.service;

import com.brotherc.aquant.integration.chinaamc.model.ChinaAMCFundPurchaseLimit;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChinaAMCFundServiceTest {

    private final ChinaAMCFundService service = new ChinaAMCFundService(null);

    @Test
    void shouldParseCurrentNasdaq100StatusForAllShares() {
        String html = """
                <table><tbody>
                  <tr><td>015299</td><td>华夏纳斯达克100ETF发起式联接（QDII）A（人民币）</td>
                    <td>暂停</td><td>开放</td><td>未开放</td><td>未开放</td><td>暂停</td><td>-</td></tr>
                  <tr><td>015300</td><td>华夏纳斯达克100ETF发起式联接（QDII）C</td>
                    <td>暂停</td><td>开放</td><td>未开放</td><td>未开放</td><td>暂停</td><td>-</td></tr>
                  <tr><td>015518</td><td>华夏纳斯达克100ETF发起式联接（QDII）A（美元现汇）</td>
                    <td>暂停</td><td>开放</td><td>未开放</td><td>未开放</td><td>暂停</td><td>-</td></tr>
                </tbody></table>
                """;

        List<ChinaAMCFundPurchaseLimit> limits = service.parsePurchaseLimits(
                html.getBytes(StandardCharsets.UTF_8)
        );

        assertThat(limits).hasSize(3).allSatisfy(limit -> {
            assertThat(limit.getSalesChannel()).isEqualTo("DIRECT");
            assertThat(limit.getPurchaseStatus()).isEqualTo("SUSPENDED");
            assertThat(limit.getRecurringStatus()).isEqualTo("SUSPENDED");
            assertThat(limit.getPurchaseLimitAmount()).isNull();
            assertThat(limit.getRecurringLimitAmount()).isNull();
        });
        assertThat(limits).extracting(ChinaAMCFundPurchaseLimit::getFundCode)
                .containsExactly("015299", "015300", "015518");
        assertThat(limits).extracting(ChinaAMCFundPurchaseLimit::getCurrency)
                .containsExactly("CNY", "CNY", "USD");
    }

    @Test
    void shouldParseLimitedAmountWithoutTreatingOpenMinimumAsLimit() {
        String html = """
                <table><tbody>
                  <tr><td>015299</td><td>华夏纳斯达克100ETF发起式联接（QDII）A（人民币）</td>
                    <td>开放-有限制</td><td>开放</td><td>未开放</td><td>未开放</td><td>开放</td>
                    <td>单日累计申购金额不超过100万元，定投每次最低10元</td></tr>
                </tbody></table>
                """;

        ChinaAMCFundPurchaseLimit limit = service.parsePurchaseLimits(html.getBytes(StandardCharsets.UTF_8)).get(0);

        assertThat(limit.getPurchaseStatus()).isEqualTo("LIMITED");
        assertThat(limit.getPurchaseLimitAmount()).isEqualByComparingTo("1000000");
        assertThat(limit.getRecurringStatus()).isEqualTo("OPEN");
    }

}
