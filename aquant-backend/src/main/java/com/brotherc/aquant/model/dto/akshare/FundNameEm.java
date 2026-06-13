package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FundNameEm {

    /**
     * 基金代码
     */
    @JsonProperty("基金代码")
    private String fundCode;

    /**
     * 拼音缩写
     */
    @JsonProperty("拼音缩写")
    private String pinyinAbbr;

    /**
     * 基金简称
     */
    @JsonProperty("基金简称")
    private String fundName;

    /**
     * 基金类型
     */
    @JsonProperty("基金类型")
    private String fundType;

    /**
     * 拼音全称
     */
    @JsonProperty("拼音全称")
    private String pinyinFull;

}
