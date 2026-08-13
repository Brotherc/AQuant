package com.brotherc.aquant.service.fund;

import com.brotherc.aquant.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.model.dto.ccbfund.CCBFundAnnouncementParseResult;
import com.brotherc.aquant.model.dto.ccbfund.CCBFundPurchaseRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CCBFundAnnouncementParserTest {

    private final CCBFundAnnouncementParser parser = new CCBFundAnnouncementParser();

    @Test
    void shouldParseDirectChannelLimitsAndIgnoreDashAmounts() {
        String title = "建信纳斯达克100指数型证券投资基金（QDII）在直销渠道暂停大额申购、定期定额投资公告";
        String text = "暂停大额申购起始日 2026年8月13日 "
                + "暂停定期定额投资起始日 2026年8月13日 建信基金直销渠道 "
                + "基金代码 012751 539001 012753 012752 023422";
        List<List<String>> rows = List.of(
                List.of("下属分级基金的交易代码", "012751", "539001", "012753", "012752", "023422"),
                List.of("该分级基金是否暂停大额申购、定期定额投资", "是", "是", "是", "是", "是"),
                List.of("下属分级基金的限制申购金额（单位：人民币元）", "-", "10,000.00", "-", "10,000.00", "-"),
                List.of("下属分级基金的限制定期定额投资金额（单位：人民币元）", "-", "10,000.00", "-", "10,000.00", "-")
        );

        CCBFundAnnouncementParseResult result = parser.parseExtractedDocument(
                title, text, rows, Set.of("012751", "539001", "012753", "012752", "023422")
        );

        assertThat(result.isMatchedTargetFund()).isTrue();
        assertThat(result.getRules()).hasSize(4);
        assertThat(result.getRules()).allSatisfy(rule -> {
            assertThat(rule.getSalesChannel()).isEqualTo(FundPurchaseLimitConstant.CHANNEL_DIRECT);
            assertThat(rule.getStatus()).isEqualTo(FundPurchaseLimitConstant.STATUS_LIMITED);
            assertThat(rule.getLimitAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
            assertThat(rule.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 8, 13));
        });
        assertThat(result.getRules()).extracting(CCBFundPurchaseRule::getFundCode)
                .containsOnly("539001", "012752");
    }

    @Test
    void shouldTreatRestoreWithoutLimitAsOpenForPurchaseAndRecurringInvestment() {
        String title = "建信纳斯达克100指数型证券投资基金恢复申购、定期定额投资公告";
        String text = "恢复申购起始日 2026年1月12日 恢复定期定额投资起始日 2026年1月12日 基金代码 539001";
        List<List<String>> rows = List.of(
                List.of("下属分级基金的交易代码", "539001"),
                List.of("该分级基金是否暂停大额申购、定期定额投资", "否")
        );

        CCBFundAnnouncementParseResult result = parser.parseExtractedDocument(
                title, text, rows, Set.of("539001")
        );

        assertThat(result.getRules()).hasSize(2);
        assertThat(result.getRules()).allSatisfy(rule ->
                assertThat(rule.getStatus()).isEqualTo(FundPurchaseLimitConstant.STATUS_OPEN));
        assertThat(result.getRules()).extracting(CCBFundPurchaseRule::getBusinessType)
                .containsExactlyInAnyOrder(
                        FundPurchaseLimitConstant.BUSINESS_PURCHASE,
                        FundPurchaseLimitConstant.BUSINESS_RECURRING
                );
    }

    @Test
    void shouldIgnoreAnnouncementForAnotherSalesChannel() {
        CCBFundAnnouncementParseResult result = parser.parseExtractedDocument(
                "建信纳斯达克100指数基金在某代销渠道暂停大额申购公告",
                "基金代码 539001", List.of(), Set.of("539001")
        );

        assertThat(result.isMatchedTargetFund()).isFalse();
        assertThat(result.getRules()).isEmpty();
    }
}
