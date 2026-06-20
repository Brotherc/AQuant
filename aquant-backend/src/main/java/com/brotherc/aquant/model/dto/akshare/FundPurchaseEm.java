package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundPurchaseEm {

    @JsonProperty("序号")
    private Integer seqNo;

    @JsonProperty("基金代码")
    private String fundCode;

    @JsonProperty("基金简称")
    private String fundName;

    @JsonProperty("基金类型")
    private String fundType;

    @JsonProperty("最新净值/万份收益")
    private BigDecimal latestNetValueOrTenThousandIncome;

    @JsonProperty("最新净值/万份收益-报告时间")
    private String latestNetValueReportTime;

    @JsonProperty("申购状态")
    private String purchaseStatus;

    @JsonProperty("赎回状态")
    private String redemptionStatus;

    @JsonProperty("下一开放日")
    private String nextOpenDay;

    @JsonProperty("购买起点")
    private BigDecimal purchaseStartAmount;

    @JsonProperty("日累计限定金额")
    private BigDecimal dailyLimitAmount;

    @JsonProperty("手续费")
    private BigDecimal feeRate;

}
