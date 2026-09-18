package com.brotherc.aquant.portfolio.service.importer;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.utils.DigestUtils;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.portfolio.model.dto.PortfolioTradeFileParseResult;
import com.brotherc.aquant.portfolio.model.dto.SourceRow;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeImportErrorVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 东方财富证券交割单（对账单）适配器。
 *
 * <p>东方财富 App/网页导出的交割单为真正的 xlsx/CSV，表头以「交割日期」为特征列
 * （区别于招商证券的「成交日期」），一文件包含多只证券的成交流水。列名在不同导出
 * 渠道间存在变体（如「流水号/合同编号/成交编号」「手续费/佣金」），统一走宽容别名映射。</p>
 */
@Component
@Order(20)
public class EastmoneyTradeFileAdapter implements BrokerTradeFileAdapter {

    private static final String BROKER_CODE = "EM";
    private static final int MAX_ROWS = 5000;
    /** 表头行识别特征：东财交割单独有「交割日期」，配合业务/金额列足以与其他券商文件区分 */
    private static final Set<String> HEADER_SIGNATURE = Set.of("交割日期", "业务名称", "证券代码", "发生金额");
    private static final Set<String> POSITION_BUSINESS_NAMES = Set.of(
            "证券买入", "证券卖出", "担保品买入", "担保品卖出", "配股", "新股入账", "中签入账", "送股入账",
            "基金申购", "场内申购", "基金赎回", "场内赎回"
    );
    private static final Set<String> ETF_PREFIXES = Set.of("15", "16", "50", "51", "52", "53", "56", "58");

    @Override
    public String brokerCode() {
        return BROKER_CODE;
    }

    @Override
    public boolean supports(String brokerCode, String fileName, byte[] content) {
        // 兼容前端券商选择器的 EASTMONEY 写法
        if ((!BROKER_CODE.equalsIgnoreCase(brokerCode) && !"EASTMONEY".equalsIgnoreCase(brokerCode))
                || content.length == 0) {
            return false;
        }
        try {
            List<List<String>> rows = readRows(content, 20);
            for (List<String> row : rows) {
                if (new HashSet<>(row).containsAll(HEADER_SIGNATURE)) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public PortfolioTradeFileParseResult parse(String fileName, byte[] content) {
        List<SourceRow> sourceRows = toSourceRows(content);
        if (sourceRows.isEmpty()) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "东方财富交割单中没有可读取的数据");
        }

        PortfolioTradeFileParseResult result = new PortfolioTradeFileParseResult();
        result.setFileName(sanitizeFileName(fileName));
        result.setFileHash(DigestUtils.sha256(content));
        fillLatestCashBalance(result, sourceRows);
        for (SourceRow sourceRow : sourceRows) {
            result.setTotalCount(result.getTotalCount() + 1);
            if (result.getTotalCount() > MAX_ROWS) {
                throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                        "单个文件最多允许 5000 条交易流水");
            }
            try {
                result.getTrades().add(toTrade(sourceRow));
            } catch (IllegalArgumentException e) {
                result.getErrors().add(new PortfolioTradeImportErrorVO(sourceRow.getRowNumber(), e.getMessage()));
            }
        }
        return result;
    }

    private void fillLatestCashBalance(PortfolioTradeFileParseResult result, List<SourceRow> sourceRows) {
        LocalDate latestTradeDate = sourceRows.stream()
                .filter(row -> StringUtils.isNotBlank(row.get("资金余额")))
                .map(row -> parseDate(row.required("交割日期")))
                .max(LocalDate::compareTo).orElse(null);
        if (latestTradeDate == null) {
            return;
        }
        // 东财交割单按时间正序排列，最新余额取最新交割日期的最后一行
        List<SourceRow> sameDateRows = sourceRows.stream()
                .filter(row -> StringUtils.isNotBlank(row.get("资金余额")))
                .filter(row -> latestTradeDate.equals(parseDate(row.required("交割日期"))))
                .toList();
        if (sameDateRows.isEmpty()) {
            return;
        }
        SourceRow row = sameDateRows.get(sameDateRows.size() - 1);
        result.setLatestCashBalance(StockUtils.decimal(row.get("资金余额"), "资金余额"));
        result.setCashBalanceDate(latestTradeDate);
        result.setCashBalanceCurrency(currency(row.get("币种")));
    }

