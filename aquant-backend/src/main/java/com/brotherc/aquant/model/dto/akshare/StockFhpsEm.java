package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockFhpsEm {

    @JsonProperty("代码")
    private String code;

    @JsonProperty("名称")
    private String name;

    @JsonProperty("送转股份-送转总比例")
    private BigDecimal bonusShareTotalRatio;

    @JsonProperty("送转股份-送转比例")
    private BigDecimal bonusShareRatio;

    @JsonProperty("送转股份-转股比例")
    private BigDecimal transferShareRatio;

    @JsonProperty("现金分红-现金分红比例")
    private BigDecimal cashDividendRatio;

    @JsonProperty("现金分红-股息率")
    private BigDecimal dividendYield;

    @JsonProperty("每股收益")
    private BigDecimal earningsPerShare;

    @JsonProperty("每股净资产")
    private BigDecimal netAssetPerShare;

    @JsonProperty("每股公积金")
    private BigDecimal capitalReservePerShare;

    @JsonProperty("每股未分配利润")
    private BigDecimal undistributedProfitPerShare;

    @JsonProperty("净利润同比增长")
    private BigDecimal netProfitGrowthRate;

    @JsonProperty("总股本")
    private Long totalShares;

    @JsonProperty("预案公告日")
    private String proposalAnnouncementDate;

    @JsonProperty("股权登记日")
    private String recordDate;

    @JsonProperty("除权除息日")
    private String exDividendDate;

    @JsonProperty("方案进度")
    private String planStatus;

    @JsonProperty("最新公告日期")
    private String latestAnnouncementDate;

}
