package com.brotherc.aquant.integration.akshare.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 期货现货价格与基差日线数据
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FuturesSpotPriceDaily {

    /**
     * 日期，格式如 "20240415"
     */
    private String date;

    /**
     * 品种代码，如 "C"
     */
    private String symbol;

    /**
     * 现货价格
     */
    @JsonProperty("spot_price")
    private BigDecimal spotPrice;

    /**
     * 近月合约代码，如 "c2405"
     */
    @JsonProperty("near_contract")
    private String nearContract;

    /**
     * 近月合约价格
     */
    @JsonProperty("near_contract_price")
    private BigDecimal nearContractPrice;

    /**
     * 主力合约代码，如 "c2405"
     */
    @JsonProperty("dominant_contract")
    private String dominantContract;

    /**
     * 主力合约价格
     */
    @JsonProperty("dominant_contract_price")
    private BigDecimal dominantContractPrice;

    /**
     * 近月交割月份，如 "2405"
     */
    @JsonProperty("near_month")
    private String nearMonth;

    /**
     * 主力合约月份，如 "2405"
     */
    @JsonProperty("dominant_month")
    private String dominantMonth;

    /**
     * 近月基差
     */
    @JsonProperty("near_basis")
    private BigDecimal nearBasis;

    /**
     * 主力基差
     */
    @JsonProperty("dom_basis")
    private BigDecimal domBasis;

    /**
     * 近月基差率
     */
    @JsonProperty("near_basis_rate")
    private BigDecimal nearBasisRate;

    /**
     * 主力基差率
     */
    @JsonProperty("dom_basis_rate")
    private BigDecimal domBasisRate;

}
