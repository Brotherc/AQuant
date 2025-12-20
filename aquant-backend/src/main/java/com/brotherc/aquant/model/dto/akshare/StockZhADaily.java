package com.brotherc.aquant.model.dto.akshare;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockZhADaily {

    /**
     * 交易日
     */
    private String date;

    /**
     * 开盘价
     */
    private BigDecimal open;

    /**
     * 收盘价
     */
    private BigDecimal close;

    /**
     * 最高价
     */
    private BigDecimal high;

    /**
     * 最低价
     */
    private BigDecimal low;

    /**
     * 成交量; 注意单位: 股
     */
    private BigDecimal volume;

    /**
     * 成交额; 注意单位: 元
     */
    private BigDecimal amount;

    /**
     * 流动股本; 注意单位: 股
     */
    private BigDecimal outstanding_share;

    /**
     * 换手率=成交量/流动股本
     */
    private BigDecimal turnover;

}
