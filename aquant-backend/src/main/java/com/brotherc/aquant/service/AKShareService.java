package com.brotherc.aquant.service;

import com.brotherc.aquant.model.dto.akshare.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AKShareService {

    @Value("${akshare-address}")
    private String akshareAddress;

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    /**
     * 从 Request 的 URL 中自动解析并截取 API 名称（取 Path 的最后一个 segment，如 stock_zh_a_spot）
     */
    private String extractApiName(Request request) {
        List<String> segments = request.url().pathSegments();
        if (segments != null && !segments.isEmpty()) {
            return segments.get(segments.size() - 1);
        }
        return request.url().encodedPath();
    }

    /**
     * 通用 HTTP 请求处理：执行 Request 并自动转为目标 Java 对象
     */
    private <T> T executeRequest(Request request, TypeReference<T> typeReference) {
        String apiName = extractApiName(request);
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.info("{} 失败响应: {}", apiName, response);
                throw new RuntimeException(apiName + "请求失败");
            }
            return objectMapper.readValue(response.body().string(), typeReference);
        } catch (IOException e) {
            log.error("{} 请求失败", apiName, e);
            throw new RuntimeException(apiName + "请求失败", e);
        }
    }

    /**
     * 通用 GET 请求重载（接收 String URL）
     */
    private <T> T executeGet(String url, TypeReference<T> typeReference) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return executeRequest(request, typeReference);
    }

    /**
     * 通用 GET 请求重载（接收 HttpUrl 构造器）
     */
    private <T> T executeGet(HttpUrl httpUrl, TypeReference<T> typeReference) {
        Request request = new Request.Builder()
                .url(httpUrl)
                .get()
                .build();
        return executeRequest(request, typeReference);
    }

    public List<StockZhASpot> stockZhASpot() {
        return executeGet(akshareAddress + "/api/public/stock_zh_a_spot", new TypeReference<>() {});
    }

    public List<StockZhIndexSpotSina> stockZhIndexSpotSina() {
        return executeGet(akshareAddress + "/api/public/stock_zh_index_spot_sina", new TypeReference<>() {});
    }

    public List<StockZhIndexDaily> stockZhIndexDaily(String symbol) {
        return executeGet(akshareAddress + "/api/public/stock_zh_index_daily?symbol=" + symbol, new TypeReference<>() {});
    }

    public List<StockZhValuationComparisonEm> stockZhValuationComparisonEm(String symbol) {
        return executeGet(akshareAddress + "/api/public/stock_zh_valuation_comparison_em?symbol=" + symbol.toUpperCase(), new TypeReference<>() {});
    }

    public List<StockZhGrowthComparisonEm> stockZhGrowthComparisonEm(String symbol) {
        return executeGet(akshareAddress + "/api/public/stock_zh_growth_comparison_em?symbol=" + symbol.toUpperCase(), new TypeReference<>() {});
    }

    public List<StockZhDupontComparisonEm> stockZhDupontComparisonEm(String symbol) {
        return executeGet(akshareAddress + "/api/public/stock_zh_dupont_comparison_em?symbol=" + symbol.toUpperCase(), new TypeReference<>() {});
    }

    public List<StockZhAHist> stockZhAHist(String symbol) {
        symbol = symbol.substring(2);
        return executeGet(akshareAddress + "/api/public/stock_zh_a_hist?symbol=" + symbol + "&&adjust=hfq", new TypeReference<>() {});
    }

    /**
     * 历史行情数据-新浪
     * <p>
     * <a href="https://finance.sina.com.cn/realstock/company/sh600006/nc.shtml">...</a>
     * 新浪财经-沪深京 A 股的数据, 历史数据按日频率更新
     *
     * @param symbol 股票代码
     * @return 历史行情日频率数据
     */
    public List<StockZhADaily> stockZhADaily(String symbol, String startDate, String endDate, String adjust) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_zh_a_daily")
                .newBuilder()
                .addQueryParameter("symbol", symbol);

        if (StringUtils.isNotBlank(startDate)) {
            builder.addQueryParameter("start_date", startDate);
        }
        if (StringUtils.isNotBlank(endDate)) {
            builder.addQueryParameter("end_date", endDate);
        }
        if (StringUtils.isNotBlank(adjust)) {
            builder.addQueryParameter("adjust", adjust);
        }

        return executeGet(builder.build(), new TypeReference<>() {});
    }

    /**
     * 获取东方财富-业绩报表数据
     * <p>
     * <a href="https://akshare.akfamily.xyz/data/stock/stock.html#id148">东方财富-业绩报表</a>
     *
     * @param date 报告期，格式为 "YYYYMMDD"（如 "20231231"），通常为季度末日
     * @return A股上市公司业绩报表数据
     */
    public List<StockYjbbEm> stockYjbbEm(String date) {
        return executeGet(akshareAddress + "/api/public/stock_yjbb_em?date=" + date, new TypeReference<>() {});
    }

    public List<StockHoldChangeCninfo> stockHoldChangeCninfo() {
        return stockHoldChangeCninfo("全部");
    }

    public List<StockHoldChangeCninfo> stockHoldChangeCninfo(String symbol) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_hold_change_cninfo")
                .newBuilder();
        if (StringUtils.isNotBlank(symbol)) {
            builder.addQueryParameter("symbol", symbol);
        }

        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<StockFhpsEm> stockFhpsEm(String date) {
        return executeGet(akshareAddress + "/api/public/stock_fhps_em?date=" + date, new TypeReference<>() {});
    }

    public List<StockFhpsDetailEm> stockFhpsDetailEm(String symbol) {
        return executeGet(akshareAddress + "/api/public/stock_fhps_detail_em?symbol=" + symbol, new TypeReference<>() {});
    }

    public List<StockFhpsDetailThs> stockFhpsDetailThs(String symbol) {
        return executeGet(akshareAddress + "/api/public/stock_fhps_detail_ths?symbol=" + symbol, new TypeReference<>() {});
    }

    public List<StockBoardIndustryConsEm> stockBoardIndustryConsEm(String symbol) {
        return executeGet(akshareAddress + "/api/public/stock_board_industry_cons_em?symbol=" + symbol, new TypeReference<>() {});
    }

    public List<StockBoardIndustrySummaryThs> stockBoardIndustrySummaryThs() {
        return executeGet(akshareAddress + "/api/public/stock_board_industry_summary_ths", new TypeReference<>() {});
    }

    public List<StockBoardIndustryIndexThs> stockBoardIndustryIndexThs(String symbol, String startDate, String endDate) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_board_industry_index_ths")
                .newBuilder()
                .addQueryParameter("symbol", symbol);

        if (StringUtils.isNotBlank(startDate)) {
            builder.addQueryParameter("start_date", startDate);
        }
        if (StringUtils.isNotBlank(endDate)) {
            builder.addQueryParameter("end_date", endDate);
        }

        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<FundNameEm> fundNameEm() {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/fund_name_em")
                .newBuilder();

        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<FundOpenFundInfoEm> fundOpenFundInfoEm(String symbol, String indicator, String period) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/fund_open_fund_info_em")
                .newBuilder();

        if (StringUtils.isNotBlank(symbol)) {
            builder.addQueryParameter("symbol", symbol);
        }

        if (StringUtils.isNotBlank(indicator)) {
            builder.addQueryParameter("indicator", indicator);
        }

        if (StringUtils.isNotBlank(period)) {
            builder.addQueryParameter("period", period);
        }

        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<FundPurchaseEm> fundPurchaseEm() {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/fund_purchase_em")
                .newBuilder();

        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<FundPortfolioHoldEm> fundPortfolioHoldEm(String symbol, String date) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/fund_portfolio_hold_em")
                .newBuilder()
                .addQueryParameter("symbol", symbol)
                .addQueryParameter("date", date);

        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<StockInfoSzDelist> stockInfoSzDelist(String symbol) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_info_sz_delist")
                .newBuilder()
                .addQueryParameter("symbol", symbol);

        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<StockInfoShDelist> stockInfoShDelist(String symbol) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_info_sh_delist")
                .newBuilder()
                .addQueryParameter("symbol", symbol);

        return executeGet(builder.build(), new TypeReference<>() {});
    }

}
