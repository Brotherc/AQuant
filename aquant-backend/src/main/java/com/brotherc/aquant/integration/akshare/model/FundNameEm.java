package com.brotherc.aquant.integration.akshare.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundNameEm {

    @JsonProperty("基金代码")
    private String fundCode;

    @JsonProperty("拼音缩写")
    private String pinyinAbbr;

    @JsonProperty("基金简称")
    private String fundName;

    @JsonProperty("基金类型")
    private String fundType;

    @JsonProperty("拼音全称")
    private String pinyinFull;

}
