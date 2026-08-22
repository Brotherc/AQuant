package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EFundAnnouncementParserTest {

    private final EFundAnnouncementParser parser = new EFundAnnouncementParser();

    @Test
    void shouldParseCnyAndUsdLimitsForAllShares() {
        String text = """
                易方达纳斯达克100指数证券投资基金（LOF）
                本基金人民币基金份额包括161130和012870，美元现汇基金份额包括003722和012871。
                本公司决定自2025年11月4日起暂停大额申购及定期定额投资业务。
                人民币基金份额单日单个基金账户累计申购金额不超过10元，
                美元现汇基金份额单日单个基金账户累计申购金额不超过2美元。
                """;

        List<FundPurchaseLimitRule> rules = parser.parseText(
                "易方达纳斯达克100指数基金暂停大额申购、定期定额投资业务的公告", text
        );

        assertThat(rules).hasSize(8);
        assertThat(rules).filteredOn(rule -> "161130".equals(rule.getFundCode()))
                .allSatisfy(rule -> {
                    assertThat(rule.getLimitAmount()).isEqualByComparingTo("10");
                    assertThat(rule.getCurrency()).isEqualTo("CNY");
                    assertThat(rule.getStatus()).isEqualTo("LIMITED");
                    assertThat(rule.getSalesChannel()).isEqualTo("ALL_CHANNELS");
                    assertThat(rule.getEffectiveDate()).isEqualTo(LocalDate.of(2025, 11, 4));
                });
        assertThat(rules).filteredOn(rule -> "003722".equals(rule.getFundCode()))
                .allSatisfy(rule -> {
                    assertThat(rule.getLimitAmount()).isEqualByComparingTo("2");
                    assertThat(rule.getCurrency()).isEqualTo("USD");
                });
    }

    @Test
    void shouldKeepLimitedWhenPurchaseIsRestoredWithLargePurchaseRestriction() {
        String text = """
                易方达纳斯达克100指数证券投资基金（LOF）
                本公司决定自2025年2月25日起恢复申购及定期定额投资业务并暂停大额申购。
                人民币份额单日单个基金账户累计申购金额不应超过100元，
                美元份额单日单个基金账户累计申购金额不应超过20美元。
                """;

        List<FundPurchaseLimitRule> rules = parser.parseText(
                "恢复申购及定期定额投资业务并暂停大额申购的公告", text
        );

        assertThat(rules).hasSize(8).allSatisfy(rule ->
                assertThat(rule.getStatus()).isEqualTo("LIMITED"));
    }

    @Test
    void shouldParseFullPurchaseSuspension() {
        String text = """
                易方达纳斯达克100指数证券投资基金（LOF）
                暂停申购起始日 2026年3月19日
                自该日起暂停申购及定期定额投资业务。
                """;

        List<FundPurchaseLimitRule> rules = parser.parseText(
                "易方达纳斯达克100指数基金暂停申购及定期定额投资业务的公告", text
        );

        assertThat(rules).hasSize(8).allSatisfy(rule -> {
            assertThat(rule.getStatus()).isEqualTo("SUSPENDED");
            assertThat(rule.getLimitAmount()).isNull();
            assertThat(rule.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 3, 19));
        });
    }

    @Test
    void shouldParseDirectChannelOpenRule() {
        String text = """
                易方达纳斯达克100指数证券投资基金（LOF）
                本公司决定自2026年9月1日起在直销渠道恢复申购及定期定额投资业务。
                """;

        List<FundPurchaseLimitRule> rules = parser.parseText(
                "易方达纳斯达克100指数基金在直销渠道恢复申购的公告", text
        );

        assertThat(rules).hasSize(8).allSatisfy(rule -> {
            assertThat(rule.getStatus()).isEqualTo("OPEN");
            assertThat(rule.getSalesChannel()).isEqualTo("DIRECT");
        });
    }

}
