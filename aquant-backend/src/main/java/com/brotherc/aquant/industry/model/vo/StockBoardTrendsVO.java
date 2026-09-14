package com.brotherc.aquant.industry.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 板块分时走势（当日/五日共用，五日时 trends 按交易日分段）
 */
@Data
public class StockBoardTrendsVO {

    /**
     * 板块代码（库内 sectorName，如"院线"）
     */
    private String boardCode;

    /**
     * 板块名称
     */
    private String name;

    /**
     * 昨收
     */
    private BigDecimal prevClose;

    /**
     * 最新价
     */
    private BigDecimal latestPrice;

    /**
     * 分时点（时间升序，HH:mm 或五日 yyyy-MM-dd HH:mm）
     */
    private List<Point> points = new ArrayList<>();

    @Data
    public static class Point {
        private String time;
        private BigDecimal price;
        private BigDecimal avgPrice;
        /** 单分钟成交量（手） */
        private BigDecimal volume;
    }
}
