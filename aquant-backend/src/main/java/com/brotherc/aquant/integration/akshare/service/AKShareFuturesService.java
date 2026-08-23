package com.brotherc.aquant.integration.akshare.service;

import com.brotherc.aquant.integration.akshare.model.FuturesDaily;
import com.brotherc.aquant.integration.akshare.model.FuturesMainSina;
import com.brotherc.aquant.integration.akshare.model.FuturesSpotPriceDaily;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * AKShare 期货数据接口服务
 */
@Service
public class AKShareFuturesService extends AbstractAKShareService {

    public AKShareFuturesService(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        super(objectMapper, okHttpClient);
    }

    /**
     * 期货-日线行情（支持指定合约/品种过滤）
     *
     * @param startDate 开始日期，格式如 "20260801"
     * @param endDate   结束日期，格式如 "20260821"
     * @param market    市场/交易所简称，如 "DCE"、"SHFE"、"CZCE"、"CFFEX"、"INE"、"GFEX"
     * @param symbol    品种或合约代码，如 "PS" 或 "PS2609"
     * @return 期货日线行情列表
     */
    public List<FuturesDaily> getFuturesDaily(String startDate, String endDate, String market, String symbol) {
        HttpUrl.Builder builder = HttpUrl.get(akshareAddress + "/api/public/get_futures_daily")
                .newBuilder();

        if (StringUtils.isNotBlank(startDate)) {
            builder.addQueryParameter("start_date", startDate);
        }
        if (StringUtils.isNotBlank(endDate)) {
            builder.addQueryParameter("end_date", endDate);
        }
        if (StringUtils.isNotBlank(market)) {
            builder.addQueryParameter("market", market);
        }
        if (StringUtils.isNotBlank(symbol)) {
            builder.addQueryParameter(SYMBOL, symbol);
        }

        return executeGet(builder.build(), new TypeReference<>() {});
    }

    /**
     * 新浪期货-主力连续日线行情（支持指定日期区间）
     *
     * @param symbol    主力合约代码，如 "V0", "RB0", "TA0"
     * @param startDate 开始日期，格式如 "20260101"
     * @param endDate   结束日期，格式如 "20260821"
     * @return 主力连续日线行情列表
     */
    public List<FuturesMainSina> futuresMainSina(String symbol, String startDate, String endDate) {
        HttpUrl.Builder builder = HttpUrl.get(akshareAddress + "/api/public/futures_main_sina")
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
     * 期货现货价格及基差日线数据（支持指定品种列表过滤）
     *
     * @param startDay 开始日期，格式如 "20240415"
     * @param endDay   结束日期，格式如 "20240418"
     * @param varsList 品种代码列表，如 ["C", "RB", "CU"]
     * @return 期货现货价格及基差列表
     */
    public List<FuturesSpotPriceDaily> futuresSpotPriceDaily(String startDay, String endDay, List<String> varsList) {
        HttpUrl.Builder builder = HttpUrl.get(akshareAddress + "/api/public/futures_spot_price_daily")
                .newBuilder();

        if (StringUtils.isNotBlank(startDay)) {
            builder.addQueryParameter("start_day", startDay);
        }
        if (StringUtils.isNotBlank(endDay)) {
            builder.addQueryParameter("end_day", endDay);
        }
        if (!CollectionUtils.isEmpty(varsList)) {
            builder.addQueryParameter("vars_list", String.join(",", varsList));
        }

        return executeGet(builder.build(), new TypeReference<>() {});
    }

}
