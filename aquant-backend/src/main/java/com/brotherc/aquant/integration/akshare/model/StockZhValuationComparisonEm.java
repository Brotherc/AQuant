package com.brotherc.aquant.integration.akshare.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockZhValuationComparisonEm {

    @JsonProperty("代码")
    private String code;

    @JsonProperty("简称")
    private String name;

    @JsonProperty("PEG")
    private BigDecimal peg;

    @JsonProperty("市盈率-24A")
    private BigDecimal pe24a;

    @JsonProperty("市盈率-TTM")
    private BigDecimal peTtm;

    @JsonProperty("市盈率-25E")
    private BigDecimal pe25e;

    @JsonProperty("市盈率-26E")
    private BigDecimal pe26e;

    @JsonProperty("市盈率-27E")
    private BigDecimal pe27e;

    @JsonProperty("市销率-24A")
    private BigDecimal ps24a;

    @JsonProperty("市销率-TTM")
    private BigDecimal psTtm;

    @JsonProperty("市销率-25E")
    private BigDecimal ps25e;

    @JsonProperty("市销率-26E")
    private BigDecimal ps26e;

    @JsonProperty("市销率-27E")
    private BigDecimal ps27e;

    @JsonProperty("市净率-24A")
    private BigDecimal pb24a;

    @JsonProperty("市净率-MRQ")
    private BigDecimal pbMrq;

    @JsonProperty("市现率PCE-24A")
    private BigDecimal pce24a;

    @JsonProperty("市现率PCE-TTM")
    private BigDecimal pceTtm;

    @JsonProperty("市现率PCF-24A")
    private BigDecimal pcf24a;

    @JsonProperty("市现率PCF-TTM")
    private BigDecimal pcfTtm;

    @JsonProperty("EV/EBITDA-24A")
    private BigDecimal evEbitda24a;

    @JsonProperty("PEG排名")
    private BigDecimal pegRank;

}
