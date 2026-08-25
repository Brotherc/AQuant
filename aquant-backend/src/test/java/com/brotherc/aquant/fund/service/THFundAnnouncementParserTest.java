package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class THFundAnnouncementParserTest {

    private final THFundAnnouncementParser parser = new THFundAnnouncementParser();

    @Test
    void shouldParseCurrentSuspensionForAllShares() {
        String text = """
                天弘纳斯达克100指数型发起式证券投资基金（QDII）
                暂停申购起始日 2026年06月01日
                暂停定期定额投资起始日 2026年06月01日
                下属分级基金的交易代码 018043 018044 022525
                """;

        List<FundPurchaseLimitRule> rules = parser.parseText(
                "天弘基金关于天弘纳斯达克100指数基金暂停申购及定期定额投资业务的公告", text
        );

        assertThat(rules).hasSize(6).allSatisfy(rule -> {
            assertThat(rule.getStatus()).isEqualTo("SUSPENDED");
            assertThat(rule.getLimitAmount()).isNull();
            assertThat(rule.getCurrency()).isEqualTo("CNY");
            assertThat(rule.getSalesChannel()).isEqualTo("ALL_CHANNELS");
            assertThat(rule.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        });
        assertThat(rules).extracting(FundPurchaseLimitRule::getFundCode)
                .containsExactlyInAnyOrder("018043", "018043", "018044", "018044", "022525", "022525");
    }

    @Test
    void shouldParseAdjustedLimitForAllShares() {
        String text = """
                天弘纳斯达克100指数型发起式证券投资基金（QDII）
                调整大额申购起始日 2026年04月08日
                本公司决定自2026年04月08日起，暂停本基金单个基金份额单笔金额100元以上
                的申购（含定期定额投资）业务申请，单日累计不得超过100元。
                """;

        List<FundPurchaseLimitRule> rules = parser.parseText(
                "天弘纳斯达克100指数基金调整大额申购及定期定额投资业务的公告", text
        );

        assertThat(rules).hasSize(6).allSatisfy(rule -> {
            assertThat(rule.getStatus()).isEqualTo("LIMITED");
            assertThat(rule.getLimitAmount()).isEqualByComparingTo("100");
            assertThat(rule.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 4, 8));
        });
    }

}
