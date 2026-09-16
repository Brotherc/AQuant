package com.brotherc.aquant.integration.eastmoney.service;

import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardDetail;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardKline;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardList;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardTrends;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 东方财富行业板块行情服务（直连东财行情主机，绕开 AKTools 与系统代理）。
 * 实时类接口（板块列表/成分股）经 EastmoneyQuoteGateway 走 push2delay（push2 兜底），
 * K线/分时走 push2his（push2delay 不提供 K 线数据，且 push2his 未被封）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EastmoneyBoardService {

    private static final String BOARD_MARKET_PREFIX = "90.";
    private static final String KLINE_URL = "https://push2his.eastmoney.com/api/qt/stock/kline/get";
    private static final String TRENDS_URL = "https://push2his.eastmoney.com/api/qt/stock/trends2/get";
    /**
     * stock/get 详情快照字段（与浏览器详情页一致）：f57/f58 代码名称，f43 最新，f44-f46 最高/最低/今开，
     * f47 成交量(手)，f48 成交额(元)，f49 外盘(手)，f50 量比，f60 昨收，f85 流通股本(股)，
     * f117 流通市值(元)，f168 换手率，f169-f171 涨跌额/涨跌幅/振幅。
     * 上游无独立内盘字段（内盘=成交量-外盘），f65/f66 对板块恒为空。
     */
    private static final String DETAIL_FIELDS =
            "f57,f58,f43,f44,f45,f46,f47,f48,f49,f50,f60,f85,f117,f168,f169,f170,f171";
    /**
     * 单页条数。浏览器实际值为 20。pz=100 全量翻页请求更集中、单次响应更大，
     * 实测更容易触发东财按主机的频次软封（502 / 空响应体），故对齐浏览器行为。
     */
    private static final int PAGE_SIZE = 20;
    /** 板块列表分页上限：全量约 496 个板块，20/页需 25 页，留足余量 */
    private static final int MAX_BOARD_PAGE = 40;
    /**
     * clist/get 公共令牌（与 akshare/东财官网一致）。缺失时接口不报错，但 fs 过滤被静默忽略，
     * 会返回全市场 6 万余条证券而非板块列表。
     */
    private static final String UT_TOKEN = "b2884a393a59ad64002292a3e90d46a5";
    /** 单个板块成分股数量上限，超出说明 fs 过滤失效返回了全市场数据 */
    private static final int MAX_CONSTITUENT_COUNT = 2000;
    /**
     * 成分股分页上限，由数量上限换算而来。二者必须对齐，
     * 否则会在未触及 MAX_CONSTITUENT_COUNT 前就静默截断成分股（改 PAGE_SIZE 时尤其容易踩）。
     */
    private static final int MAX_CONSTITUENT_PAGE = MAX_CONSTITUENT_COUNT / PAGE_SIZE;

    private final EastmoneyQuoteGateway quoteGateway;

    /**
     * 行业板块列表（全量分页拉取，约 496 个板块）
     */
    public EastmoneyBoardList fetchBoardList() {
        EastmoneyBoardList result = new EastmoneyBoardList();
        for (int page = 1; page <= MAX_BOARD_PAGE; page++) {
            final int currentPage = page;
            JsonNode data = quoteGateway.executeQuote(builder -> builder
                    .addPathSegments("api/qt/clist/get")
                    .addQueryParameter("pn", String.valueOf(currentPage))
                    .addQueryParameter("pz", String.valueOf(PAGE_SIZE))
                    .addQueryParameter("po", "1")
                    .addQueryParameter("np", "1")
                    .addQueryParameter("ut", UT_TOKEN)
                    .addQueryParameter("fltt", "2")
                    .addQueryParameter("invt", "2")
                    .addQueryParameter("fid", "f3")
                    .addQueryParameter("fs", "m:90+t:2+f:!50")
                    .addQueryParameter("fields", "f12,f14,f2,f3,f5,f6,f62,f104,f105,f140,f136"))
                    .path("data");
            int total = data.path("total").asInt(0);
            JsonNode diff = data.path("diff");
            if (!diff.isArray() || diff.isEmpty()) {
                break;
            }
            int pageBoardCount = 0;
            for (JsonNode item : diff) {
                EastmoneyBoardList.Board board = new EastmoneyBoardList.Board();
                board.setSectorCode(item.path("f12").asText());
                board.setSectorName(item.path("f14").asText());
                board.setLatestPrice(parseDecimal(item.path("f2")));
                board.setChangePercent(parseDecimal(item.path("f3")));
                board.setTotalVolume(parseDecimal(item.path("f5")));
                board.setTotalAmount(parseDecimal(item.path("f6")));
                board.setNetInflow(parseDecimal(item.path("f62")));
                board.setRiseCount(item.path("f104").isNumber() ? item.path("f104").asInt() : null);
                board.setFallCount(item.path("f105").isNumber() ? item.path("f105").asInt() : null);
                board.setLeadingStock(item.path("f140").asText(null));
                board.setLeadingStockChangePercent(parseDecimal(item.path("f136")));
                if (board.getSectorCode().startsWith("BK") && !board.getSectorName().isBlank()) {
                    result.getBoards().add(board);
                    pageBoardCount++;
                }
            }
            if (pageBoardCount == 0) {
                log.warn("东财板块列表响应不含板块代码，fs 过滤可能被上游忽略，total={}", total);
                throw ExceptionEnum.API_REQUEST_ERROR.toException();
            }
            if (page * PAGE_SIZE >= total) {
                break;
            }
        }
        if (result.getBoards().isEmpty()) {
            throw ExceptionEnum.API_REQUEST_ERROR.toException();
        }
        return result;
    }

    /**
     * 板块成分股列表
     *
     * @param sectorCode 板块代码，如 BK1300
     */
    public List<EastmoneyBoardList.Board> fetchBoardConstituents(String sectorCode) {
        List<EastmoneyBoardList.Board> result = new ArrayList<>();
        for (int page = 1; page <= MAX_CONSTITUENT_PAGE; page++) {
            final int currentPage = page;
            JsonNode data = quoteGateway.executeQuote(builder -> builder
                    .addPathSegments("api/qt/clist/get")
                    .addQueryParameter("pn", String.valueOf(currentPage))
                    .addQueryParameter("pz", String.valueOf(PAGE_SIZE))
                    .addQueryParameter("po", "1")
                    .addQueryParameter("np", "1")
                    .addQueryParameter("ut", UT_TOKEN)
                    .addQueryParameter("fltt", "2")
                    .addQueryParameter("invt", "2")
                    .addQueryParameter("fid", "f12")
                    .addQueryParameter("fs", "b:" + sectorCode + "+f:!50")
                    .addQueryParameter("fields", "f12,f14"))
                    .path("data");
            int total = data.path("total").asInt(0);
            if (total > MAX_CONSTITUENT_COUNT) {
                log.warn("东财板块成分股数量异常，fs 过滤可能被上游忽略，sectorCode={}, total={}", sectorCode, total);
                throw ExceptionEnum.API_REQUEST_ERROR.toException();
            }
            JsonNode diff = data.path("diff");
            if (!diff.isArray() || diff.isEmpty()) {
                break;
            }
            for (JsonNode item : diff) {
                EastmoneyBoardList.Board stock = new EastmoneyBoardList.Board();
                stock.setSectorCode(item.path("f12").asText());
                stock.setSectorName(item.path("f14").asText());
                if (!stock.getSectorCode().isBlank() && !stock.getSectorName().isBlank()) {
                    result.add(stock);
                }
            }
            if (page * PAGE_SIZE >= total) {
                break;
            }
        }
        return result;
    }

    /**
     * 板块详情快照（详情页顶部盘口指标：今开/昨收/最高/最低/换手/量比/外盘/流通市值/流通股本等）。
     * 走实时行情主机（push2delay 优先），与板块列表一致。
     *
     * @param sectorCode 板块代码，如 BK1201
     */
    public EastmoneyBoardDetail fetchBoardDetail(String sectorCode) {
        JsonNode data = quoteGateway.executeQuote(builder -> builder
                .addPathSegments("api/qt/stock/get")
                .addQueryParameter("secid", BOARD_MARKET_PREFIX + sectorCode)
                .addQueryParameter("fltt", "2")
                .addQueryParameter("invt", "2")
                .addQueryParameter("fields", DETAIL_FIELDS))
                .path("data");
        if (data.isMissingNode() || data.isNull() || data.path("f57").asText("").isBlank()) {
            throw ExceptionEnum.API_REQUEST_ERROR.toException();
        }
        EastmoneyBoardDetail detail = new EastmoneyBoardDetail();
        detail.setSectorCode(data.path("f57").asText());
        detail.setSectorName(data.path("f58").asText(null));
        detail.setLatestPrice(parseDecimal(data.path("f43")));
        detail.setOpenPrice(parseDecimal(data.path("f46")));
        detail.setPreClosePrice(parseDecimal(data.path("f60")));
        detail.setHighPrice(parseDecimal(data.path("f44")));
        detail.setLowPrice(parseDecimal(data.path("f45")));
        detail.setChangeAmount(parseDecimal(data.path("f169")));
        detail.setChangePercent(parseDecimal(data.path("f170")));
        detail.setAmplitude(parseDecimal(data.path("f171")));
        detail.setVolume(parseDecimal(data.path("f47")));
        detail.setAmount(parseDecimal(data.path("f48")));
        detail.setTurnoverRate(parseDecimal(data.path("f168")));
        detail.setVolumeRatio(parseDecimal(data.path("f50")));
        detail.setOuterDisc(parseDecimal(data.path("f49")));
        detail.setCirculatingMarketValue(parseDecimal(data.path("f117")));
        detail.setCirculatingShares(parseDecimal(data.path("f85")));
        return detail;
    }

    /**
     * 板块日K线
     *
     * @param sectorCode 板块代码，如 BK1300
     * @param startDate  开始日期 yyyyMMdd
     * @param endDate    结束日期 yyyyMMdd
     */
    public EastmoneyBoardKline fetchBoardDailyKline(String sectorCode, String startDate, String endDate) {
        HttpUrl url = HttpUrl.get(KLINE_URL).newBuilder()
                .addQueryParameter("secid", BOARD_MARKET_PREFIX + sectorCode)
                .addQueryParameter("klt", "101")
                .addQueryParameter("fqt", "1")
                .addQueryParameter("beg", startDate)
                .addQueryParameter("end", endDate)
                .addQueryParameter("fields1", "f1,f2,f3,f4,f5,f6")
                .addQueryParameter("fields2", "f51,f52,f53,f54,f55,f56,f57,f58")
                .build();
        return parseKline(url, 101);
    }

    /**
     * 板块1分钟K线
     *
     * @param sectorCode 板块代码，如 BK1300
     * @param days       拉取天数（东财按交易日返回最近 days 个交易日的分钟线）
     */
    public EastmoneyBoardKline fetchBoardMinuteKline(String sectorCode, int days) {
        HttpUrl url = HttpUrl.get(KLINE_URL).newBuilder()
                .addQueryParameter("secid", BOARD_MARKET_PREFIX + sectorCode)
                .addQueryParameter("klt", "1")
                .addQueryParameter("fqt", "1")
                .addQueryParameter("beg", "0")
                .addQueryParameter("end", "20500101")
                .addQueryParameter("lmt", String.valueOf(days * 240))
                .addQueryParameter("fields1", "f1,f2,f3,f4,f5,f6")
                .addQueryParameter("fields2", "f51,f52,f53,f54,f55,f56,f57,f58")
                .build();
        return parseKline(url, 1);
    }

    /**
     * 板块分时走势
     *
     * @param sectorCode 板块代码，如 BK1300
     * @param days       1=当日分时，5=五日分时
     */
    public EastmoneyBoardTrends fetchBoardTrends(String sectorCode, int days) {
        HttpUrl url = HttpUrl.get(TRENDS_URL).newBuilder()
                .addQueryParameter("secid", BOARD_MARKET_PREFIX + sectorCode)
                .addQueryParameter("fields1", "f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13")
                .addQueryParameter("fields2", "f51,f52,f53,f54,f55,f56,f57,f58")
                .addQueryParameter("iscr", "0")
                .addQueryParameter("ndays", String.valueOf(days))
                .build();
        JsonNode data = quoteGateway.execute(url).path("data");
        if (data.isMissingNode() || data.isNull()) {
            throw ExceptionEnum.API_REQUEST_ERROR.toException();
        }
        EastmoneyBoardTrends result = new EastmoneyBoardTrends();
        result.setCode(data.path("code").asText());
        result.setName(data.path("name").asText());
        result.setPreClose(parseDecimal(data.path("preClose")));
        for (JsonNode node : data.path("trends")) {
            // 格式: "yyyy-MM-dd HH:mm,开,收/现,高,低,量(手),额(元),均价"
            String[] parts = node.asText().trim().split(",");
            if (parts.length < 8) {
                continue;
            }
            EastmoneyBoardTrends.Trend trend = new EastmoneyBoardTrends.Trend();
            trend.setTime(parts[0]);
            trend.setPrice(parseDecimalNode(parts[2]));
            trend.setAvgPrice(parseDecimalNode(parts[7]));
            trend.setVolume(parseDecimalNode(parts[5]));
            trend.setAmount(parseDecimalNode(parts[6]));
            result.getTrends().add(trend);
        }
        if (result.getTrends().isEmpty()) {
            throw ExceptionEnum.API_REQUEST_ERROR.toException();
        }
        return result;
    }

    private EastmoneyBoardKline parseKline(HttpUrl url, int klt) {
        JsonNode data = quoteGateway.execute(url).path("data");
        if (data.isMissingNode() || data.isNull()) {
            throw ExceptionEnum.API_REQUEST_ERROR.toException();
        }
        EastmoneyBoardKline result = new EastmoneyBoardKline();
        result.setCode(data.path("code").asText());
        result.setKlt(klt);
        result.setPreClose(parseDecimal(data.path("prePrice")));
        for (JsonNode node : data.path("klines")) {
            // 格式: "时间,开,收,高,低,量(手),额(元),振幅"
            String[] parts = node.asText().trim().split(",");
            if (parts.length < 7) {
                continue;
            }
            EastmoneyBoardKline.Bar bar = new EastmoneyBoardKline.Bar();
            bar.setTime(parts[0]);
            bar.setOpenPrice(parseDecimalNode(parts[1]));
            bar.setClosePrice(parseDecimalNode(parts[2]));
            bar.setHighPrice(parseDecimalNode(parts[3]));
            bar.setLowPrice(parseDecimalNode(parts[4]));
            bar.setVolume(parseDecimalNode(parts[5]));
            bar.setAmount(parseDecimalNode(parts[6]));
            bar.setAmplitude(parts.length >= 8 ? parseDecimalNode(parts[7]) : null);
            result.getBars().add(bar);
        }
        return result;
    }

    private static BigDecimal parseDecimalNode(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return parseDecimalNode(node.asText());
    }
}
