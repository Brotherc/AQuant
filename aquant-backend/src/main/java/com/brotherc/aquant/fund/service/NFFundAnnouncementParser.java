package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NFFundAnnouncementParser {

    private static final List<String> TARGET_FUND_CODES = List.of("016452", "016453", "021000");
    private static final Pattern EFFECTIVE_DATE_PATTERN = Pattern.compile(
            "(?:调整|暂停|恢复)(?:办理)?(?:大额)?申购起始日[：:]?"
                    + "(\\d{4})年(\\d{1,2})月(\\d{1,2})日"
    );
    private static final Pattern FALLBACK_EFFECTIVE_DATE_PATTERN = Pattern.compile(
            "自(\\d{4})年(\\d{1,2})月(\\d{1,2})日起"
    );
    private static final Pattern LIMIT_TABLE_PATTERN = Pattern.compile(
            "下属基金份额的代码.*?016452.*?016453.*?021000.*?该基金份额的限制金额.*?"
                    + "([\\d,.]+)(万?)元.*?([\\d,.]+)(万?)元.*?([\\d,.]+)(万?)元"
    );

    public List<FundPurchaseLimitRule> parse(String title, byte[] attachment) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(attachment))) {
            return parseText(title, new PDFTextStripper().getText(document));
        } catch (Exception e) {
            throw new IllegalStateException("解析南方基金额度公告失败", e);
        }
    }

    List<FundPurchaseLimitRule> parseText(String title, String text) {
        String normalized = text.replace('\u00a0', ' ').replaceAll("\\s+", "").trim();
        String fullText = title.replaceAll("\\s+", "") + normalized;
        if (!fullText.contains("南方纳斯达克100")) {
            return List.of();
        }

        LocalDate effectiveDate = extractEffectiveDate(normalized);
        String channel = fullText.contains("直销渠道")
                ? FundPurchaseLimitConstant.CHANNEL_DIRECT : FundPurchaseLimitConstant.CHANNEL_ALL;
        Matcher limitMatcher = LIMIT_TABLE_PATTERN.matcher(normalized);
        List<BigDecimal> limits = new ArrayList<>();
        if (limitMatcher.find()) {
            limits.add(StockUtils.toAmount(limitMatcher.group(1), limitMatcher.group(2)));
            limits.add(StockUtils.toAmount(limitMatcher.group(3), limitMatcher.group(4)));
            limits.add(StockUtils.toAmount(limitMatcher.group(5), limitMatcher.group(6)));
        }

        String status;
        if (!limits.isEmpty()) {
            status = FundPurchaseLimitConstant.STATUS_LIMITED;
        } else if (fullText.contains("恢复大额申购") || fullText.contains("恢复办理大额申购")
                || fullText.contains("取消大额申购限制")) {
            status = FundPurchaseLimitConstant.STATUS_OPEN;
        } else if (fullText.contains("暂停申购") && !fullText.contains("暂停大额申购")) {
            status = FundPurchaseLimitConstant.STATUS_SUSPENDED;
        } else {
            throw new IllegalStateException("南方额度公告未解析出三类基金份额的限制金额");
        }

        boolean includesRecurring = fullText.contains("定投") || fullText.contains("定期定额");
        List<FundPurchaseLimitRule> result = new ArrayList<>();
        for (int i = 0; i < TARGET_FUND_CODES.size(); i++) {
            BigDecimal limit = limits.isEmpty() ? null : limits.get(i);
            result.add(createRule(TARGET_FUND_CODES.get(i), channel,
                    FundPurchaseLimitConstant.BUSINESS_PURCHASE, status, limit, effectiveDate));
            if (includesRecurring) {
                result.add(createRule(TARGET_FUND_CODES.get(i), channel,
                        FundPurchaseLimitConstant.BUSINESS_RECURRING, status, limit, effectiveDate));
            }
        }
        return result;
    }

    private FundPurchaseLimitRule createRule(
            String fundCode, String channel, String businessType, String status,
            BigDecimal limit, LocalDate effectiveDate
    ) {
        FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
        rule.setFundCode(fundCode);
        rule.setCurrency("CNY");
        rule.setSalesChannel(channel);
        rule.setBusinessType(businessType);
        rule.setStatus(status);
        rule.setLimitAmount(FundPurchaseLimitConstant.STATUS_LIMITED.equals(status) ? limit : null);
        rule.setEffectiveDate(effectiveDate);
        return rule;
    }

    private LocalDate extractEffectiveDate(String text) {
        Matcher matcher = EFFECTIVE_DATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            matcher = FALLBACK_EFFECTIVE_DATE_PATTERN.matcher(text);
            if (!matcher.find()) {
                throw new IllegalStateException("南方额度公告未解析出生效日期");
            }
        }
        return LocalDate.of(Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

}
