package com.brotherc.aquant.integration.akshare.service;

import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryIndexThs;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustrySummaryThs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AKShareIndustryService extends AbstractAKShareService {

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

}
