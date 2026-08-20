package com.brotherc.aquant.integration.akshare.service;

import com.brotherc.aquant.integration.akshare.model.FundNameEm;
import com.brotherc.aquant.integration.akshare.model.FundOpenFundInfoEm;
import com.brotherc.aquant.integration.akshare.model.FundPortfolioHoldEm;
import com.brotherc.aquant.integration.akshare.model.FundPurchaseEm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AKShareFundService extends AbstractAKShareService {

    public AKShareFundService(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        super(objectMapper, okHttpClient);
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

}
