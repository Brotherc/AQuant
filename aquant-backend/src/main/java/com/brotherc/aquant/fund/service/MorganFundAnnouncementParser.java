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
public class MorganFundAnnouncementParser {

    private static final Pattern EFFECTIVE_DATE_PATTERN = Pattern.compile(
            "(?:暂停(?:大额)?申购起始日|恢复(?:大额)?申购起始日|决定自)\\s*"
                    + "(20\\d{2})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日"
    );
    private static final Map<String, String> TARGET_FUND_CURRENCIES = createTargetFundCurrencies();

    public List<FundPurchaseLimitRule> parse(String title, byte[] attachment) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(attachment))) {
            return parseText(title, new PDFTextStripper().getText(document));
        } catch (Exception e) {
            throw new IllegalStateException("解析摩根基金额度公告失败", e);
        }
    }

    List<FundPurchaseLimitRule> parseText(String title, String text) {
        String normalized = text.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
        if (!normalized.replace(" ", "").contains("摩根纳斯达克100指数")) {
            return List.of();
        }
        String channel = (title + normalized).contains("直销")
                ? FundPurchaseLimitConstant.CHANNEL_DIRECT : FundPurchaseLimitConstant.CHANNEL_ALL;
        LocalDate effectiveDate = extractEffectiveDate(normalized);
        List<String> codes = extractCodes(normalized);
        if (codes.isEmpty()) {
            return List.of();
        }

        List<FundPurchaseLimitRule> result = new ArrayList<>();
        addRules(result, codes, extractAmounts(normalized, "限制申购金额", "限制转换转入金额"),
                FundPurchaseLimitConstant.BUSINESS_PURCHASE, channel, effectiveDate);
        addRules(result, codes, extractAmounts(normalized, "限制定期定额投资金额", "注:"),
                FundPurchaseLimitConstant.BUSINESS_RECURRING, channel, effectiveDate);
        if (result.isEmpty()) {
            String status = null;
            if ((title + normalized).contains("恢复大额申购") || (title + normalized).contains("恢复申购")) {
                status = FundPurchaseLimitConstant.STATUS_OPEN;
            } else if ((title + normalized).contains("暂停申购")
                    && !(title + normalized).contains("暂停大额申购")) {
                status = FundPurchaseLimitConstant.STATUS_SUSPENDED;
            }
            if (status != null) {
                addStatusRules(result, codes, FundPurchaseLimitConstant.BUSINESS_PURCHASE,
                        channel, status, effectiveDate);
                if ((title + normalized).contains("定期定额") || (title + normalized).contains("定投")) {
                    addStatusRules(result, codes, FundPurchaseLimitConstant.BUSINESS_RECURRING,
                            channel, status, effectiveDate);
                }
            }
        }
        return result;
    }

    private void addStatusRules(
            List<FundPurchaseLimitRule> result, List<String> codes, String businessType,
            String channel, String status, LocalDate effectiveDate
    ) {
        for (String code : codes) {
            FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
            rule.setFundCode(code);
            rule.setSalesChannel(channel);
            rule.setBusinessType(businessType);
            rule.setStatus(status);
            rule.setCurrency(TARGET_FUND_CURRENCIES.get(code));
            rule.setEffectiveDate(effectiveDate);
            result.add(rule);
        }
    }

    private void addRules(
            List<FundPurchaseLimitRule> result, List<String> codes, List<BigDecimal> amounts,
            String businessType, String channel, LocalDate effectiveDate
    ) {
        if (amounts.size() < codes.size()) {
            return;
        }
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
            rule.setFundCode(code);
            rule.setSalesChannel(channel);
            rule.setBusinessType(businessType);
            rule.setStatus(FundPurchaseLimitConstant.STATUS_LIMITED);
            rule.setLimitAmount(amounts.get(i));
            rule.setCurrency(TARGET_FUND_CURRENCIES.get(code));
            rule.setEffectiveDate(effectiveDate);
            result.add(rule);
        }
    }

    private List<String> extractCodes(String text) {
        int start = text.indexOf("交易代码");
        if (start < 0) {
            return List.of();
        }
        String codeSection = text.substring(start, Math.min(text.length(), start + 160));
        List<String> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("(?<!\\d)\\d{6}(?!\\d)").matcher(codeSection);
        while (matcher.find()) {
            String code = matcher.group();
            if (TARGET_FUND_CURRENCIES.containsKey(code)) {
                result.add(code);
            }
        }
        return result;
    }

    private List<BigDecimal> extractAmounts(String text, String startKeyword, String endKeyword) {
        int start = text.indexOf(startKeyword);
        if (start < 0) {
            return List.of();
        }
        int end = text.indexOf(endKeyword, start + startKeyword.length());
        String amountSection = text.substring(start, end > start ? end : Math.min(text.length(), start + 220));
        List<BigDecimal> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("(?<![\\d.])\\d+(?:\\.\\d+)?(?![\\d.])").matcher(amountSection);
        while (matcher.find() && result.size() < TARGET_FUND_CURRENCIES.size()) {
            result.add(new BigDecimal(matcher.group()));
        }
        return result;
    }

    private LocalDate extractEffectiveDate(String text) {
        Matcher matcher = EFFECTIVE_DATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return LocalDate.of(Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

    private static Map<String, String> createTargetFundCurrencies() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("019172", "CNY");
        result.put("019173", "CNY");
        result.put("019174", "USD");
        result.put("019175", "USD");
        return result;
    }

}
