package com.brotherc.aquant.integration.tencent.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 腾讯实时盘口（qt.gtimg.cn 单股行情，含五档买卖盘）
 */
@Data
public class TencentOrderBook {

    private String code;
    private String name;
    private BigDecimal latestPrice;
    private BigDecimal prevClose;
    private BigDecimal open;
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal volume;       // 成交量(手)
    private BigDecimal turnover;     // 成交额(万)
    private BigDecimal turnoverRate; // 换手率%
    private BigDecimal quantityRatio; // 量比
    private String quoteTime;        // yyyyMMddHHmmss

    private List<Level> bids = new ArrayList<>(); // 买一~买五
    private List<Level> asks = new ArrayList<>(); // 卖一~卖五

    @Data
    public static class Level {
        private BigDecimal price;
        private BigDecimal volume; // 手

        public Level() {
        }

        public Level(BigDecimal price, BigDecimal volume) {
            this.price = price;
            this.volume = volume;
        }
    }
}
