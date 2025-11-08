package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockZhValuationComparisonEm {

    private String 代码;
    private String 简称;

    @JsonProperty("PEG")
    private BigDecimal PEG;

    @JsonProperty("市盈率-24A")
    private BigDecimal 市盈率_24A;
    @JsonProperty("市盈率-TTM")
    private BigDecimal 市盈率_TTM;
    @JsonProperty("市盈率-25E")
    private BigDecimal 市盈率_25E;
    @JsonProperty("市盈率-26E")
    private BigDecimal 市盈率_26E;
    @JsonProperty("市盈率-27E")
    private BigDecimal 市盈率_27E;

    @JsonProperty("市销率-24A")
    private BigDecimal 市销率_24A;
    @JsonProperty("市销率-TTM")
    private BigDecimal 市销率_TTM;
    @JsonProperty("市销率-25E")
    private BigDecimal 市销率_25E;
    @JsonProperty("市销率-26E")
    private BigDecimal 市销率_26E;
    @JsonProperty("市销率-27E")
    private BigDecimal 市销率_27E;

    @JsonProperty("市净率-24A")
    private BigDecimal 市净率_24A;
    @JsonProperty("市净率-MRQ")
    private BigDecimal 市净率_MRQ;

    @JsonProperty("市现率PCE-24A")
    private BigDecimal 市现率PCE_24A;
    @JsonProperty("市现率PCE-TTM")
    private BigDecimal 市现率PCE_TTM;
    @JsonProperty("市现率PCF-24A")
    private BigDecimal 市现率PCF_24A;
    @JsonProperty("市现率PCF-TTM")
    private BigDecimal 市现率PCF_TTM;

    @JsonProperty("EV/EBITDA-24A")
    private BigDecimal EV_EBITDA_24A;

    private BigDecimal PEG排名;

}
