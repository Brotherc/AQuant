package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.integration.ccb.model.CCBFundAnnouncementParseResult;
import com.brotherc.aquant.integration.ccb.model.CCBFundPurchaseRule;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CCBFundAnnouncementParser {

    private static final Pattern FUND_CODE_PATTERN = Pattern.compile("(?<!\\d)\\d{6}(?!\\d)");
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2})[年/-](\\d{1,2})[月/-](\\d{1,2})日?");
    private static final Pattern NARRATIVE_AMOUNT_PATTERN = Pattern.compile("高于\\s*([\\d,.]+)\\s*(万)?元");

    public CCBFundAnnouncementParseResult parse(
            String title, String attachmentName, byte[] attachment, Set<String> targetFundCodes
    ) {
        ExtractedDocument document = extractDocument(attachmentName, attachment);
        return parseExtractedDocument(title, document.getFullText(), document.getTables(), targetFundCodes);
    }

    CCBFundAnnouncementParseResult parseExtractedDocument(
            String title, String fullText, List<List<String>> rows, Set<String> targetFundCodes
    ) {
        CCBFundAnnouncementParseResult result = new CCBFundAnnouncementParseResult();
        String normalizedText = normalize(fullText);
        Set<String> documentCodes = extractFundCodes(normalizedText);
        documentCodes.retainAll(targetFundCodes);
        result.setMatchedTargetFund(!documentCodes.isEmpty());
        if (documentCodes.isEmpty()) {
            return result;
        }
        if (isOtherSalesChannel(title, normalizedText)) {
            result.setMatchedTargetFund(false);
            return result;
        }

        String channel = isDirectChannel(title, normalizedText)
                ? FundPurchaseLimitConstant.CHANNEL_DIRECT : FundPurchaseLimitConstant.CHANNEL_ALL;
        List<String> codeRow = findRow(rows, "下属分级基金的交易代码", "下属基金的交易代码");
        if (codeRow.isEmpty()) {
            addNarrativeRules(result, documentCodes, title, normalizedText, channel);
            return result;
        }

        List<String> codes = codeRow.subList(1, codeRow.size());
        List<String> statusRow = findRow(rows, "是否暂停大额申购", "是否暂停申购");
        addTableRules(result, codes, statusRow,
                findRow(rows, "限制申购金额"), FundPurchaseLimitConstant.BUSINESS_PURCHASE,
                title, normalizedText, channel, targetFundCodes);
        addTableRules(result, codes, statusRow,
                findRow(rows, "限制定期定额投资金额"), FundPurchaseLimitConstant.BUSINESS_RECURRING,
                title, normalizedText, channel, targetFundCodes);

        if (result.getRules().isEmpty()) {
            addNarrativeRules(result, documentCodes, title, normalizedText, channel);
        }
        return result;
    }

    private void addTableRules(
            CCBFundAnnouncementParseResult result, List<String> codes, List<String> statusRow,
            List<String> amountRow, String businessType, String title, String text,
            String channel, Set<String> targetFundCodes
    ) {
        for (int i = 0; i < codes.size(); i++) {
            String code = normalize(codes.get(i));
            if (!targetFundCodes.contains(code)) {
                continue;
            }
            String amountText = getColumnValue(amountRow, i + 1);
            BigDecimal amount = parseAmount(amountText);
            String statusText = getColumnValue(statusRow, i + 1);
            String status = determineStatus(title, text, statusText, amount, businessType);
            if (status == null) {
                continue;
            }
            CCBFundPurchaseRule rule = newRule(code, channel, businessType, status,
                    amount, detectCurrency(amountRow), extractEffectiveDate(text, businessType));
            result.getRules().add(rule);
        }
    }

    private void addNarrativeRules(
            CCBFundAnnouncementParseResult result, Set<String> codes, String title, String text, String channel
    ) {
        BigDecimal amount = null;
        Matcher amountMatcher = NARRATIVE_AMOUNT_PATTERN.matcher(text);
        if (amountMatcher.find()) {
            amount = new BigDecimal(amountMatcher.group(1).replace(",", ""));
            if (StringUtils.isNotBlank(amountMatcher.group(2))) {
                amount = amount.multiply(BigDecimal.valueOf(10000));
            }
        }
        for (String code : codes) {
            addNarrativeRule(result, code, FundPurchaseLimitConstant.BUSINESS_PURCHASE, title, text, channel, amount);
            if (text.contains("定期定额") || text.contains("定投")) {
                addNarrativeRule(result, code, FundPurchaseLimitConstant.BUSINESS_RECURRING, title, text, channel, amount);
            }
        }
    }

    private void addNarrativeRule(
            CCBFundAnnouncementParseResult result, String code, String businessType,
            String title, String text, String channel, BigDecimal amount
    ) {
        String status = determineStatus(title, text, "是", amount, businessType);
        if (status == null) {
            return;
        }
        result.getRules().add(newRule(code, channel, businessType, status, amount, "CNY",
                extractEffectiveDate(text, businessType)));
    }

    private String determineStatus(
            String title, String text, String statusText, BigDecimal amount, String businessType
    ) {
        if (amount != null) {
            return FundPurchaseLimitConstant.STATUS_LIMITED;
        }
        String businessWord = FundPurchaseLimitConstant.BUSINESS_PURCHASE.equals(businessType) ? "申购" : "定期定额";
        if (!text.contains(businessWord) && !title.contains(businessWord)) {
            return null;
        }
        boolean pauseApplies = actionApplies(title, text, "暂停", businessType);
        boolean largePauseApplies = actionApplies(title, text, "暂停大额", businessType);
        boolean restoreApplies = actionApplies(title, text, "恢复", businessType);
        if (pauseApplies && !largePauseApplies) {
            return FundPurchaseLimitConstant.STATUS_SUSPENDED;
        }
        if (restoreApplies && !largePauseApplies && !text.contains("限制" + businessWord + "金额")) {
            return FundPurchaseLimitConstant.STATUS_OPEN;
        }
        if ("否".equals(normalize(statusText)) && restoreApplies) {
            return FundPurchaseLimitConstant.STATUS_OPEN;
        }
        return null;
    }

    private boolean actionApplies(String title, String text, String action, String businessType) {
        if (FundPurchaseLimitConstant.BUSINESS_PURCHASE.equals(businessType)) {
            return title.contains(action + "申购") || text.contains(action + "申购");
        }
        return title.contains(action + "定期定额") || text.contains(action + "定期定额")
                || title.contains(action + "申购、定期定额") || text.contains(action + "申购、定期定额");
    }

    private CCBFundPurchaseRule newRule(
            String fundCode, String channel, String businessType, String status,
            BigDecimal amount, String currency, LocalDate effectiveDate
    ) {
        CCBFundPurchaseRule rule = new CCBFundPurchaseRule();
        rule.setFundCode(fundCode);
        rule.setSalesChannel(channel);
        rule.setBusinessType(businessType);
        rule.setStatus(status);
        rule.setLimitAmount(amount);
        rule.setCurrency(currency);
        rule.setEffectiveDate(effectiveDate);
        return rule;
    }

    private LocalDate extractEffectiveDate(String text, String businessType) {
        String keyword = FundPurchaseLimitConstant.BUSINESS_PURCHASE.equals(businessType) ? "申购" : "定期定额投资";
        Pattern pattern = Pattern.compile("(?:暂停(?:大额)?|恢复)" + keyword + "(?:业务)?(?:的)?(?:起始日|日)?.{0,24}?"
                + DATE_PATTERN.pattern());
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        int offset = matcher.groupCount() >= 3 ? matcher.groupCount() - 2 : 1;
        return LocalDate.of(
                Integer.parseInt(matcher.group(offset)),
                Integer.parseInt(matcher.group(offset + 1)),
                Integer.parseInt(matcher.group(offset + 2))
        );
    }

    private BigDecimal parseAmount(String value) {
        String normalized = normalize(value).replace(",", "").replace("人民币元", "").replace("元", "");
        if (StringUtils.isBlank(normalized) || "-".equals(normalized) || "—".equals(normalized)) {
            return null;
        }
        Matcher matcher = Pattern.compile("[\\d.]+").matcher(normalized);
        return matcher.find() ? new BigDecimal(matcher.group()) : null;
    }

    private String detectCurrency(List<String> amountRow) {
        if (!amountRow.isEmpty() && normalize(amountRow.get(0)).contains("美元")) {
            return "USD";
        }
        return "CNY";
    }

    private List<String> findRow(List<List<String>> rows, String... keywords) {
        for (List<String> row : rows) {
            if (row.isEmpty()) {
                continue;
            }
            String label = normalize(row.get(0));
            for (String keyword : keywords) {
                if (label.contains(keyword)) {
                    return row;
                }
            }
        }
        return List.of();
    }

    private String getColumnValue(List<String> row, int index) {
        return row.size() > index ? row.get(index) : "";
    }

    private Set<String> extractFundCodes(String text) {
        Set<String> codes = new LinkedHashSet<>();
        Matcher matcher = FUND_CODE_PATTERN.matcher(text);
        while (matcher.find()) {
            codes.add(matcher.group());
        }
        return codes;
    }

    private boolean isDirectChannel(String title, String text) {
        return title.contains("直销") || text.contains("建信基金直销") || text.contains("直销渠道");
    }

    private boolean isOtherSalesChannel(String title, String text) {
        Matcher matcher = Pattern.compile("在([^，。]{1,30})渠道").matcher(title);
        return matcher.find() && !matcher.group(1).contains("直销");
    }

    private ExtractedDocument extractDocument(String attachmentName, byte[] attachment) {
        String lowerName = attachmentName.toLowerCase(Locale.ROOT);
        try {
            if (lowerName.endsWith(".docx")) {
                return extractDocx(attachment);
            }
            if (lowerName.endsWith(".doc")) {
                return extractDoc(attachment);
            }
            throw new IllegalArgumentException("不支持的公告附件格式");
        } catch (Exception e) {
            throw new IllegalStateException("解析建信基金公告附件失败", e);
        }
    }

    private ExtractedDocument extractDoc(byte[] attachment) throws Exception {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(attachment))) {
            List<List<String>> rows = new ArrayList<>();
            TableIterator iterator = new TableIterator(document.getRange());
            while (iterator.hasNext()) {
                Table table = iterator.next();
                for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
                    TableRow tableRow = table.getRow(rowIndex);
                    List<String> cells = new ArrayList<>();
                    for (int cellIndex = 0; cellIndex < tableRow.numCells(); cellIndex++) {
                        TableCell cell = tableRow.getCell(cellIndex);
                        cells.add(normalize(cell.text()));
                    }
                    rows.add(cells);
                }
            }
            return new ExtractedDocument(document.getRange().text(), rows);
        }
    }

    private ExtractedDocument extractDocx(byte[] attachment) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(attachment))) {
            List<List<String>> rows = new ArrayList<>();
            StringBuilder fullText = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> fullText.append(paragraph.getText()).append('\n'));
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow tableRow : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : tableRow.getTableCells()) {
                        String cellText = normalize(cell.getText());
                        cells.add(cellText);
                        fullText.append(cellText).append('\n');
                    }
                    rows.add(cells);
                }
            }
            return new ExtractedDocument(fullText.toString(), rows);
        }
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value)
                .replace('\u0007', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static class ExtractedDocument {

        private final String fullText;
        private final List<List<String>> tables;

        private ExtractedDocument(String fullText, List<List<String>> tables) {
            this.fullText = fullText;
            this.tables = tables;
        }

        public String getFullText() {
            return fullText;
        }

        public List<List<String>> getTables() {
            return tables;
        }
    }

}
