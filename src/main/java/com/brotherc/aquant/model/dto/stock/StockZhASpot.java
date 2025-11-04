package com.brotherc.aquant.model.dto.stock;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockZhASpot {

    private String 代码;

    private String 名称;

    private BigDecimal 最新价;

    private BigDecimal 涨跌额;

    private BigDecimal 涨跌幅;

    private BigDecimal 买入;

    private BigDecimal 卖出;

    private BigDecimal 昨收;

    private BigDecimal 今开;

    private BigDecimal 最高;

    private BigDecimal 最低;

    private BigDecimal 成交量;

    private BigDecimal 成交额;

    private String 时间戳;

}
