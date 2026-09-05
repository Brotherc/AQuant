package com.brotherc.aquant.stock.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 个股当日分笔成交明细 VO
 */
@Data
public class StockTickTradeVO {

    private String code;
    /** 分笔数量 */
    private Integer total;
    /** 分笔成交（时间升序） */
    private List<Trade> trades = new ArrayList<>();

    @Data
    public static class Trade {
        /** 成交时间 HH:mm:ss */
        private String time;
        /** 成交价格 */
        private BigDecimal price;
        /** 成交量（手） */
        private BigDecimal volume;
        /** 成交方向：B 买盘 / S 卖盘 / M 中性盘 */
        private String direction;
    }
}
