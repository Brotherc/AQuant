package com.brotherc.aquant.portfolio.service.sync;

import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.integration.eastmoney.service.EastmoneyJywgClient;
import com.brotherc.aquant.portfolio.model.vo.BrokerPositionSnapshotVO;
import com.brotherc.aquant.portfolio.model.vo.BrokerPositionSnapshotVO.BrokerPositionItemVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 东方财富证券网页交易渠道（UserBrokerAccount.syncMode = EM_WEB）。
 *
 * <p>经 EastmoneyJywgClient（浏览器会话 Cookie + validatekey）读取当日与最近 7 天成交、
 * 资金持仓。成交接口无费用明细（费用在次日交割单/资金流水中），现金净发生额按
 * 方向推算；同一账户建议以网页同步为主，避免再导交割单造成重复计账。</p>
 */
@Slf4j
@Component
public class EastmoneyWebTradeSyncProvider implements PortfolioBrokerSyncProvider {

    public static final String CHANNEL = "EM_WEB";
    private static final Set<String> ETF_PREFIXES = Set.of("15", "16", "50", "51", "52", "53", "56", "58");

    private final EastmoneyJywgClient jywgClient;

    public EastmoneyWebTradeSyncProvider(EastmoneyJywgClient jywgClient) {
        this.jywgClient = jywgClient;
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public List<PortfolioTradeSaveReqVO> fetchTrades() {
        // 当日成交 + 最近 7 天历史成交合并（历史接口含当日数据，两接口重叠行按
        // 成交编号+代码+时间+数量去重），避免跨天未同步时漏单；落库另有 dedupKey 幂等兜底
        LocalDate today = LocalDate.now();
        JsonNode todayMatches = jywgClient.todayMatches();
        JsonNode historyMatches = jywgClient.historyMatches(
                today.minusDays(6).format(DateTimeFormatter.BASIC_ISO_DATE),
                today.format(DateTimeFormatter.BASIC_ISO_DATE));
        Map<String, JsonNode> merged = new LinkedHashMap<>();
        for (JsonNode row : todayMatches) {
            merged.putIfAbsent(rowKey(row), row);
        }
        for (JsonNode row : historyMatches) {
            merged.putIfAbsent(rowKey(row), row);
        }
        List<PortfolioTradeSaveReqVO> trades = new ArrayList<>();
        for (JsonNode row : merged.values()) {
            trades.add(toTrade(row));
        }
        return trades;
    }

    /** 跨接口去重键：成交编号优先，缺失时退化为 代码|日期|时间|数量|金额 */
    private String rowKey(JsonNode row) {
        String dealId = firstNonBlank(text(row, "matchcode", "Cjbh", "成交编号"),
                text(row, "orderid", "Wtbh", "委托编号"));
        if (StringUtils.isNotBlank(dealId)) {
            return "id:" + dealId;
        }
        return String.join("|",
                StringUtils.defaultString(text(row, "stkcode", "Zqdm")),
                StringUtils.defaultString(text(row, "bizdate", "Wtrq")),
                StringUtils.defaultString(text(row, "matchtime", "Cjsj")),
                StringUtils.defaultString(text(row, "matchqty", "Cjsl")),
                StringUtils.defaultString(text(row, "matchamt")));
    }

    /** 依次取第一个非空文本字段 */
    private String text(JsonNode row, String... fields) {
        for (String field : fields) {
            String value = normalizeDigits(row.path(field).asText(""));
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    /** 依次取第一个可解析的非空数值字段（缺失或不可解析返回 null，不抛错） */
    private BigDecimal firstDecimal(JsonNode row, String... fields) {
        for (String field : fields) {
            String value = normalizeDigits(row.path(field).asText(""));
            if (StringUtils.isBlank(value)) {
                continue;
            }
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException ignored) {
                // 尝试下一个候选字段。
            }
        }
        return null;
    }

    /**
     * 券商实时持仓快照（多证券）。jywg queryAssetAndPositionV1 实测为 snake_case：
     * 资产层 total_asset/security_market_value/enable_balance/fetch_balance/frozen_balance/
     * money_balance/position_income/day_income；持仓层 security_code/security_name/
     * holding_quantity/enable_quantity/cost_price/last_price/market_value/income/
     * income_rate/day_income/day_income_rate/keep_cost_price。
     * 所有指标直取券商原值（成本价摊薄可为负、比例带 %），本地不做任何推算；
     * 同时兼容旧的大写拼音缩写体系字段名。
     */
    @Override
    public BrokerPositionSnapshotVO fetchPositions() {
        JsonNode data = jywgClient.assetAndPosition();
        JsonNode first = data.isArray() && !data.isEmpty() ? data.get(0) : data;
        BrokerPositionSnapshotVO snapshot = new BrokerPositionSnapshotVO();
        snapshot.setChannel(CHANNEL);
        snapshot.setFetchTime(java.time.LocalDateTime.now().toString());
        if (first == null || first.isMissingNode()) {
            return snapshot;
        }
        // 字段映射诊断：东财 jywg 各接口字段命名不一致（成交行实测为 stkcode/matchprice 小写体系），
        // 持仓字段若与下方映射不匹配会被记为缺失；该日志输出真实字段名与值，便于精确对齐映射
        log.info("东财持仓接口原始响应（资产层）: {}", first);
        // 候选字段覆盖三种命名风格：snake_case(官方文档)、无下划线小写(东财实测 stkcode 体系)、拼音缩写(旧逆向)
        snapshot.setTotalAsset(decimal(first, "Zzc", "zzc", "total_asset", "totalasset"));
        snapshot.setSecurityMarketValue(decimal(first, "Zxsz", "zxsz", "totalSecMkval", "totalsecmkval",
                "security_market_value", "securitymarketvalue"));
        snapshot.setEnableBalance(decimal(first, "Kyzj", "kyzj", "enable_balance", "enablebalance", "fundbal"));
        snapshot.setFetchBalance(decimal(first, "Kqzj", "kqzj", "fetch_balance", "fetchbalance"));
        snapshot.setFrozenBalance(decimal(first, "Djzj", "djzj", "frozen_balance", "frozenbalance"));
        snapshot.setMoneyBalance(decimal(first, "Zjye", "zjye", "money_balance", "moneybalance", "fund_balance"));
        snapshot.setPositionIncome(decimal(first, "Ljyk", "ljyk", "position_income", "positionincome"));
        snapshot.setDayIncome(decimal(first, "Dryk", "dryk", "day_income", "dayincome"));

        JsonNode rows = first.path("positions");
        for (String candidate : List.of("positions", "positionlist", "stocklist", "holdings", "list", "data")) {
            if (rows.isArray() && !rows.isEmpty()) {
                break;
            }
            JsonNode value = first.path(candidate);
            if (value.isArray() && !value.isEmpty()) {
                rows = value;
            }
        }
        if (!rows.isArray() || rows.isEmpty()) {
            for (JsonNode field : first) {
                if (field.isArray() && !field.isEmpty() && field.get(0).isObject()) {
                    rows = field;
                    break;
                }
            }
        }
        // 持仓行字段同样输出一例（首个持仓），用于校准持仓层字段映射
        if (rows.isArray() && !rows.isEmpty()) {
            log.info("东财持仓接口原始响应（持仓示例行）: {}", rows.get(0));
        }
        for (JsonNode position : rows) {
            BrokerPositionItemVO item = new BrokerPositionItemVO();
            item.setSecurityCode(text(position, "Zqdm", "zqdm", "stkcode", "security_code"));
            item.setSecurityName(text(position, "Zqmc", "zqmc", "stkname", "security_name"));
            item.setHoldingQuantity(decimal(position, "Zqsl", "zqsl", "stkqty", "holding_quantity"));
            item.setEnableQuantity(decimal(position, "Kysl", "kysl", "enableqty", "enable_quantity"));
            item.setCostPrice(decimal(position, "Cbjg", "cbjg", "costprice", "cost_price"));
            item.setLastPrice(decimal(position, "Zxjg", "zxjg", "lastprice", "last_price"));
            item.setMarketValue(decimal(position, "Zxsz", "zxsz", "zxsznew", "marketvalue", "market_value"));
            item.setIncome(decimal(position, "Ljyk", "ljyk", "Ckyk", "income"));
            // Ykbl 为小数比例（-0.056604 = -5.660%），需放大 100 倍与官网百分比口径一致
            BigDecimal ykbl = scaledPercent(position, "Ykbl", "ykbl");
            item.setIncomeRate(ykbl != null ? ykbl : percent(position, "income_rate"));
            BigDecimal drykbl = scaledPercent(position, "Drykbl", "drykbl");
            item.setDayIncomeRate(drykbl != null ? drykbl : percent(position, "day_income_rate"));
            item.setDayIncome(decimal(position, "Dryk", "dryk", "day_income"));
            item.setKeepCostPrice(decimal(position, "Cbjgex", "cbjgex", "keep_cost_price", "keepcostprice"));
            snapshot.getPositions().add(item);
        }
        return snapshot;
    }

    /** 数值解析：去除千分位/百分号外字符；失败返回 null（快照字段可缺省） */
    private BigDecimal decimal(JsonNode row, String... fields) {
        return firstDecimal(row, fields);
    }

    /** 小数比例解析（如 -0.056604），放大 100 倍转为百分比口径（-5.6604），失败返回 null */
    private BigDecimal scaledPercent(JsonNode row, String... fields) {
        for (String field : fields) {
            String value = normalizeDigits(row.path(field).asText("")).replace("%", "");
            if (StringUtils.isBlank(value)) {
                continue;
            }
            try {
                return new BigDecimal(value).multiply(BigDecimal.valueOf(100));
            } catch (NumberFormatException ignored) {
                // 尝试下一个候选字段。
            }
        }
        return null;
    }

    /** 比例解析：接口带 % 后缀（如 "-5.660%"），去 % 后返回数值；支持多候选字段 */
    private BigDecimal percent(JsonNode row, String... fields) {
        for (String field : fields) {
            String value = normalizeDigits(row.path(field).asText("")).replace("%", "");
            if (StringUtils.isBlank(value)) {
                continue;
            }
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException ignored) {
                // 尝试下一个候选字段。
            }
        }
        return null;
    }

    /**
     * 成交行映射。jywg 实测字段为小写体系（ stkcode/matchprice/matchqty/matchamt/fundeffect/
     * bsflag_ex/matchtime/bizdate/fee_* ），同时兼容旧逆向文档的 Zqdm/Cjsj 大写体系。
     * 交割/成交行自带费用明细与签名资金净额（fundeffect），直接采用，不再推算。
     */
    private PortfolioTradeSaveReqVO toTrade(JsonNode row) {
        String assetCode = firstNonBlank(text(row, "stkcode", "Zqdm", "证券代码"));
        if (StringUtils.isBlank(assetCode)) {
            throw new IllegalArgumentException("成交行缺少证券代码：" + row);
        }
        String operation = firstNonBlank(text(row, "bsflag_ex", "Mmsm", "操作", "买卖标志"));
        String bsflag = text(row, "bsflag");
        BigDecimal price = firstDecimal(row, "matchprice", "Cjjg", "成交价格");
        BigDecimal quantity = firstDecimal(row, "matchqty", "Cjsl", "成交数量");
        BigDecimal amount = firstDecimal(row, "matchamt", "成交金额");
        BigDecimal gross = amount != null ? amount.abs()
                : price == null || quantity == null ? null : price.multiply(quantity).abs();

        PortfolioTradeSaveReqVO trade = new PortfolioTradeSaveReqVO();
        trade.setTradeType(tradeType(operation, bsflag, assetCode));
        trade.setAssetType(assetType(assetCode));
        trade.setMarket(StockUtils.market(assetCode));
        trade.setAssetCode(assetCode);
        trade.setAssetName(firstNonBlank(text(row, "stkname", "Zqmc", "证券名称")));
        trade.setTradeTime(parseTradeTime(row));
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setGrossAmount(gross);
        // 费用明细实测（买入样例）：fee_sxf≈万分之2.5 佣金、fee_yhs 印花税（买入为 0）、
        // fee_ghf≈十万分之1 过户费、fee_jsxf 经手费等其他费用
        trade.setCommission(firstDecimal(row, "fee_sxf", "佣金"));
        trade.setStampDuty(firstDecimal(row, "fee_yhs", "印花税"));
        trade.setTransferFee(firstDecimal(row, "fee_ghf", "过户费"));
        trade.setOtherFee(firstDecimal(row, "fee_jsxf", "其他费用", "经手费"));
        // fundeffect 为签名资金净发生额（含费，买入为负），上游权威值直接采用
        BigDecimal netAmount = firstDecimal(row, "fundeffect", "发生金额");
        trade.setNetAmount(netAmount != null ? netAmount
                : "BUY".equals(trade.getTradeType()) ? (gross == null ? null : gross.negate()) : gross);
        trade.setCurrency("CNY");
        trade.setSourceTradeId(firstNonBlank(text(row, "matchcode", "Cjbh", "成交编号"),
                text(row, "orderid", "Wtbh", "委托编号")));
        trade.setRemark("东方财富网页交易：" + StringUtils.defaultIfBlank(operation, "成交"));
        return trade;
    }

    private String tradeType(String operation, String bsflag, String assetCode) {
        String text = StringUtils.defaultString(operation);
        if (text.contains("买")) {
            return "BUY";
        }
        if (text.contains("卖")) {
            return "SELL";
        }
        // bsflag 代码形如 0B/0S
        String flag = StringUtils.defaultString(bsflag);
        if (flag.endsWith("B")) {
            return "BUY";
        }
        if (flag.endsWith("S")) {
            return "SELL";
        }
        throw new IllegalArgumentException("暂不支持东财网页成交方向：" + operation + "（" + assetCode + "）");
    }

    private String assetType(String assetCode) {
        if (assetCode.length() >= 2 && ETF_PREFIXES.contains(assetCode.substring(0, 2))) {
            return "ETF";
        }
        return StockUtils.isBond(assetCode) ? "BOND" : "STOCK";
    }

    /** 成交时间优先字段：matchtime(成交时间 HHmmss) / bizdate(业务日期 yyyyMMdd)，兼容旧体系 */
    private static final List<String> PREFERRED_TIME_FIELDS =
            List.of("matchtime", "Cjsj", "成交时间", "Wtsj", "委托时间", "Sj");
    /** 日期优先字段：bizdate(业务日期) / cleardate(清算日期) / sortdate(交收日期)，兼容旧体系 */
    private static final List<String> PREFERRED_DATE_FIELDS =
            List.of("bizdate", "cleardate", "sortdate", "Wtrq", "成交日期", "委托日期");

    /**
     * 成交时间解析：优先 matchtime+bizdate 组合（实测 134357 + 20260916），
     * 失败后全行扫描兜底，仍失败抛错并打印整行字段。
     */
    private LocalDateTime parseTradeTime(JsonNode row) {
        LocalTime preferredTime = null;
        for (String field : PREFERRED_TIME_FIELDS) {
            LocalTime time = tryParseTime(row.path(field).asText(""));
            if (time != null) {
                preferredTime = time;
                break;
            }
        }
        LocalDate preferredDate = null;
        for (String field : PREFERRED_DATE_FIELDS) {
            LocalDate date = tryParseDate(row.path(field).asText(""));
            if (date != null) {
                preferredDate = date;
                break;
            }
        }
        if (preferredDate != null) {
            return preferredDate.atTime(preferredTime == null ? LocalTime.MIN : preferredTime);
        }
        // 偏好字段缺失时全行扫描兜底
        LocalDate date = null;
        LocalTime fallbackTime = null;
        for (Iterator<Map.Entry<String, JsonNode>> fields = row.fields(); fields.hasNext(); ) {
            Map.Entry<String, JsonNode> field = fields.next();
            String name = field.getKey();
            String value = normalizeDigits(field.getValue().asText(""));
            if (value.isEmpty() || isNonDateTimeField(name)) {
                continue;
            }
            LocalDateTime full = tryParseDateTime(value);
            if (full != null) {
                return full;
            }
            if (date == null) {
                date = tryParseDate(value);
            }
            if (fallbackTime == null) {
                fallbackTime = tryParseTime(value);
            }
        }
        if (date != null) {
            LocalTime time = preferredTime != null ? preferredTime : fallbackTime;
            return date.atTime(time == null ? LocalTime.MIN : time);
        }
        log.warn("东财网页成交行时间字段无法识别，原始行: {}", row);
        throw new IllegalArgumentException("成交时间格式无法识别：" + row);
    }

    /** 全角数字转半角，去千分位与引用符号 */
    private String normalizeDigits(String text) {
        return text.replace("，", ",").replace(",", "").replace("\"", "").trim();
    }

    /** 编号/数量/价格/金额/代码等字段不参与日期时间识别 */
    private boolean isNonDateTimeField(String fieldName) {
        String lower = fieldName.toLowerCase(Locale.ROOT);
        return lower.contains("bh") || lower.contains("sl") || lower.contains("jg")
                || lower.contains("je") || lower.contains("dm") || lower.contains("mc");
    }

    private LocalTime tryParseTime(String text) {
        String value = normalizeDigits(StringUtils.trimToEmpty(text));
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ofPattern("HHmmss"),
                DateTimeFormatter.ofPattern("HH:mm:ss"),
                DateTimeFormatter.ofPattern("HH:mm"))) {
            try {
                return LocalTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种常见格式。
            }
        }
        return null;
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

    private BigDecimal parseDecimal(JsonNode row, String field) {
        JsonNode value = row.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.asText().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("数值字段无法解析：" + field + "=" + value.asText());
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
