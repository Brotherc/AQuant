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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HTFFundAnnouncementParser {

    private static final List<String> TARGET_FUND_CODES = List.of(
            "018966", "018967", "018969", "018968", "021773"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(20\\d{2})\\D{1,5}(\\d{1,2})\\D{1,5}(\\d{1,2})"
    );
    private static final Pattern AMOUNT_TOKEN_PATTERN = Pattern.compile("(?:[\\d,.]+|-)");

    public List<FundPurchaseLimitRule> parse(String title, byte[] attachment) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(attachment))) {
            return parseText(title, new PDFTextStripper().getText(document));
        } catch (Exception e) {
            throw new IllegalStateException("解析汇添富基金额度公告失败", e);
        }
    }

    List<FundPurchaseLimitRule> parseText(String title, String text) {
        String normalizedTitle = title.replaceAll("\\s+", "");
        if (!normalizedTitle.contains("汇添富纳斯达克100")) {
            return List.of();
        }
        String status;
        if (normalizedTitle.contains("恢复大额申购") || normalizedTitle.contains("取消大额申购限制")) {
            status = FundPurchaseLimitConstant.STATUS_OPEN;
        } else if (normalizedTitle.contains("暂停申购") && !normalizedTitle.contains("暂停大额申购")) {
            status = FundPurchaseLimitConstant.STATUS_SUSPENDED;
        } else if (normalizedTitle.contains("调整大额申购") || normalizedTitle.contains("暂停大额申购")
                || normalizedTitle.contains("业务限制金额")) {
            status = FundPurchaseLimitConstant.STATUS_LIMITED;
        } else {
            return List.of();
        }

        String[] lines = text.lines().map(String::trim).filter(line -> !line.isEmpty()).toArray(String[]::new);
        int codeRowIndex = findCodeRow(lines);
        LocalDate effectiveDate = findEffectiveDate(lines, codeRowIndex);
        boolean includesRecurring = normalizedTitle.contains("定期定额") || normalizedTitle.contains("定投");
        List<List<BigDecimal>> amountRows = FundPurchaseLimitConstant.STATUS_LIMITED.equals(status)
                ? findAmountRows(lines, codeRowIndex, includesRecurring ? 2 : 1) : List.of();

        List<FundPurchaseLimitRule> result = new ArrayList<>();
        for (int index = 0; index < TARGET_FUND_CODES.size(); index++) {
            BigDecimal purchaseAmount = amountRows.isEmpty() ? null : amountRows.get(0).get(index);
            if (!FundPurchaseLimitConstant.STATUS_LIMITED.equals(status) || purchaseAmount != null) {
                result.add(createRule(TARGET_FUND_CODES.get(index), index, FundPurchaseLimitConstant.BUSINESS_PURCHASE,
                        status, purchaseAmount, effectiveDate));
            }
            if (includesRecurring) {
                BigDecimal recurringAmount = amountRows.isEmpty() ? null : amountRows.get(1).get(index);
                if (!FundPurchaseLimitConstant.STATUS_LIMITED.equals(status) || recurringAmount != null) {
                    result.add(createRule(TARGET_FUND_CODES.get(index), index,
                            FundPurchaseLimitConstant.BUSINESS_RECURRING,
                            status, recurringAmount, effectiveDate));
                }
            }
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("汇添富额度公告未解析出适用份额");
        }
        return result;
    }

    private int findCodeRow(String[] lines) {
        for (int index = 0; index < lines.length; index++) {
            boolean containsAllCodes = true;
            for (String fundCode : TARGET_FUND_CODES) {
                if (!lines[index].contains(fundCode)) {
                    containsAllCodes = false;
                }
            }
            if (containsAllCodes) {
                return index;
            }
        }
        throw new IllegalStateException("汇添富额度公告未找到完整份额代码行");
    }

    private LocalDate findEffectiveDate(String[] lines, int codeRowIndex) {
        LocalDate result = null;
        for (int index = 0; index < codeRowIndex; index++) {
            Matcher matcher = DATE_PATTERN.matcher(lines[index]);
            while (matcher.find()) {
                result = LocalDate.of(Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
            }
        }
        if (result == null) {
            throw new IllegalStateException("汇添富额度公告未解析出生效日期");
        }
        return result;
    }

    private List<List<BigDecimal>> findAmountRows(String[] lines, int codeRowIndex, int expectedRowCount) {
        List<List<BigDecimal>> result = new ArrayList<>();
        for (int index = codeRowIndex + 1; index < lines.length && result.size() < expectedRowCount; index++) {
            String[] tokens = lines[index].split("\\s+");
            if (tokens.length == TARGET_FUND_CODES.size()) {
                boolean amountRow = true;
                List<BigDecimal> amounts = new ArrayList<>();
                for (String token : tokens) {
                    if (!AMOUNT_TOKEN_PATTERN.matcher(token).matches()) {
                        amountRow = false;
                    } else {
                        amounts.add("-".equals(token) ? null : new BigDecimal(token.replace(",", "")));
                    }
                }
                if (amountRow) {
                    result.add(amounts);
                }
            }
        }
        if (result.size() != expectedRowCount) {
            throw new IllegalStateException("汇添富额度公告金额表不完整");
        }
        return result;
    }

    private FundPurchaseLimitRule createRule(
            String fundCode, int codeIndex, String businessType, String status,
            BigDecimal amount, LocalDate effectiveDate
    ) {
        FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
        rule.setFundCode(fundCode);
        rule.setCurrency(codeIndex == 2 || codeIndex == 3 ? "USD" : "CNY");
        rule.setSalesChannel(FundPurchaseLimitConstant.CHANNEL_ALL);
        rule.setBusinessType(businessType);
        rule.setStatus(status);
        rule.setLimitAmount(FundPurchaseLimitConstant.STATUS_LIMITED.equals(status) ? amount : null);
        rule.setEffectiveDate(effectiveDate);
        return rule;
    }

}
