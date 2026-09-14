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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * 招商证券资金流水适配器。
 *
 * <p>招商证券导出的“xls”实际可能是 GB18030 编码的制表符文本，单元格使用
 * {@code ="值"} 包装，因此不能仅根据文件扩展名交给 Excel 解析器。</p>
 */
@Component
@Order(10)
public class CMSSecuritiesTradeFileAdapter implements BrokerTradeFileAdapter {

    private static final String BROKER_CODE = "CMS";
    private static final int MAX_ROWS = 5000;
    private static final Set<String> REQUIRED_HEADERS = Set.of("成交日期", "业务名称", "流水号", "发生金额");
    private static final Set<String> ETF_PREFIXES = Set.of("15", "16", "50", "51", "52", "53", "56", "58");
    private static final Set<String> POSITION_BUSINESS_NAMES = Set.of(
            "证券买入", "证券卖出", "新股入账", "市值申购中签", "托管转出"
    );

    @Override
    public boolean supports(String brokerCode, String fileName, byte[] content) {
        if (!BROKER_CODE.equalsIgnoreCase(brokerCode) || content.length == 0) {
            return false;
        }
        try {
            String sample = decode(Arrays.copyOf(content, Math.min(content.length, 4096)));
            return sample.contains("业务名称") && sample.contains("流水号") && sample.indexOf('\t') >= 0;
        } catch (BusinessException e) {
            return false;
        }
    }

