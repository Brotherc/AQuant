package com.brotherc.aquant.service.akshare;

import com.brotherc.aquant.model.dto.akshare.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AKShareIndicatorService extends AbstractAKShareService {

    public AKShareIndicatorService(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        super(objectMapper, okHttpClient);
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

}
