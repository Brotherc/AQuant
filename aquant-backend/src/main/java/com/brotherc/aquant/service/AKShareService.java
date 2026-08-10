package com.brotherc.aquant.service;

import com.brotherc.aquant.model.dto.akshare.*;
import com.brotherc.aquant.service.akshare.AbstractAKShareService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AKShareService extends AbstractAKShareService {

    public AKShareService(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        super(objectMapper, okHttpClient);
    }

    public List<StockZhASpot> stockZhASpot() {
        return executeGet(akshareAddress + "/api/public/stock_zh_a_spot", new TypeReference<>() {});
    }

    public List<StockZhIndexSpotSina> stockZhIndexSpotSina() {
        return executeGet(akshareAddress + "/api/public/stock_zh_index_spot_sina", new TypeReference<>() {});
    }

    public List<StockZhIndexDaily> stockZhIndexDaily(String symbol) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_zh_index_daily")
                .newBuilder()
                .addQueryParameter("symbol", symbol);
        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<StockZhValuationComparisonEm> stockZhValuationComparisonEm(String symbol) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_zh_valuation_comparison_em")
                .newBuilder()
                .addQueryParameter("symbol", symbol.toUpperCase());
        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<StockZhGrowthComparisonEm> stockZhGrowthComparisonEm(String symbol) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_zh_growth_comparison_em")
                .newBuilder()
                .addQueryParameter("symbol", symbol.toUpperCase());
        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<StockZhDupontComparisonEm> stockZhDupontComparisonEm(String symbol) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_zh_dupont_comparison_em")
                .newBuilder()
                .addQueryParameter("symbol", symbol.toUpperCase());
        return executeGet(builder.build(), new TypeReference<>() {});
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
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_yjbb_em")
                .newBuilder()
                .addQueryParameter("date", date);
        return executeGet(builder.build(), new TypeReference<>() {});
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
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_fhps_em")
                .newBuilder()
                .addQueryParameter("date", date);
        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<StockFhpsDetailEm> stockFhpsDetailEm(String symbol) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_fhps_detail_em")
                .newBuilder()
                .addQueryParameter("symbol", symbol);
        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<StockFhpsDetailThs> stockFhpsDetailThs(String symbol) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_fhps_detail_ths")
                .newBuilder()
                .addQueryParameter("symbol", symbol);
        return executeGet(builder.build(), new TypeReference<>() {});
    }

    public List<StockBoardIndustryConsEm> stockBoardIndustryConsEm(String symbol) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_board_industry_cons_em")
                .newBuilder()
                .addQueryParameter("symbol", symbol);
        return executeGet(builder.build(), new TypeReference<>() {});
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
