package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HTFFundAnnouncementParserTest {

    private final HTFFundAnnouncementParser parser = new HTFFundAnnouncementParser();

    @Test
    void shouldMapFiveShareAmountColumns() {
        String title = "关于汇添富纳斯达克100基金调整大额申购、定期定额投资业务限制金额的公告";
        String text = """
                公告送出日期 2026 ? 07 ? 17 ?
                基金主代码 018966
                暂停大额申购起始日
                2026 ? 07 ? 17 ?
                018966 018967 018969 018968 021773
                是 是 是 是 是
                人民币元 人民币元 美元 美元 人民币元
                10.00 10.00 2.00 2.00 10.00
                10.00 10.00 2.00 2.00 10.00
                """;

        List<FundPurchaseLimitRule> rules = parser.parseText(title, text);

        assertThat(rules).hasSize(10).allSatisfy(rule -> {
            assertThat(rule.getStatus()).isEqualTo("LIMITED");
            assertThat(rule.getSalesChannel()).isEqualTo("ALL_CHANNELS");
            assertThat(rule.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 7, 17));
        });
        assertThat(rules).extracting(FundPurchaseLimitRule::getFundCode)
                .containsExactly("018966", "018966", "018967", "018967", "018969",
                        "018969", "018968", "018968", "021773", "021773");
        assertThat(rules).filteredOn(rule -> "USD".equals(rule.getCurrency()))
                .allSatisfy(rule -> assertThat(rule.getLimitAmount()).isEqualByComparingTo("2"));
        assertThat(rules).filteredOn(rule -> "CNY".equals(rule.getCurrency()))
                .allSatisfy(rule -> assertThat(rule.getLimitAmount()).isEqualByComparingTo("10"));
    }

}