    private PortfolioTradeSaveReqVO toTrade(SourceRow row) {
        String businessName = row.required("业务名称");
        String assetCode = StringUtils.trimToNull(row.get("证券代码"));
        String assetName = StringUtils.trimToNull(row.get("证券名称"));
        BigDecimal quantity = StockUtils.decimal(row.get("成交数量"), "成交数量");
        BigDecimal price = StockUtils.decimal(row.get("成交价格"), "成交价格");
        BigDecimal tradeAmount = StockUtils.decimal(row.get("成交金额"), "成交金额");
        BigDecimal netAmount = StockUtils.decimal(row.get("发生金额"), "发生金额");

        PortfolioTradeSaveReqVO trade = new PortfolioTradeSaveReqVO();
        trade.setTradeType(tradeType(businessName));
        trade.setAssetType(assetType(assetCode));
        trade.setAssetCode(assetCode);
        trade.setAssetName(assetName);
        trade.setMarket(StockUtils.market(assetCode));
        trade.setTradeTime(parseDate(row.required("交割日期")).atStartOfDay());
        trade.setCurrency(currency(row.get("币种")));
        trade.setSourceTradeId(firstNonBlank(row.get("流水号"), row.get("合同编号"), row.get("成交编号")));
        trade.setNetAmount(netAmount);
        trade.setCommission(fee(row, "手续费", "佣金"));
        trade.setStampDuty(fee(row, "印花税"));
        trade.setTransferFee(fee(row, "过户费"));
        trade.setOtherFee(fee(row, "结算费", "规费", "经手费", "证管费", "其他费用"));
        trade.setRemark(remark(row, businessName));

        if (POSITION_BUSINESS_NAMES.contains(businessName)) {
            if (StringUtils.isBlank(assetCode) || quantity == null || quantity.signum() == 0) {
                throw new IllegalArgumentException(businessName + "缺少有效的证券代码或成交数量");
            }
            trade.setQuantity(quantity.abs());
            trade.setPrice(price == null || price.signum() == 0
                    ? BigDecimal.ZERO : price.abs());
            if ("新股入账".equals(businessName) || "中签入账".equals(businessName) || "送股入账".equals(businessName)) {
                trade.setGrossAmount(BigDecimal.ZERO);
            } else if (tradeAmount != null && tradeAmount.signum() != 0) {
                trade.setGrossAmount(tradeAmount.abs());
            } else {
                trade.setGrossAmount(trade.getPrice().multiply(trade.getQuantity()));
            }
        } else {
            trade.setGrossAmount(netAmount == null ? null : netAmount.abs());
        }
        return trade;
    }

    private String tradeType(String businessName) {
        return switch (businessName) {
            case "证券买入", "担保品买入", "配股", "证券买入(配股)" -> "BUY";
            case "证券卖出", "担保品卖出" -> "SELL";
            case "股息入账", "红利入账" -> "DIVIDEND_CASH";
            case "股息红利税补缴", "红利补税", "利息税", "红利税" -> "TAX";
            case "利息归本" -> "INTEREST";
            case "银行转存", "银行转入", "资金转入" -> "CASH_DEPOSIT";
            case "银行转取", "银行转出", "资金转出" -> "CASH_WITHDRAW";
            case "新股入账", "中签入账", "送股入账" -> "TRANSFER_IN";
            case "基金申购", "场内申购" -> "SUBSCRIBE";
            case "基金赎回", "场内赎回" -> "REDEEM";
            default -> throw new IllegalArgumentException("暂不支持东方财富业务类型：" + businessName);
        };
    }

    private String assetType(String assetCode) {
        if (StringUtils.isBlank(assetCode)) {
            return "CASH";
        }
        if (assetCode.length() >= 2 && ETF_PREFIXES.contains(assetCode.substring(0, 2))) {
            return "ETF";
        }
        return StockUtils.isBond(assetCode) ? "BOND" : "STOCK";
    }

    private String remark(SourceRow row, String businessName) {
        return StringUtils.isBlank(row.get("备注"))
                ? "东方财富：" + businessName
                : "东方财富：" + businessName + "；" + row.get("备注");
    }

