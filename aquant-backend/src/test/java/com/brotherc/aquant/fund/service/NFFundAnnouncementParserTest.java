package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NFFundAnnouncementParserTest {

    private final NFFundAnnouncementParser parser = new NFFundAnnouncementParser();

    @Test
    void shouldParseCurrentLimitsForAllShares() {
        String text = """
                南方纳斯达克100指数发起式证券投资基金（QDII）
                调整大额申购起始日 2026年7月9日
                调整大额定投起始日 2026年7月9日
                下属基金份额的代码 016452 016453 021000
                该基金份额的限制金额 10元 10元 1000元
                """;

        List<FundPurchaseLimitRule> rules = parser.parseText(
                "关于调整南方纳斯达克100指数发起式证券投资基金（QDII）申购、定投及转换转入业务金额限制的公告",
                text
        );

        assertThat(rules).hasSize(6).allSatisfy(rule -> {
            assertThat(rule.getCurrency()).isEqualTo("CNY");
            assertThat(rule.getStatus()).isEqualTo("LIMITED");
            assertThat(rule.getSalesChannel()).isEqualTo("ALL_CHANNELS");
            assertThat(rule.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 7, 9));
        });
        assertThat(rules).filteredOn(rule -> "016452".equals(rule.getFundCode()))
                .allSatisfy(rule -> assertThat(rule.getLimitAmount()).isEqualByComparingTo("10"));
        assertThat(rules).filteredOn(rule -> "021000".equals(rule.getFundCode()))
                .allSatisfy(rule -> assertThat(rule.getLimitAmount()).isEqualByComparingTo("1000"));
    }

    @Test
    void shouldConvertTenThousandYuanUnit() {
        String text = """
                南方纳斯达克100指数发起式证券投资基金（QDII）
                暂停大额申购起始日 2025年10月20日
                下属基金份额的代码 016452 016453 021000
                该基金份额的限制金额 2000元 2000元 10万元
                """;

        List<FundPurchaseLimitRule> rules = parser.parseText(
                "南方纳斯达克100指数基金暂停大额申购公告", text
        );

        assertThat(rules).hasSize(3);
        assertThat(rules).filteredOn(rule -> "021000".equals(rule.getFundCode()))
                .singleElement().satisfies(rule ->
                        assertThat(rule.getLimitAmount()).isEqualByComparingTo("100000"));
    }

}
