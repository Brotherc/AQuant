package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockYjbbEm {

    @JsonProperty("股票代码")
    private String stockCode;

    @JsonProperty("股票简称")
    private String stockName;

    @JsonProperty("每股收益")
    private BigDecimal earningsPerShare;

    @JsonProperty("营业总收入-营业总收入")
    private BigDecimal totalRevenue;

    @JsonProperty("营业总收入-同比增长")
    private BigDecimal totalRevenueYoY;

    @JsonProperty("营业总收入-季度环比增长")
    private BigDecimal totalRevenueQoQ;

    @JsonProperty("净利润-净利润")
    private BigDecimal netProfit;

    @JsonProperty("净利润-同比增长")
    private BigDecimal netProfitYoY;

    @JsonProperty("净利润-季度环比增长")
    private BigDecimal netProfitQoQ;

    @JsonProperty("每股净资产")
    private BigDecimal netAssetsPerShare;

    @JsonProperty("净资产收益率")
    private BigDecimal roe;

    @JsonProperty("每股经营现金流量")
    private BigDecimal operatingCashFlowPerShare;

    @JsonProperty("销售毛利率")
    private BigDecimal grossProfitMargin;

    @JsonProperty("所处行业")
    private String industry;

    /**
     * 2026-07-31T00:00:00.000
     */
    @JsonProperty("最新公告日期")
    private String latestAnnouncementDate;

}
