package com.brotherc.aquant.stock.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 个股当日分时数据
 */
@Data
public class StockMinuteRealtimeVO {

    /**
     * 股票代码，带交易所前缀，如 sh600519
     */
    private String code;

    /**
     * 股票名称
     */
    private String name;

    /**
     * 交易日，格式 yyyy-MM-dd
     */
    private String tradeDate;

    /**
     * 昨日收盘价
     */
    private BigDecimal prevClose;

    /**
     * 今日开盘价
     */
    private BigDecimal open;

    /**
     * 最新价（当日最后一分钟价格）
     */
    private BigDecimal latestPrice;

    private List<StockMinutePointVO> points = new ArrayList<>();
}
