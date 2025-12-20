package com.brotherc.aquant.model.dto.akshare;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockZhAHist {

    private String 日期;
    private String 股票代码;
    private BigDecimal 开盘;
    private BigDecimal 收盘;
    private BigDecimal 最高;
    private BigDecimal 最低;
    private Long 成交量;
    private BigDecimal 成交额;
    private BigDecimal 振幅;
    private BigDecimal 涨跌幅;
    private BigDecimal 涨跌额;
    private BigDecimal 换手率;

}
