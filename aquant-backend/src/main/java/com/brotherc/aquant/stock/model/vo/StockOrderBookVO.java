package com.brotherc.aquant.stock.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 个股实时盘口 VO（五档买卖盘 + 成交信息）
 */
@Data
public class StockOrderBookVO {

    private String code;
    private String name;
    private BigDecimal latestPrice;
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal prevClose;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal volume;        // 成交量(手)
    private BigDecimal turnover;      // 成交额(万)
    private BigDecimal turnoverRate;  // 换手率%
    private BigDecimal quantityRatio; // 量比
    private String quoteTime;         // HH:mm:ss

    private List<Level> bids = new ArrayList<>(); // 买一~买五
    private List<Level> asks = new ArrayList<>(); // 卖一~卖五

    @Data
    public static class Level {
        private BigDecimal price;
        private BigDecimal volume; // 手
    }
}
