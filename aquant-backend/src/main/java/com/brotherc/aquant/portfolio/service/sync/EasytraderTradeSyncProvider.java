package com.brotherc.aquant.portfolio.service.sync;

import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.integration.easytrader.EasytraderClient;
import com.brotherc.aquant.portfolio.model.vo.BrokerPositionSnapshotVO;
import com.brotherc.aquant.portfolio.model.vo.BrokerPositionSnapshotVO.BrokerPositionItemVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * easytrader 客户端渠道（UserBrokerAccount.syncMode = EASYTRADER）。
 *
 * <p>easytrader 以券商客户端 GUI 自动化方式提供 /position 与 /today_trades，
 * 网格列名随券商客户端版本变化，统一走宽容别名映射。当日成交网格不含费用明细
 * （费用次日出现在交割单），故佣金/印花税等按 0 记录，现金净发生额按买卖方向
 * 用成交金额推算；同一账户建议以客户端同步为主，避免再导交割单造成重复计账。</p>
 */
@Component
public class EasytraderTradeSyncProvider implements PortfolioBrokerSyncProvider {

    public static final String CHANNEL = "EASYTRADER";
    private static final Set<String> ETF_PREFIXES = Set.of("15", "16", "50", "51", "52", "53", "56", "58");

    private final EasytraderClient easytraderClient;

    public EasytraderTradeSyncProvider(EasytraderClient easytraderClient) {
        this.easytraderClient = easytraderClient;
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public List<PortfolioTradeSaveReqVO> fetchTrades() {
        return easytraderClient.todayTrades().stream().map(this::toTrade).toList();
    }

    @Override
    public BrokerPositionSnapshotVO fetchPositions() {
        BrokerPositionSnapshotVO snapshot = new BrokerPositionSnapshotVO();
        snapshot.setChannel(CHANNEL);
        snapshot.setFetchTime(java.time.LocalDateTime.now().toString());
        // easytrader /position 为券商客户端网格原始行（中文列名随客户端变化），
        // 只映射可稳定获取的代码/名称/数量/成本价/市值，盈亏类指标客户端不一定提供，留空
        for (Map<String, Object> row : easytraderClient.position()) {
            BrokerPositionItemVO item = new BrokerPositionItemVO();
            item.setSecurityCode(text(row, "证券代码", "代码"));
            item.setSecurityName(text(row, "证券名称", "名称"));
            item.setHoldingQuantity(decimal(row, "股票余额", "持仓数量", "证券余额"));
            item.setEnableQuantity(decimal(row, "可用余额", "可用数量"));
            item.setCostPrice(decimal(row, "成本价", "摊薄成本价"));
            item.setMarketValue(decimal(row, "市值", "最新市值"));
            snapshot.getPositions().add(item);
        }
        return snapshot;
    }

    private String text(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null && StringUtils.isNotBlank(value.toString())) {
                return value.toString().trim();
            }
        }
        return "";
    }

    private BigDecimal decimal(Map<String, Object> row, String... keys) {
        String value = text(row, keys);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private PortfolioTradeSaveReqVO toTrade(Map<String, Object> row) {
        String assetCode = text(row, "证券代码", "代码", "zqdm");
        if (StringUtils.isBlank(assetCode)) {
            throw new IllegalArgumentException("成交行缺少证券代码：" + row);
        }
        String operation = text(row, "操作", "买卖标志", "业务名称", "mmsm");
        String assetName = text(row, "证券名称", "名称", "zqmc");
        BigDecimal price = decimal(row, "成交价格", "成交均价");
        BigDecimal quantity = decimal(row, "成交数量", "数量");
        BigDecimal amount = decimal(row, "成交金额");
        String tradeId = StringUtils.trimToNull(text(row, "合同编号", "成交编号"));

        PortfolioTradeSaveReqVO trade = new PortfolioTradeSaveReqVO();
        trade.setTradeType(tradeType(operation, assetCode));
        trade.setAssetType(assetType(assetCode));
        trade.setMarket(StockUtils.market(assetCode));
        trade.setAssetCode(assetCode);
        trade.setAssetName(assetName);
        trade.setTradeTime(parseTradeTime(text(row, "成交日期", "委托日期"), text(row, "成交时间", "委托时间")));
        trade.setQuantity(quantity == null ? null : quantity.abs());
        trade.setPrice(price);
        trade.setGrossAmount(amount == null
                ? (price == null || quantity == null ? null : price.multiply(quantity).abs())
                : amount.abs());
        trade.setCommission(BigDecimal.ZERO);
        trade.setStampDuty(BigDecimal.ZERO);
        trade.setTransferFee(BigDecimal.ZERO);
        trade.setOtherFee(BigDecimal.ZERO);
        // 当日成交网格无费用明细：现金净发生额按方向由成交金额推算（买入扣减、卖出增加）
        trade.setNetAmount("BUY".equals(trade.getTradeType())
                ? (amount == null ? null : amount.abs().negate())
                : (amount == null ? null : amount.abs()));
        trade.setCurrency("CNY");
        trade.setSourceTradeId(tradeId);
        trade.setRemark("easytrader：当日成交" + (StringUtils.isBlank(operation) ? "" : "（" + operation + "）")
                + "，费用以后续交割单为准");
        return trade;
    }

    private String tradeType(String operation, String assetCode) {
        String text = StringUtils.defaultString(operation);
        if (text.contains("买")) {
            return "BUY";
        }
        if (text.contains("卖")) {
            return "SELL";
        }
        throw new IllegalArgumentException("暂不支持 easytrader 成交方向：" + operation + "（" + assetCode + "）");
    }

    private String assetType(String assetCode) {
        if (assetCode.length() >= 2 && ETF_PREFIXES.contains(assetCode.substring(0, 2))) {
            return "ETF";
        }
        return StockUtils.isBond(assetCode) ? "BOND" : "STOCK";
    }

    /**
     * 成交时间解析。券商客户端网格常见返回：成交日期（20260917 或 2026-09-17）与
     * 成交时间（102502 或 10:25:02）分离，也可能已是完整时间串。
     */
    private LocalDateTime parseTradeTime(String date, String time) {
        LocalDateTime parsed = tryParseDateTime(time);
        if (parsed == null) {
            parsed = tryParseDateTime(joinDateAndTime(date, time));
        }
        if (parsed != null) {
            return parsed;
        }
        LocalDate parsedDate = tryParseDate(firstNonBlank(time, date));
        if (parsedDate != null) {
            return parsedDate.atStartOfDay();
        }
        throw new IllegalArgumentException("成交时间格式无法识别："
                + (date + " " + time).trim());
    }

    private String joinDateAndTime(String date, String time) {
        String dateText = normalizeDigits(StringUtils.trimToEmpty(date));
        String timeText = normalizeDigits(StringUtils.trimToEmpty(time));
        if (dateText.isEmpty() || timeText.isEmpty()) {
            return "";
        }
        return dateText + " " + timeText;
    }

    private String normalizeDigits(String text) {
        return text.replace("，", ",").replace(",", "").replace("\"", "").trim();
    }

    private LocalDateTime tryParseDateTime(String text) {
        String value = normalizeDigits(StringUtils.trimToEmpty(text));
        if (value.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss"),
                DateTimeFormatter.ofPattern("yyyyMMdd HHmmss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyyMMdd HH:mm"))) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种常见格式。
            }
        }
        return null;
    }

    private LocalDate tryParseDate(String text) {
        String value = normalizeDigits(StringUtils.trimToEmpty(text));
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
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
