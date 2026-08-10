package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockFhpsDetailThs {

    @JsonProperty("报告期")
    private String reportPeriod;

    @JsonProperty("董事会日期")
    private String boardDate;

    @JsonProperty("股东大会预案公告日期")
    private String shareholdersMeetingProposalDate;

    @JsonProperty("实施公告日")
    private String implementationNoticeDate;

    @JsonProperty("分红方案说明")
    private String dividendPlanDesc;

    @JsonProperty("A股股权登记日")
    private String aShareRecordDate;

    @JsonProperty("A股除权除息日")
    private String aShareExDividendDate;

    @JsonProperty("分红总额")
    private String totalDividend;

    @JsonProperty("方案进度")
    private String planStatus;

    @JsonProperty("股利支付率")
    private String payoutRatio;

    @JsonProperty("税前分红率")
    private String preTaxDividendYield;

}
