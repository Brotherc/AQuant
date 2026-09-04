package com.brotherc.aquant.stock.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分时单分钟数据点
 */
@Data
public class StockMinutePointVO {

    /**
     * 时间，格式 HH:mm
     */
    private String time;

    /**
     * 该分钟价格
     */
    private BigDecimal price;

    /**
     * 均价（累计成交额/累计成交量），停牌分钟为 null
     */
    private BigDecimal avgPrice;

    /**
     * 该分钟成交量，单位：手
     */
    private BigDecimal volume;
}
