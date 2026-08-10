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
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/stock_zh_index_daily")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol)
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
    }

    public List<StockZhValuationComparisonEm> stockZhValuationComparisonEm(String symbol) {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/stock_zh_valuation_comparison_em")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol.toUpperCase())
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
    }

    public List<StockZhGrowthComparisonEm> stockZhGrowthComparisonEm(String symbol) {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/stock_zh_growth_comparison_em")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol.toUpperCase())
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
    }

    public List<StockZhDupontComparisonEm> stockZhDupontComparisonEm(String symbol) {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/stock_zh_dupont_comparison_em")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol.toUpperCase())
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
    }

    /**
     * <a href="https://akshare.akfamily.xyz/data/stock/stock.html#id25">历史行情数据-新浪</a>
     *
     * @param symbol 股票代码
     * @param startDate 开始查询的日期，20201103
     * @param endDate 结束查询的日期，20201116
     * @param adjust 默认返回不复权的数据; qfq: 返回前复权后的数据; hfq: 返回后复权后的数据; hfq-factor: 返回后复权因子; qfq-factor: 返回前复权因子
     *
     * @return 历史行情数据
     */
    public List<StockZhADaily> stockZhADaily(String symbol, String startDate, String endDate, String adjust) {
        HttpUrl.Builder builder = HttpUrl.get(akshareAddress + "/api/public/stock_zh_a_daily")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol);

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
     * <a href="https://akshare.akfamily.xyz/data/stock/stock.html#id148">东方财富-业绩报表</a>
     *
     * @param date choice of {"XXXX0331", "XXXX0630", "XXXX0930", "XXXX1231"}; 从 20100331 开始
     *
     * @return A股上市公司业绩报表数据
     */
    public List<StockYjbbEm> stockYjbbEm(String date) {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/stock_yjbb_em")
                .newBuilder()
                .addQueryParameter("date", date)
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
    }

    public List<StockHoldChangeCninfo> stockHoldChangeCninfo() {
        return stockHoldChangeCninfo("全部");
    }

    /**
     * <a href="https://akshare.akfamily.xyz/data/stock/stock.html#id278">股本变动</a>
     *
     * @param symbol choice of {"深市主板", "沪市", "创业板", "科创板", "北交所", "全部"}
     *
     * @return 股本变动
     */
    public List<StockHoldChangeCninfo> stockHoldChangeCninfo(String symbol) {
        HttpUrl.Builder builder = HttpUrl.get(akshareAddress + "/api/public/stock_hold_change_cninfo")
                .newBuilder();

        if (StringUtils.isNotBlank(symbol)) {
            builder.addQueryParameter(SYMBOL, symbol);
        }

        return executeGet(builder.build(), new TypeReference<>() {});
    }

    /**
     * <a href="https://akshare.akfamily.xyz/data/stock/stock.html#id168">分红配送-东财</a>
     *
     * @param date choice of {"XXXX0630", "XXXX1231"}; 从 19901231 开始
     *
     * @return 分红配送
     */
    public List<StockFhpsEm> stockFhpsEm(String date) {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/stock_fhps_em")
                .newBuilder()
                .addQueryParameter("date", date)
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
    }

    /**
     * <a href="https://akshare.akfamily.xyz/data/stock/stock.html#id169">分红配送详情-东财</a>
     *
     * @param symbol 股票代码
     *
     * @return 分红配送详情
     */
    public List<StockFhpsDetailEm> stockFhpsDetailEm(String symbol) {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/stock_fhps_detail_em")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol)
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
    }

    /**
     * <a href="https://akshare.akfamily.xyz/data/stock/stock.html#id377">分红情况-同花顺</a>
     *
     * @param symbol 股票代码
     *
     * @return 分红情况
     */
    public List<StockFhpsDetailThs> stockFhpsDetailThs(String symbol) {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/stock_fhps_detail_ths")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol)
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
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

    /**
     * <a href="https://akshare.akfamily.xyz/data/fund/fund_public.html#id1">基金基本信息</a>
     *
     * @return 基金基本信息
     */
    public List<FundNameEm> fundNameEm() {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/fund_name_em")
                .newBuilder()
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
    }

    /**
     * <a href="https://akshare.akfamily.xyz/data/fund/fund_public.html#id17">开放式基金-历史数据</a>
     *
     * @param symbol 基金代码
     * @param indicator choice of {"单位净值走势", "累计净值走势", "累计收益率走势", "同类排名走势", "同类排名百分比", "分红送配详情", "拆分详情"}
     * @param period choice of {"1月", "3月", "6月", "1年", "3年", "5年", "今年来", "成立来"}，"成立来"该参数只对 累计收益率走势 有效
     *
     * @return 基金历史数据
     */
    public List<FundOpenFundInfoEm> fundOpenFundInfoEm(String symbol, String indicator, String period) {
        HttpUrl.Builder builder = HttpUrl.get(akshareAddress + "/api/public/fund_open_fund_info_em")
                .newBuilder();

        if (StringUtils.isNotBlank(symbol)) {
            builder.addQueryParameter(SYMBOL, symbol);
        }

        if (StringUtils.isNotBlank(indicator)) {
            builder.addQueryParameter("indicator", indicator);
        }

        if (StringUtils.isNotBlank(period)) {
            builder.addQueryParameter("period", period);
        }

        return executeGet(builder.build(), new TypeReference<>() {});
    }

    /**
     * <a href="https://akshare.akfamily.xyz/data/fund/fund_public.html#id5">基金申购状态</a>
     *
     * @return 基金申购列表
     */
    public List<FundPurchaseEm> fundPurchaseEm() {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/fund_purchase_em")
                .newBuilder()
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
    }

    /**
     * <a href="https://akshare.akfamily.xyz/data/fund/fund_public.html#id46">基金持仓</a>
     *
     * @param symbol 基金代码，symbol="000001"
     * @param date 指定年份, date="2024"；传入空字符串 "" 时返回最新可用年份数据
     *
     * @return 基金持仓列表
     */
    public List<FundPortfolioHoldEm> fundPortfolioHoldEm(String symbol, String date) {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/fund_portfolio_hold_em")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol)
                .addQueryParameter("date", date)
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
    }

    /**
     * <a href="https://akshare.akfamily.xyz/data/stock/stock.html#id258">终止/暂停上市-深证</a>
     *
     * @param symbol choice of {"暂停上市公司", "终止上市公司"}
     *
     * @return 深证交易所终止/暂停上市股票代码列表
     */
    public List<StockInfoSzDelist> stockInfoSzDelist(String symbol) {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/stock_info_sz_delist")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol)
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
    }

    /**
     * <a href="https://akshare.akfamily.xyz/data/stock/stock.html#id258">暂停/终止上市-上证</a>
     *
     * @param symbol choice of {"全部", "沪市", "科创板"}
     *
     * @return 上证交易所暂停/终止上市股票代码列表
     */
    public List<StockInfoShDelist> stockInfoShDelist(String symbol) {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/stock_info_sh_delist")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol)
                .build();
        return executeGet(httpUrl, new TypeReference<>() {});
    }

}