    @Override
    public PortfolioTradeFileParseResult parse(String fileName, byte[] content) {
        List<SourceRow> sourceRows = readRows(content);
        if (sourceRows.isEmpty()) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "招商证券资金流水中没有可读取的数据");
        }

        Map<String, BigDecimal> subscriptionPayments = new HashMap<>();
        for (SourceRow sourceRow : sourceRows) {
            if ("新股申购确认缴款".equals(sourceRow.get("业务名称"))) {
                String contractNo = sourceRow.get("合同编号");
                BigDecimal amount = StockUtils.decimal(sourceRow.get("发生金额"), "发生金额");
                if (StringUtils.isNotBlank(contractNo) && amount != null) {
                    subscriptionPayments.put(contractNo, amount.abs());
                }
            }
        }

        PortfolioTradeFileParseResult result = new PortfolioTradeFileParseResult();
        result.setFileName(sanitizeFileName(fileName));
        result.setFileHash(DigestUtils.sha256(content));
        sourceRows.stream()
                .filter(row -> StringUtils.isNotBlank(row.get("资金余额")))
                .map(row -> StockUtils.parseTradeDate(row.required("成交日期")))
                .max(LocalDate::compareTo).ifPresent(latestTradeDate -> sourceRows.stream()
                        .filter(row -> StringUtils.isNotBlank(row.get("资金余额")))
                        .filter(row -> latestTradeDate.equals(StockUtils.parseTradeDate(row.required("成交日期"))))
                        .findFirst()
                        .ifPresent(row -> {
                            result.setLatestCashBalance(StockUtils.decimal(row.get("资金余额"), "资金余额"));
                            result.setCashBalanceDate(latestTradeDate);
                            result.setCashBalanceCurrency(currency(row.get("币种")));
                        }));
        for (SourceRow sourceRow : sourceRows) {
            result.setTotalCount(result.getTotalCount() + 1);
            if (result.getTotalCount() > MAX_ROWS) {
                throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                        "单个文件最多允许 5000 条交易流水");
            }
            try {
                result.getTrades().add(toTrade(sourceRow, subscriptionPayments));
            } catch (IllegalArgumentException e) {
                result.getErrors().add(new PortfolioTradeImportErrorVO(sourceRow.getRowNumber(), e.getMessage()));
            }
        }
        return result;
    }

    private PortfolioTradeSaveReqVO toTrade(SourceRow row, Map<String, BigDecimal> subscriptionPayments) {
        String businessName = row.required("业务名称");
        String assetCode = StringUtils.trimToNull(row.get("证券代码"));
        String assetName = StringUtils.trimToNull(row.get("证券名称"));
        BigDecimal quantity = StockUtils.decimal(row.get("成交数量"), "成交数量");
        BigDecimal price = StockUtils.decimal(row.get("成交价格"), "成交价格");
        BigDecimal netAmount = StockUtils.decimal(row.get("发生金额"), "发生金额");
        BigDecimal commission = StockUtils.decimal(row.get("佣金"), "佣金");
        BigDecimal stampDuty = StockUtils.decimal(row.get("印花税"), "印花税");
        BigDecimal transferFee = StockUtils.decimal(row.get("过户费"), "过户费");

        PortfolioTradeSaveReqVO trade = new PortfolioTradeSaveReqVO();
        trade.setTradeType(tradeType(businessName));
        trade.setAssetType(assetType(businessName, assetCode));
        trade.setAssetCode(assetCode);
        trade.setAssetName(assetName);
        trade.setMarket(StockUtils.market(assetCode));
        trade.setTradeTime(StockUtils.parseTradeDate(row.required("成交日期")).atStartOfDay());
        trade.setCurrency(currency(row.get("币种")));
        trade.setSourceTradeId(row.required("流水号"));
        trade.setNetAmount(netAmount);
        trade.setCommission(commission == null ? BigDecimal.ZERO : commission);
        trade.setStampDuty(stampDuty == null ? BigDecimal.ZERO : stampDuty);
        trade.setTransferFee(transferFee == null ? BigDecimal.ZERO : transferFee);
        trade.setOtherFee(sum(row, "经手费", "证管费", "结算费", "其他费用"));
        trade.setRemark(StringUtils.isBlank(row.get("备注"))
                ? "招商证券：" + businessName
                : "招商证券：" + businessName + "；" + row.get("备注"));

        if (POSITION_BUSINESS_NAMES.contains(businessName)) {
            if (StringUtils.isBlank(assetCode) || quantity == null || quantity.signum() == 0) {
                throw new IllegalArgumentException(businessName + "缺少有效的证券代码或成交数量");
            }
            trade.setQuantity(quantity.abs());
            trade.setPrice(price == null ? BigDecimal.ZERO : price.abs());
            if ("新股入账".equals(businessName)) {
                trade.setGrossAmount(subscriptionPayments.getOrDefault(row.get("合同编号"), BigDecimal.ZERO));
            } else if ("市值申购中签".equals(businessName) || "托管转出".equals(businessName)) {
                trade.setGrossAmount(BigDecimal.ZERO);
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
            case "证券买入" -> "BUY";
            case "证券卖出" -> "SELL";
            case "股息入账" -> "DIVIDEND_CASH";
            case "股息红利税补缴" -> "TAX";
            case "利息归本" -> "INTEREST";
            case "银行转存", "市值申购中签扣款回冲" -> "CASH_DEPOSIT";
            case "银行转取", "新股申购确认缴款", "市值申购中签扣款" -> "CASH_WITHDRAW";
            case "新股入账", "市值申购中签" -> "TRANSFER_IN";
            case "托管转出" -> "TRANSFER_OUT";
            default -> throw new IllegalArgumentException("暂不支持招商证券业务类型：" + businessName);
        };
    }

    private String assetType(String businessName, String assetCode) {
        if (StringUtils.isBlank(assetCode)) {
            return "CASH";
        }
        if (businessName.contains("新股") || businessName.contains("中签") || businessName.contains("托管")) {
            return StockUtils.isBond(assetCode) ? "BOND" : "STOCK";
        }
        if (assetCode.length() >= 2 && ETF_PREFIXES.contains(assetCode.substring(0, 2))) {
            return "ETF";
        }
        return StockUtils.isBond(assetCode) ? "BOND" : "STOCK";
    }

    private List<SourceRow> readRows(byte[] content) {
        String[] lines = decode(content).split("\\R", -1);
        int headerLine = -1;
        List<String> headers = List.of();
        for (int i = 0; i < Math.min(lines.length, 20); i++) {
            List<String> values = splitLine(lines[i]);
            if (new HashSet<>(values).containsAll(REQUIRED_HEADERS)) {
                headerLine = i;
                headers = values;
                break;
            }
        }
        if (headerLine < 0) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "无法识别招商证券资金流水表头");
        }

        List<SourceRow> rows = new ArrayList<>();
        for (int i = headerLine + 1; i < lines.length; i++) {
            List<String> values = splitLine(lines[i]);
            if (values.stream().allMatch(StringUtils::isBlank)) {
                continue;
            }
            Map<String, String> data = new HashMap<>();
            for (int column = 0; column < Math.min(headers.size(), values.size()); column++) {
                if (StringUtils.isNotBlank(headers.get(column))) {
                    data.put(headers.get(column), values.get(column));
                }
            }
            rows.add(new SourceRow(i + 1, data));
        }
        return rows;
    }

    private List<String> splitLine(String line) {
        return Arrays.stream(line.split("\\t", -1)).map(this::unwrapCell).toList();
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
                return java.nio.charset.Charset.forName("GB18030").newDecoder().decode(ByteBuffer.wrap(content)).toString();
            } catch (CharacterCodingException ex) {
                throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                        "招商证券资金流水编码无法识别");
            }
        }
    }

    private BigDecimal sum(SourceRow row, String... fields) {
        BigDecimal result = BigDecimal.ZERO;
        for (String field : fields) {
            BigDecimal value = StockUtils.decimal(row.get(field), field);
            result = result.add(value == null ? BigDecimal.ZERO : value);
        }
        return result;
    }

    private String currency(String value) {
        return switch (StringUtils.defaultIfBlank(value, "人民币").toUpperCase(Locale.ROOT)) {
            case "人民币", "CNY", "RMB" -> "CNY";
            case "港币", "HKD" -> "HKD";
            case "美元", "USD" -> "USD";
            default -> throw new IllegalArgumentException("暂不支持币种：" + value);
        };
    }

    private String sanitizeFileName(String value) {
        String normalized = StringUtils.defaultIfBlank(value, "cms-trade-file.xls").replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

}
