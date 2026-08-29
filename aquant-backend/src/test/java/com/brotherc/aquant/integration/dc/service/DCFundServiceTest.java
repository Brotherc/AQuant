package com.brotherc.aquant.integration.dc.service;

import com.brotherc.aquant.integration.dc.model.DCFundPurchaseLimit;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DCFundServiceTest {

    private final DCFundService service = new DCFundService(null);

    @Test
    void shouldParseCurrentDirectLimitForBothShares() {
        String html = """
                <ul class="fund_list">
                  <li data-value-fund_name="大成纳斯达克100ETF联接（QDII）A"
                      data-value-fund_code="000834">
                    <p class="p1">A/C各限额100元，支持日定投</p>
                    <p class="p2">大成纳斯达克100ETF联接（QDII）A</p>
                  </li>
                </ul>
                """;

        List<DCFundPurchaseLimit> limits = service.parsePurchaseLimits(
                html.getBytes(StandardCharsets.UTF_8)
        );

        assertThat(limits).hasSize(2).allSatisfy(limit -> {
            assertThat(limit.getCurrency()).isEqualTo("CNY");
            assertThat(limit.getSalesChannel()).isEqualTo("DIRECT");
            assertThat(limit.getPurchaseStatus()).isEqualTo("LIMITED");
            assertThat(limit.getPurchaseLimitAmount()).isEqualByComparingTo("100");
            assertThat(limit.getRecurringStatus()).isEqualTo("LIMITED");
            assertThat(limit.getRecurringLimitAmount()).isEqualByComparingTo("100");
        });
        assertThat(limits).extracting(DCFundPurchaseLimit::getFundCode)
                .containsExactly("000834", "008971");
    }

    @Test
    void shouldSupportAmountUnit() {
        String html = """
                <li data-value-fund_name="大成纳斯达克100ETF联接（QDII）A"
                    data-value-fund_code="000834">
                  <p class="p1">A/C各限额1万元，支持日定投</p>
                </li>
                """;

        List<DCFundPurchaseLimit> limits = service.parsePurchaseLimits(
                html.getBytes(StandardCharsets.UTF_8)
        );

        assertThat(limits).extracting(DCFundPurchaseLimit::getPurchaseLimitAmount)
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("10000"));
    }

    @Test
    void shouldRejectChangedPageInsteadOfReturningIncompleteRules() {
        String html = """
                <li data-value-fund_name="大成纳斯达克100ETF联接（QDII）A"
                    data-value-fund_code="000834">
                  <p class="p1">额度规则请查看公告</p>
                </li>
                """;

        assertThatThrownBy(() -> service.parsePurchaseLimits(html.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法识别大成纳指100直销额度");
    }

}
