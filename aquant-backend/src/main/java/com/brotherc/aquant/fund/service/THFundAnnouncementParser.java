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
public class THFundAnnouncementParser {

    private static final List<String> TARGET_FUND_CODES = List.of("018043", "018044", "022525");
    private static final Pattern EFFECTIVE_DATE_PATTERN = Pattern.compile(
            "(?:暂停(?:大额)?申购起始日|恢复(?:大额)?申购(?:日|起始日)|调整大额申购起始日)"
                    + ".{0,30}?(20\\d{2})年(\\d{1,2})月(\\d{1,2})日"
    );
    private static final Pattern FALLBACK_EFFECTIVE_DATE_PATTERN = Pattern.compile(
            "(?:决定)?自(20\\d{2})年(\\d{1,2})月(\\d{1,2})日起"
    );
    private static final List<Pattern> LIMIT_PATTERNS = List.of(
            Pattern.compile("单笔金额([\\d,.]+)(万?)元以上"),
            Pattern.compile("(?:单日)?累计申购.{0,30}?不得超过([\\d,.]+)(万?)元"),
            Pattern.compile("(?:金额限制|限额)(?:调整)?为([\\d,.]+)(万?)元")
    );

    public List<FundPurchaseLimitRule> parse(String title, byte[] attachment) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(attachment))) {
            return parseText(title, new PDFTextStripper().getText(document));
        } catch (Exception e) {
            throw new IllegalStateException("解析天弘基金额度公告失败", e);
        }
    }

    List<FundPurchaseLimitRule> parseText(String title, String text) {
        String normalized = text.replace('\u00a0', ' ').replaceAll("\\s+", "").trim();
        String fullText = title.replaceAll("\\s+", "") + normalized;
        if (!fullText.contains("天弘纳斯达克100")) {
            return List.of();
        }

        LocalDate effectiveDate = extractEffectiveDate(normalized);
        BigDecimal limit = extractLimit(normalized);
        String status;
        if (fullText.contains("暂停申购") && !fullText.contains("暂停大额申购")) {
            status = FundPurchaseLimitConstant.STATUS_SUSPENDED;
        } else if (limit != null || fullText.contains("暂停大额申购") || fullText.contains("限制大额申购")) {
            if (limit == null) {
                throw new IllegalStateException("天弘额度公告未解析出限制金额");
            }
            status = FundPurchaseLimitConstant.STATUS_LIMITED;
        } else if (fullText.contains("恢复申购") || fullText.contains("恢复大额申购")
                || fullText.contains("取消大额申购限制")) {
            status = FundPurchaseLimitConstant.STATUS_OPEN;
        } else {
            return List.of();
        }

        String channel = fullText.contains("直销渠道") && !fullText.contains("全部销售机构")
                ? FundPurchaseLimitConstant.CHANNEL_DIRECT : FundPurchaseLimitConstant.CHANNEL_ALL;
        boolean includesRecurring = fullText.contains("定期定额") || fullText.contains("定投");
        List<FundPurchaseLimitRule> result = new ArrayList<>();
        for (String fundCode : TARGET_FUND_CODES) {
            result.add(createRule(fundCode, channel, FundPurchaseLimitConstant.BUSINESS_PURCHASE,
                    status, limit, effectiveDate));
            if (includesRecurring) {
                result.add(createRule(fundCode, channel, FundPurchaseLimitConstant.BUSINESS_RECURRING,
                        status, limit, effectiveDate));
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

    private BigDecimal extractLimit(String text) {
        for (Pattern pattern : LIMIT_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return StockUtils.toAmount(matcher.group(1), matcher.group(2));
            }
        }
        return null;
    }

    private LocalDate extractEffectiveDate(String text) {
        Matcher matcher = EFFECTIVE_DATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            matcher = FALLBACK_EFFECTIVE_DATE_PATTERN.matcher(text);
            if (!matcher.find()) {
                throw new IllegalStateException("天弘额度公告未解析出生效日期");
            }
        }
        return LocalDate.of(Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

}
