package com.brotherc.aquant.integration.akshare.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 期货日线行情数据
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FuturesDaily {

    /**
     * 合约代码，如 "PS2609"
     */
    private String symbol;

    /**
     * 交易日，如 "20260821"
     */
    private String date;

    /**
     * 开盘价
     */
    private BigDecimal open;

    /**
     * 最高价
     */
    private BigDecimal high;

    /**
     * 最低价
     */
    private BigDecimal low;

    /**
     * 收盘价
     */
    private BigDecimal close;

    /**
     * 成交量
     */
    private BigDecimal volume;

    /**
     * 持仓量
     */
    @JsonProperty("open_interest")
    private BigDecimal openInterest;

    /**
     * 成交额
     */
    private BigDecimal turnover;

    /**
     * 结算价
     */
    private BigDecimal settle;

    /**
     * 昨结算价
     */
    @JsonProperty("pre_settle")
    private BigDecimal preSettle;

    /**
     * 品种代码，如 "PS"
     */
    private String variety;

}
