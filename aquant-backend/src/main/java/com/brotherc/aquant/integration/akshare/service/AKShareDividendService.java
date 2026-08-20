package com.brotherc.aquant.integration.akshare.service;

import com.brotherc.aquant.integration.akshare.model.StockFhpsDetailEm;
import com.brotherc.aquant.integration.akshare.model.StockFhpsDetailThs;
import com.brotherc.aquant.integration.akshare.model.StockFhpsEm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AKShareDividendService extends AbstractAKShareService {

    public AKShareDividendService(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        super(objectMapper, okHttpClient);
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

}
