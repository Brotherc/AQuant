package com.brotherc.aquant.model.dto.akshare;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockHistoryDividend {

    private String 代码;

    private String 名称;

    private String 上市日期;

    private BigDecimal 累计股息;

    private BigDecimal 年均股息;

    private Integer 分红次数;

    private BigDecimal 融资总额;

    private Integer 融资次数;

}
