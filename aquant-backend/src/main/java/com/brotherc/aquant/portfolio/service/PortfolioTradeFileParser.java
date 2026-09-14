package com.brotherc.aquant.portfolio.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.utils.DigestUtils;
import com.brotherc.aquant.portfolio.model.dto.PortfolioTradeFileParseResult;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeImportErrorVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.zip.ZipInputStream;

/**
 * 解析 AQuant 标准交易流水模板。券商专用格式后续可以在导入服务前增加转换器，
 * 最终仍转换为相同的交易流水对象，统一复用幂等和持仓重算逻辑。
 */
@Component
public class PortfolioTradeFileParser {

    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    public static final int MAX_ROWS = 5000;

    private static final List<String> TEMPLATE_HEADERS = List.of(
            "资产类型", "市场", "证券代码", "证券名称", "交易类型", "交易时间", "交收日期",
            "数量", "成交价格", "成交金额", "佣金", "印花税", "过户费", "其他费用",
            "现金净发生额", "币种", "券商流水号", "备注"
    );
    private static final Map<String, String> HEADER_ALIASES = createHeaderAliases();
    private static final Map<String, String> ASSET_TYPE_ALIASES = Map.ofEntries(
            Map.entry("股票", "STOCK"), Map.entry("STOCK", "STOCK"),
            Map.entry("ETF", "ETF"), Map.entry("基金", "FUND"), Map.entry("FUND", "FUND"),
            Map.entry("债券", "BOND"), Map.entry("BOND", "BOND"),
            Map.entry("现金", "CASH"), Map.entry("CASH", "CASH")
    );
    private static final Map<String, String> TRADE_TYPE_ALIASES = Map.ofEntries(
            Map.entry("买入", "BUY"), Map.entry("BUY", "BUY"),
            Map.entry("卖出", "SELL"), Map.entry("SELL", "SELL"),
            Map.entry("申购", "SUBSCRIBE"), Map.entry("SUBSCRIBE", "SUBSCRIBE"),
            Map.entry("赎回", "REDEEM"), Map.entry("REDEEM", "REDEEM"),
            Map.entry("转入", "TRANSFER_IN"), Map.entry("TRANSFER_IN", "TRANSFER_IN"),
            Map.entry("转出", "TRANSFER_OUT"), Map.entry("TRANSFER_OUT", "TRANSFER_OUT"),
            Map.entry("红利再投", "DIVIDEND_SHARE"), Map.entry("DIVIDEND_SHARE", "DIVIDEND_SHARE"),
            Map.entry("现金分红", "DIVIDEND_CASH"), Map.entry("DIVIDEND_CASH", "DIVIDEND_CASH"),
            Map.entry("费用", "FEE"), Map.entry("FEE", "FEE"),
            Map.entry("税费", "TAX"), Map.entry("TAX", "TAX"),
            Map.entry("利息", "INTEREST"), Map.entry("INTEREST", "INTEREST"),
            Map.entry("银证转入", "CASH_DEPOSIT"), Map.entry("CASH_DEPOSIT", "CASH_DEPOSIT"),
            Map.entry("银证转出", "CASH_WITHDRAW"), Map.entry("CASH_WITHDRAW", "CASH_WITHDRAW"),
            Map.entry("期初持仓", "POSITION_INIT"), Map.entry("POSITION_INIT", "POSITION_INIT")
    );
    private static final Set<String> POSITION_TYPES = Set.of(
            "BUY", "SELL", "SUBSCRIBE", "REDEEM", "TRANSFER_IN", "TRANSFER_OUT", "DIVIDEND_SHARE", "POSITION_INIT"
    );
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.BASIC_ISO_DATE
    );

    public PortfolioTradeFileParseResult parse(String originalFileName, byte[] content) {
        String fileName = sanitizeFileName(originalFileName);
        String extension = StringUtils.substringAfterLast(fileName, ".").toLowerCase(Locale.ROOT);
        if (!Set.of("csv", "xls", "xlsx").contains(extension)) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "仅支持 .csv、.xls、.xlsx 文件");
        }
        if (content.length == 0 || content.length > MAX_FILE_SIZE) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "文件不能为空且不能超过 5 MB");
        }

        List<List<String>> rows;
        try {
            if ("xlsx".equals(extension)) {
                validateXlsxArchive(content);
            }
            rows = "csv".equals(extension) ? readCsv(content) : readWorkbook(content);
        } catch (Exception e) {
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "无法读取交易文件，请确认文件未损坏：" + StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
        }
        return convert(fileName, content, rows);
    }

    public byte[] createTemplate(String format) {
        if ("csv".equalsIgnoreCase(format)) {
            String content = "\uFEFF" + toCsvLine(TEMPLATE_HEADERS) + "\r\n";
            return content.getBytes(StandardCharsets.UTF_8);
        }
        if (!"xlsx".equalsIgnoreCase(format)) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "模板格式仅支持 csv 或 xlsx");
        }
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("交易流水");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            Row header = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
                Cell headerCell = header.createCell(i);
                headerCell.setCellValue(TEMPLATE_HEADERS.get(i));
                headerCell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, Math.min(28, Math.max(12, TEMPLATE_HEADERS.get(i).length() * 3)) * 256);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID, "生成交易模板失败");
        }
    }

    private PortfolioTradeFileParseResult convert(String fileName, byte[] content, List<List<String>> rows) {
        if (rows.isEmpty()) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "文件中没有表头");
        }
        Map<String, Integer> columns = new HashMap<>();
        List<String> headers = rows.get(0);
        for (int i = 0; i < headers.size(); i++) {
            String field = HEADER_ALIASES.get(normalizeHeader(headers.get(i)));
            if (field != null && !columns.containsKey(field)) {
                columns.put(field, i);
            }
        }
        List<String> missingHeaders = List.of("assetType", "tradeType", "tradeTime").stream()
                .filter(field -> !columns.containsKey(field)).map(this::headerName).toList();
        if (!missingHeaders.isEmpty()) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "缺少必要表头：" + String.join("、", missingHeaders));
        }

        PortfolioTradeFileParseResult result = new PortfolioTradeFileParseResult();
        result.setFileName(fileName);
        result.setFileHash(DigestUtils.sha256(content));
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.stream().allMatch(StringUtils::isBlank)) {
                continue;
            }
            result.setTotalCount(result.getTotalCount() + 1);
            if (result.getTotalCount() > MAX_ROWS) {
                throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                        "单个文件最多允许 5000 条交易流水");
            }
            try {
                result.getTrades().add(toTrade(row, columns));
            } catch (IllegalArgumentException e) {
                result.getErrors().add(new PortfolioTradeImportErrorVO(i + 1, e.getMessage()));
            }
        }
        if (result.getTotalCount() == 0) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "文件中没有可导入的数据行");
        }
        return result;
    }

    private PortfolioTradeSaveReqVO toTrade(List<String> row, Map<String, Integer> columns) {
        PortfolioTradeSaveReqVO trade = new PortfolioTradeSaveReqVO();
        String assetTypeText = value(row, columns, "assetType").toUpperCase(Locale.ROOT);
        String tradeTypeText = value(row, columns, "tradeType").toUpperCase(Locale.ROOT);
        trade.setAssetType(ASSET_TYPE_ALIASES.get(assetTypeText));
        trade.setTradeType(TRADE_TYPE_ALIASES.get(tradeTypeText));
        if (trade.getAssetType() == null) {
            throw new IllegalArgumentException("资产类型不支持：" + assetTypeText);
        }
        if (trade.getTradeType() == null) {
            throw new IllegalArgumentException("交易类型不支持：" + tradeTypeText);
        }

        trade.setMarket(parseMarket(value(row, columns, "market")));
        trade.setAssetCode(StringUtils.trimToNull(value(row, columns, "assetCode")));
        trade.setAssetName(StringUtils.trimToNull(value(row, columns, "assetName")));
        trade.setTradeTime(parseDateTime(value(row, columns, "tradeTime"), "交易时间"));
        trade.setSettlementDate(parseDate(value(row, columns, "settlementDate"), "交收日期", false));
        trade.setQuantity(parseDecimal(value(row, columns, "quantity"), "数量", false));
        trade.setPrice(parseDecimal(value(row, columns, "price"), "成交价格", false));
        trade.setGrossAmount(parseDecimal(value(row, columns, "grossAmount"), "成交金额", false));
        trade.setCommission(parseDecimal(value(row, columns, "commission"), "佣金", false));
        trade.setStampDuty(parseDecimal(value(row, columns, "stampDuty"), "印花税", false));
        trade.setTransferFee(parseDecimal(value(row, columns, "transferFee"), "过户费", false));
        trade.setOtherFee(parseDecimal(value(row, columns, "otherFee"), "其他费用", false));
        trade.setNetAmount(parseDecimal(value(row, columns, "netAmount"), "现金净发生额", false));
        trade.setCurrency(StringUtils.defaultIfBlank(value(row, columns, "currency"), "CNY").toUpperCase(Locale.ROOT));
        trade.setSourceTradeId(StringUtils.trimToNull(value(row, columns, "sourceTradeId")));
        trade.setRemark(StringUtils.trimToNull(value(row, columns, "remark")));

        if (POSITION_TYPES.contains(trade.getTradeType())) {
            if (StringUtils.isBlank(trade.getAssetCode())) {
                throw new IllegalArgumentException("持仓类流水必须填写证券代码");
            }
            if (trade.getQuantity() == null || trade.getQuantity().signum() <= 0) {
                throw new IllegalArgumentException("持仓类流水的数量必须大于 0");
            }
            if (!"DIVIDEND_SHARE".equals(trade.getTradeType()) && trade.getGrossAmount() == null
                    && (trade.getPrice() == null || trade.getPrice().signum() < 0)) {
                throw new IllegalArgumentException("请填写非负的成交价格或成交金额");
            }
        }
        for (BigDecimal fee : new BigDecimal[]{
                trade.getCommission(), trade.getStampDuty(), trade.getTransferFee(), trade.getOtherFee()}) {
            if (fee != null && fee.signum() < 0) {
                throw new IllegalArgumentException("各项费用不能小于 0");
            }
        }
        return trade;
    }

    private List<List<String>> readWorkbook(byte[] content) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            if (workbook.getNumberOfSheets() == 0) {
                return List.of();
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            List<List<String>> rows = new ArrayList<>();
            int lastRow = Math.min(sheet.getLastRowNum(), MAX_ROWS + 100);
            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<String> values = new ArrayList<>();
                int lastCell = row == null ? 0 : Math.min(row.getLastCellNum(), 50);
                for (int columnIndex = 0; columnIndex < lastCell; columnIndex++) {
                    Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        values.add(cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } else {
                        values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                    }
                }
                rows.add(values);
            }
            return rows;
        }
    }

    private void validateXlsxArchive(byte[] content) throws IOException {
        long expandedSize = 0;
        int entryCount = 0;
        byte[] buffer = new byte[8192];
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(content))) {
            while (input.getNextEntry() != null) {
                entryCount++;
                if (entryCount > 200) {
                    throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                            "Excel 文件包含过多内部条目");
                }
                int read;
                while ((read = input.read(buffer)) != -1) {
                    expandedSize += read;
                    if (expandedSize > 25L * 1024 * 1024) {
                        throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                                "Excel 文件解压后超过 25 MB");
                    }
                }
            }
        }
        if (entryCount == 0) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "Excel 文件内容不完整");
        }
    }

    private List<List<String>> readCsv(byte[] content) throws CharacterCodingException {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException e) {
            text = java.nio.charset.Charset.forName("GB18030").newDecoder().decode(ByteBuffer.wrap(content)).toString();
        }
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                row.add(value.toString().trim());
                value.setLength(0);
            } else if ((ch == '\n' || ch == '\r') && !quoted) {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(value.toString().trim());
                rows.add(row);
                row = new ArrayList<>();
                value.setLength(0);
            } else {
                value.append(ch);
            }
        }
        if (quoted) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "CSV 文件存在未闭合的双引号");
        }
        if (!row.isEmpty() || !value.isEmpty()) {
            row.add(value.toString().trim());
            rows.add(row);
        }
        return rows;
    }

    private LocalDateTime parseDateTime(String text, String field) {
        if (StringUtils.isBlank(text)) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种常见格式。
            }
        }
        LocalDate date = parseDate(text, field, true);
        return date.atStartOfDay();
    }

    private LocalDate parseDate(String text, String field, boolean required) {
        if (StringUtils.isBlank(text)) {
            if (required) {
                throw new IllegalArgumentException(field + "不能为空");
            }
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种常见格式。
            }
        }
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(text, formatter).toLocalDate();
            } catch (DateTimeParseException ignored) {
                // Excel 日期单元格可能包含午夜时间，继续尝试其他格式。
            }
        }
        throw new IllegalArgumentException(field + "格式应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
    }

    private BigDecimal parseDecimal(String text, String field, boolean required) {
        if (StringUtils.isBlank(text)) {
            if (required) {
                throw new IllegalArgumentException(field + "不能为空");
            }
            return null;
        }
        try {
            return new BigDecimal(text.replace(",", "").replace("¥", "").trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + "不是有效数字");
        }
    }

    private String value(List<String> row, Map<String, Integer> columns, String field) {
        Integer index = columns.get(field);
        return index == null || index >= row.size() ? "" : row.get(index).trim();
    }

    private String parseMarket(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "SH", "上海", "上交所", "沪市", "沪A" -> "SH";
            case "SZ", "深圳", "深交所", "深市", "深A" -> "SZ";
            case "BJ", "北京", "北交所", "北市" -> "BJ";
            default -> throw new IllegalArgumentException("市场仅支持 SH/SZ/BJ 或对应中文名称");
        };
    }

    private String headerName(String field) {
        return switch (field) {
            case "assetType" -> "资产类型";
            case "tradeType" -> "交易类型";
            case "tradeTime" -> "交易时间";
            default -> field;
        };
    }

    private static Map<String, String> createHeaderAliases() {
        Map<String, String> aliases = new HashMap<>();
        addAliases(aliases, "assetType", "资产类型", "品种类型", "assettype");
        addAliases(aliases, "market", "市场", "交易市场", "market");
        addAliases(aliases, "assetCode", "证券代码", "股票代码", "基金代码", "代码", "assetcode", "symbol");
        addAliases(aliases, "assetName", "证券名称", "股票名称", "基金名称", "名称", "assetname");
        addAliases(aliases, "tradeType", "交易类型", "业务类型", "买卖标志", "tradetype");
        addAliases(aliases, "tradeTime", "交易时间", "成交时间", "发生时间", "tradetime");
        addAliases(aliases, "settlementDate", "交收日期", "清算日期", "settlementdate");
        addAliases(aliases, "quantity", "数量", "成交数量", "发生数量", "quantity");
        addAliases(aliases, "price", "成交价格", "成交均价", "价格", "price");
        addAliases(aliases, "grossAmount", "成交金额", "发生金额", "grossamount", "amount");
        addAliases(aliases, "commission", "佣金", "手续费", "commission");
        addAliases(aliases, "stampDuty", "印花税", "stampduty");
        addAliases(aliases, "transferFee", "过户费", "transferfee");
        addAliases(aliases, "otherFee", "其他费用", "其它费用", "otherfee");
        addAliases(aliases, "netAmount", "现金净发生额", "资金发生额", "清算金额", "netamount");
        addAliases(aliases, "currency", "币种", "货币", "currency");
        addAliases(aliases, "sourceTradeId", "券商流水号", "成交编号", "流水号", "合同编号", "sourcetradeid");
        addAliases(aliases, "remark", "备注", "摘要", "remark");
        return aliases;
    }

    private static void addAliases(Map<String, String> aliases, String field, String... names) {
        for (String name : names) {
            aliases.put(normalizeHeader(name), field);
        }
    }

    private static String normalizeHeader(String value) {
        return StringUtils.defaultString(value).replaceAll("[\\s_()（）/-]", "").toLowerCase(Locale.ROOT);
    }

    private static String sanitizeFileName(String value) {
        String normalized = StringUtils.defaultIfBlank(value, "trade-file").replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private static String toCsvLine(List<String> values) {
        return values.stream().map(value -> '"' + value.replace("\"", "\"\"") + '"')
                .reduce((left, right) -> left + "," + right).orElse("");
    }

}
