package com.brotherc.aquant.integration.akshare.service;

import com.brotherc.aquant.integration.akshare.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

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
     * <a href="https://akshare.akfamily.xyz/data/stock/stock.html">分钟行情数据-新浪</a>
     * 返回最近若干交易日（实测约 8 个）的分钟 K，完整日 238 根。
     *
     * @param symbol 股票代码，带交易所前缀，如 sh600519
     * @param period 周期，1/5/15/30/60
     * @param adjust 复权口径，固定传 ""（不复权），避免口径混库
     *
     * @return 分钟K线数据
     */
    public List<StockZhAMinute> stockZhAMinute(String symbol, String period, String adjust) {
        HttpUrl httpUrl = HttpUrl.get(akshareAddress + "/api/public/stock_zh_a_minute")
                .newBuilder()
                .addQueryParameter(SYMBOL, symbol)
                .addQueryParameter("period", period)
                .addQueryParameter("adjust", adjust)
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
