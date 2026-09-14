package com.brotherc.aquant.integration.eastmoney.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 东方财富板块K线 (push2his kline 接口，klt 指定周期)
 */
@Data
public class EastmoneyBoardKline {

    private String code;
    /** K线周期：1=1分钟 5=5分钟 15/30/60=分钟 101=日K */
    private Integer klt;
    /** 昨收（仅实时请求时用于首日涨跌计算） */
    private BigDecimal preClose;

    private List<Bar> bars = new ArrayList<>();

    @Data
    public static class Bar {
        /** 时间：日K为 yyyy-MM-dd，分钟K为 yyyy-MM-dd HH:mm */
        private String time;
        private BigDecimal openPrice;
        private BigDecimal closePrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        /** 成交量（手） */
        private BigDecimal volume;
        /** 成交额（元） */
        private BigDecimal amount;
        /** 振幅（百分比） */
        private BigDecimal amplitude;
    }
}
