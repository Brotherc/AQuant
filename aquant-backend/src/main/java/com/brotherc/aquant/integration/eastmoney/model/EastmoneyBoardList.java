package com.brotherc.aquant.integration.eastmoney.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 东方财富行业板块列表 (push2 clist 接口)
 */
@Data
public class EastmoneyBoardList {

    private List<Board> boards = new ArrayList<>();

    @Data
    public static class Board {
        /** 板块代码，如 BK1300 */
        private String sectorCode;
        /** 板块名称 */
        private String sectorName;
        /** 最新价 */
        private BigDecimal latestPrice;
        /** 涨跌幅（百分比，东财 fltt=2 时已放大100倍为数值，如 2.57 表示 +2.57%） */
        private BigDecimal changePercent;
        /** 总市值（元） */
        private BigDecimal totalMarketValue;
        /** 上涨家数 */
        private Integer riseCount;
        /** 下跌家数 */
        private Integer fallCount;
        /** 领涨股票名称 */
        private String leadingStock;
        /** 领涨股票涨跌幅 */
        private BigDecimal leadingStockChangePercent;
    }
}
