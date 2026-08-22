package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EFundAnnouncementParser {

    private static final Pattern EFFECTIVE_DATE_PATTERN = Pattern.compile(
            "(?:暂停(?:大额)?申购起始日|恢复(?:大额)?申购(?:日|起始日)|决定自|自)"
                    + ".{0,20}?(20\\d{2})年(\\d{1,2})月(\\d{1,2})日"
    );
    private static final Pattern CNY_LIMIT_PATTERN = Pattern.compile(
            "人民币(?:基金)?份额.{0,400}?(?:不超过|不得超过|不应超过|限额(?:为)?)([\\d,.]+)元"
    );
    private static final Pattern USD_LIMIT_PATTERN = Pattern.compile(
            "美元(?:现汇)?(?:基金)?份额.{0,400}?(?:不超过|不得超过|不应超过|限额(?:为)?)([\\d,.]+)美元"
    );
    private static final Map<String, String> TARGET_FUND_CURRENCIES = createTargetFundCurrencies();

    public List<FundPurchaseLimitRule> parse(String title, byte[] attachment) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(attachment))) {
            return parseText(title, new PDFTextStripper().getText(document));
        } catch (Exception e) {
            throw new IllegalStateException("解析易方达基金额度公告失败", e);
        }
    }

    List<FundPurchaseLimitRule> parseText(String title, String text) {
        String normalized = text.replace('\u00a0', ' ').replaceAll("\\s+", "").trim();
        String fullText = title.replaceAll("\\s+", "") + normalized;
        if (!fullText.contains("易方达纳斯达克100")) {
            return List.of();
        }

        LocalDate effectiveDate = extractEffectiveDate(normalized);
        String channel = fullText.contains("直销") && !fullText.contains("全部销售机构")
                ? FundPurchaseLimitConstant.CHANNEL_DIRECT : FundPurchaseLimitConstant.CHANNEL_ALL;
        BigDecimal cnyLimit = extractAmount(CNY_LIMIT_PATTERN, normalized);
        BigDecimal usdLimit = extractAmount(USD_LIMIT_PATTERN, normalized);
        boolean hasLimit = cnyLimit != null || usdLimit != null;
        String status;
        if (hasLimit || fullText.contains("暂停大额申购")) {
            status = FundPurchaseLimitConstant.STATUS_LIMITED;
            if (cnyLimit == null || usdLimit == null) {
                throw new IllegalStateException("易方达额度公告未同时解析出人民币和美元份额限额");
            }
        } else if (fullText.contains("暂停申购")) {
            status = FundPurchaseLimitConstant.STATUS_SUSPENDED;
        } else if (fullText.contains("恢复申购") || fullText.contains("恢复大额申购")) {
            status = FundPurchaseLimitConstant.STATUS_OPEN;
        } else {
            return List.of();
        }

        boolean includesRecurring = fullText.contains("定期定额") || fullText.contains("定投");
        List<FundPurchaseLimitRule> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : TARGET_FUND_CURRENCIES.entrySet()) {
            BigDecimal amount = "USD".equals(entry.getValue()) ? usdLimit : cnyLimit;
            result.add(createRule(entry.getKey(), entry.getValue(), channel,
                    FundPurchaseLimitConstant.BUSINESS_PURCHASE, status, amount, effectiveDate));
            if (includesRecurring) {
                result.add(createRule(entry.getKey(), entry.getValue(), channel,
                        FundPurchaseLimitConstant.BUSINESS_RECURRING, status, amount, effectiveDate));
            }
        }
        return result;
    }

    private FundPurchaseLimitRule createRule(
            String fundCode, String currency, String channel, String businessType,
            String status, BigDecimal amount, LocalDate effectiveDate
    ) {
        FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
        rule.setFundCode(fundCode);
        rule.setCurrency(currency);
        rule.setSalesChannel(channel);
        rule.setBusinessType(businessType);
        rule.setStatus(status);
        rule.setLimitAmount(FundPurchaseLimitConstant.STATUS_LIMITED.equals(status) ? amount : null);
        rule.setEffectiveDate(effectiveDate);
        return rule;
    }

    private BigDecimal extractAmount(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? new BigDecimal(matcher.group(1).replace(",", "")) : null;
    }

    private LocalDate extractEffectiveDate(String text) {
        Matcher matcher = EFFECTIVE_DATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            throw new IllegalStateException("易方达额度公告未解析出生效日期");
        }
        return LocalDate.of(Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

    private static Map<String, String> createTargetFundCurrencies() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("161130", "CNY");
        result.put("012870", "CNY");
        result.put("003722", "USD");
        result.put("012871", "USD");
        return result;
    }

}
