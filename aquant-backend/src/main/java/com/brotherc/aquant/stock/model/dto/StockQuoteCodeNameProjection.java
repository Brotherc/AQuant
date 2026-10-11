package com.brotherc.aquant.stock.model.dto;

/**
 * 股票代码与名称投影，用于无需加载完整行情的股票清单查询。
 */
public interface StockQuoteCodeNameProjection {

    /**
     * 带交易所前缀的股票代码。
     */
    String getCode();

    /**
     * 股票名称。
     */
    String getName();

}