    private List<SourceRow> toSourceRows(byte[] content) {
        List<List<String>> rows = readRows(content, MAX_ROWS + 100);
        int headerLine = -1;
        List<String> headers = List.of();
        for (int i = 0; i < Math.min(rows.size(), 20); i++) {
            if (new HashSet<>(rows.get(i)).containsAll(HEADER_SIGNATURE)) {
                headerLine = i;
                headers = rows.get(i);
                break;
            }
        }
        if (headerLine < 0) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "无法识别东方财富交割单表头，请确认导出的是交割单（对账单）明细文件");
        }
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = normalize(headers.get(i));
            if (StringUtils.isNotBlank(header) && !columns.containsKey(header)) {
                columns.put(header, i);
            }
        }
        List<SourceRow> result = new ArrayList<>();
        for (int i = headerLine + 1; i < rows.size(); i++) {
            List<String> values = rows.get(i);
            if (values.stream().allMatch(StringUtils::isBlank)) {
                continue;
            }
            Map<String, String> data = new HashMap<>();
            columns.forEach((header, index) -> data.put(header,
                    index < values.size() ? values.get(index) : ""));
            result.add(new SourceRow(i + 1, data));
        }
        return result;
    }

    /**
     * 读文件为行列文本。xlsx/xls（zip 魔数）走 POI，其余按 UTF-8 → GB18030 文本解析；
     * 单元格允许 ="值" 包装（券商导出软件常见格式）。
     */
    private List<List<String>> readRows(byte[] content, int maxRows) {
        if (content.length >= 2 && content[0] == 'P' && content[1] == 'K') {
            return readWorkbook(content, maxRows);
        }
        return readText(decode(content), maxRows);
    }

    private List<List<String>> readWorkbook(byte[] content, int maxRows) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            List<List<String>> rows = new ArrayList<>();
            if (workbook.getNumberOfSheets() == 0) {
                return rows;
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            int lastRow = Math.min(sheet.getLastRowNum(), maxRows);
            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<String> values = new ArrayList<>();
                int lastCell = row == null ? 0 : Math.min(row.getLastCellNum(), 50);
                for (int column = 0; column < lastCell; column++) {
                    Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                }
                rows.add(values);
            }
            return rows;
        } catch (Exception e) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "无法读取东方财富交割单文件，请确认文件未损坏");
        }
    }

    private List<List<String>> readText(String text, int maxRows) {
        String[] lines = text.split("\\R", -1);
        boolean tabbed = lines.length > 0 && lines[0].indexOf('\t') >= 0;
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < Math.min(lines.length, maxRows); i++) {
            List<String> values = Arrays.stream(lines[i].split(tabbed ? "\\t" : ",", -1))
                    .map(this::unwrapCell).toList();
            rows.add(values);
        }
        return rows;
    }

    private String unwrapCell(String value) {
        String text = value.trim();
        if (text.startsWith("=\"") && text.endsWith("\"") && text.length() >= 3) {
            return text.substring(2, text.length() - 1).replace("\"\"", "\"").trim();
        }
        return text;
    }

    private String decode(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException e) {
            try {
                return java.nio.charset.Charset.forName("GB18030").newDecoder()
                        .decode(ByteBuffer.wrap(content)).toString();
            } catch (CharacterCodingException ex) {
                throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                        "东方财富交割单编码无法识别");
            }
        }
    }

    private LocalDate parseDate(String text) {
        String value = StringUtils.trimToEmpty(text);
        if (value.length() > 10 && value.charAt(10) == ' ') {
            value = value.substring(0, 10);
        }
        for (DateTimeFormatter formatter : List.of(DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy/MM/dd"), DateTimeFormatter.BASIC_ISO_DATE)) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种常见格式。
            }
        }
        throw new IllegalArgumentException("交割日期格式无法识别：" + text);
    }

    private BigDecimal fee(SourceRow row, String... fields) {
        BigDecimal total = BigDecimal.ZERO;
        for (String field : fields) {
            BigDecimal value = StockUtils.decimal(row.get(field), field);
            total = total.add(value == null ? BigDecimal.ZERO : value.abs());
        }
        return total;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String currency(String value) {
        return switch (StringUtils.defaultIfBlank(value, "人民币").toUpperCase(Locale.ROOT)) {
            case "人民币", "CNY", "RMB" -> "CNY";
            case "港币", "HKD" -> "HKD";
            case "美元", "USD" -> "USD";
            default -> throw new IllegalArgumentException("暂不支持币种：" + value);
        };
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).replaceAll("[\\s_()（）/-]", "");
    }

    private String sanitizeFileName(String value) {
        String normalized = StringUtils.defaultIfBlank(value, "em-statement.xlsx").replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }
}
