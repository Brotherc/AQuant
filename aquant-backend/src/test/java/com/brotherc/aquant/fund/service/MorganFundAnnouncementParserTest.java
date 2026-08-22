package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MorganFundAnnouncementParserTest {

    private final MorganFundAnnouncementParser parser = new MorganFundAnnouncementParser();

    @Test
    void shouldParseAllChannelCnyAndUsdLimits() {
        String text = """
                摩根纳斯达克 100 指数型发起式证券投资基金(QDII)
                暂停大额申购起始日 2026 年 7 月 27 日
                下属分级基金的交易代码 019172 019173 019174 019175
                下属分级基金的限制申购金额（单位：人民币元） 10.00 10.00 1.00 1.00
                下属分级基金的限制转换转入金额（单位：人民币元） 10.00 10.00 - -
                下属分级基金的限制定期定额投资金额（单位：人民币元） 10.00 10.00 1.00 1.00
                注:人民币份额的限制金额单位为人民币元，美元份额的限制金额单位为美元。
                """;

        List<FundPurchaseLimitRule> rules = parser.parseText(
                "摩根纳斯达克100指数基金调整大额申购限制金额的公告", text
        );

        assertThat(rules).hasSize(8);
        assertThat(rules).filteredOn(rule -> "PURCHASE".equals(rule.getBusinessType()))
                .extracting(FundPurchaseLimitRule::getFundCode)
                .containsExactly("019172", "019173", "019174", "019175");
        assertThat(rules).filteredOn(rule -> "019172".equals(rule.getFundCode()))
                .allSatisfy(rule -> {
                    assertThat(rule.getLimitAmount()).isEqualByComparingTo("10");
                    assertThat(rule.getCurrency()).isEqualTo("CNY");
                    assertThat(rule.getSalesChannel()).isEqualTo("ALL_CHANNELS");
                    assertThat(rule.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 7, 27));
                });
        assertThat(rules).filteredOn(rule -> "019174".equals(rule.getFundCode()))
                .allSatisfy(rule -> {
                    assertThat(rule.getLimitAmount()).isEqualByComparingTo("1");
                    assertThat(rule.getCurrency()).isEqualTo("USD");
                });
    }

    @Test
    void shouldRecognizeDirectChannelRule() {
        String text = """
                摩根纳斯达克100指数型发起式证券投资基金(QDII)
                本公司决定自 2026 年 7 月 10 日起对在直销渠道的本基金申购业务进行限制。
                下属分级基金的交易代码 019172 019173 019174 019175
                下属分级基金的限制申购金额（单位：人民币元） 300.00 300.00 30.00 30.00
                下属分级基金的限制转换转入金额（单位：人民币元） 300.00 300.00 - -
                下属分级基金的限制定期定额投资金额（单位：人民币元） 300.00 300.00 30.00 30.00
                注:人民币份额的限制金额单位为人民币元，美元份额的限制金额单位为美元。
                """;

        List<FundPurchaseLimitRule> rules = parser.parseText(
                "调整直销渠道大额申购、定期定额投资限制金额的公告", text
        );

        assertThat(rules).hasSize(8).allSatisfy(rule ->
                assertThat(rule.getSalesChannel()).isEqualTo("DIRECT"));
    }

    @Test
    void shouldParseRestoredPurchaseAsOpen() {
        String text = """
                摩根纳斯达克100指数型发起式证券投资基金(QDII)
                恢复大额申购起始日 2026 年 8 月 24 日
                下属分级基金的交易代码 019172 019173 019174 019175
                """;

        List<FundPurchaseLimitRule> rules = parser.parseText(
                "摩根纳斯达克100指数基金恢复大额申购业务的公告", text
        );

        assertThat(rules).hasSize(4).allSatisfy(rule -> {
            assertThat(rule.getStatus()).isEqualTo("OPEN");
            assertThat(rule.getLimitAmount()).isNull();
            assertThat(rule.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 8, 24));
        });
    }

}
