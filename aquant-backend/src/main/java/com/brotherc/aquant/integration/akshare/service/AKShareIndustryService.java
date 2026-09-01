package com.brotherc.aquant.integration.akshare.service;

import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryIndexThs;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryConsThs;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustrySummaryThs;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryNameEm;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryHistEm;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryConsEm;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AKShareIndustryService extends AbstractAKShareService {

    private static final String THS_DETAIL_URL = "https://q.10jqka.com.cn/thshy/detail/code/";
    private static final String THS_CATALOG_SECTOR_CODE = "881272";
    private static final String THS_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36";
    private static final int THS_TIMEOUT_MILLIS = 15_000;
    private static final int THS_MAX_PAGE_COUNT = 50;
    private static final Pattern THS_SECTOR_CODE_PATTERN = Pattern.compile("/code/(\\d+)(?:/|$)");

    private volatile Map<String, String> thsSectorCodeMap;

    public AKShareIndustryService(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        super(objectMapper, okHttpClient);
    }

    /**
     * <a href="https://akshare.akfamily.xyz/data/stock/stock.html#id377">同花顺-同花顺行业一览表</a>
     *
     * @return 当前时刻同花顺行业一览表
     */
    public List<StockBoardIndustrySummaryThs> stockBoardIndustrySummaryThs() {
        return executeGet(akshareAddress + "/api/public/stock_board_industry_summary_ths", new TypeReference<>() {});
    }

    /**
     * <a href="https://akshare.akfamily.xyz/data/stock/stock.html#id378">同花顺-指数</a>
     *
     * @param symbol 行业名称
     * @param startDate 开始时间，20200101
     * @param endDate 结束时间 20211027
     *
     * @return 板块日频指数数据
     */
    public List<StockBoardIndustryIndexThs> stockBoardIndustryIndexThs(String symbol, String startDate, String endDate) {
        HttpUrl.Builder builder = HttpUrl.get(akshareAddress + "/api/public/stock_board_industry_index_ths")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol);

        if (StringUtils.isNotBlank(startDate)) {
            builder.addQueryParameter("start_date", startDate);
        }
        if (StringUtils.isNotBlank(endDate)) {
            builder.addQueryParameter("end_date", endDate);
        }

        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<StockBoardIndustryNameEm> stockBoardIndustryNameEm() {
        return executeGet(akshareAddress + "/api/public/stock_board_industry_name_em", new TypeReference<>() {});
    }

    public List<StockBoardIndustryHistEm> stockBoardIndustryHistEm(String symbol, String startDate, String endDate) {
        HttpUrl url = HttpUrl.get(akshareAddress + "/api/public/stock_board_industry_hist_em")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol)
                .addQueryParameter("start_date", startDate)
                .addQueryParameter("end_date", endDate)
                .addQueryParameter("period", "日k")
                .addQueryParameter("adjust", "")
                .build();
        return executeGet(url, new TypeReference<>() {});
    }

    public List<StockBoardIndustryConsEm> stockBoardIndustryConstituentsEm(String symbol) {
        HttpUrl url = HttpUrl.get(akshareAddress + "/api/public/stock_board_industry_cons_em")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol)
                .build();
        return executeGet(url, new TypeReference<>() {});
    }

    public List<StockBoardIndustryConsThs> stockBoardIndustryConstituentsThs(String sectorName) {
        String sectorCode = getThsSectorCodeMap().get(sectorName);
        if (sectorCode == null) {
            throw ExceptionEnum.STOCK_INDUSTRY_BOARD_UN_EXIST.toException();
        }

        Document firstPage = getThsDocument(buildThsPageUrl(sectorCode, 1));
        int pageCount = getPageCount(firstPage);
        List<StockBoardIndustryConsThs> constituents = new ArrayList<>();
        constituents.addAll(parseThsConstituents(firstPage));
        for (int page = 2; page <= pageCount; page++) {
            constituents.addAll(parseThsConstituents(getThsDocument(buildThsPageUrl(sectorCode, page))));
        }
        if (constituents.isEmpty()) {
            throw ExceptionEnum.API_REQUEST_ERROR.toException();
        }
        return constituents;
    }

    private Map<String, String> getThsSectorCodeMap() {
        Map<String, String> cached = thsSectorCodeMap;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (thsSectorCodeMap == null) {
                Map<String, String> nextMap = new HashMap<>();
                Document document = getThsDocument(buildThsPageUrl(THS_CATALOG_SECTOR_CODE, 1));
                for (Element link : document.select("div.cate_inner a[href]")) {
                    String code = parseThsSectorCode(link.attr("href"));
                    if (StringUtils.isNotBlank(code) && StringUtils.isNotBlank(link.text())) {
                        nextMap.put(link.text().trim(), code);
                    }
                }
                if (nextMap.isEmpty()) {
                    throw ExceptionEnum.API_REQUEST_ERROR.toException();
                }
                thsSectorCodeMap = Map.copyOf(nextMap);
            }
            return thsSectorCodeMap;
        }
    }

    static String parseThsSectorCode(String href) {
        Matcher matcher = THS_SECTOR_CODE_PATTERN.matcher(href);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Document getThsDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(THS_USER_AGENT)
                    .timeout(THS_TIMEOUT_MILLIS)
                    .get();
        } catch (Exception exception) {
            throw ExceptionEnum.API_REQUEST_ERROR.toException(exception);
        }
    }

    private String buildThsPageUrl(String sectorCode, int page) {
        String baseUrl = THS_DETAIL_URL + sectorCode + "/";
        return page == 1 ? baseUrl : baseUrl + "page/" + page + "/";
    }

    private int getPageCount(Document document) {
        String pageInfo = document.selectFirst("span.page_info") == null
                ? "1/1"
                : document.selectFirst("span.page_info").text();
        String[] values = pageInfo.split("/");
        if (values.length != 2) {
            return 1;
        }
        try {
            return Math.min(Integer.parseInt(values[1]), THS_MAX_PAGE_COUNT);
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    static List<StockBoardIndustryConsThs> parseThsConstituents(Document document) {
        List<StockBoardIndustryConsThs> result = new ArrayList<>();
        for (Element row : document.select("#maincont tbody tr")) {
            List<Element> cells = row.select("td");
            if (cells.size() < 14) {
                continue;
            }
            StockBoardIndustryConsThs constituent = new StockBoardIndustryConsThs();
            constituent.setStockCode(cells.get(1).text());
            constituent.setStockName(cells.get(2).text());
            constituent.setLatestPrice(parseDecimal(cells.get(3).text()));
            constituent.setChangePercent(parseDecimal(cells.get(4).text()));
            constituent.setChangeAmount(parseDecimal(cells.get(5).text()));
            constituent.setAmplitude(parseDecimal(cells.get(9).text()));
            constituent.setTurnover(parseChineseAmount(cells.get(10).text()));
            constituent.setPeTtm(parseDecimal(cells.get(13).text()));
            if (StringUtils.isNotBlank(constituent.getStockCode()) && StringUtils.isNotBlank(constituent.getStockName())) {
                result.add(constituent);
            }
        }
        return result;
    }

    private static BigDecimal parseDecimal(String value) {
        if (StringUtils.isBlank(value) || "--".equals(value.trim())) {
            return null;
        }
        try {
            return new BigDecimal(value.trim().replace(",", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static BigDecimal parseChineseAmount(String value) {
        if (StringUtils.isBlank(value) || "--".equals(value.trim())) {
            return null;
        }
        String normalized = value.trim().replace(",", "");
        try {
            if (normalized.endsWith("亿")) {
                return new BigDecimal(normalized.substring(0, normalized.length() - 1)).multiply(BigDecimal.valueOf(100_000_000));
            }
            if (normalized.endsWith("万")) {
                return new BigDecimal(normalized.substring(0, normalized.length() - 1)).multiply(BigDecimal.valueOf(10_000));
            }
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

}
