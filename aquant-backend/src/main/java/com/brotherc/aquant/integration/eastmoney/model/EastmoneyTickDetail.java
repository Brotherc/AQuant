package com.brotherc.aquant.integration.eastmoney.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 东方财富个股当日分笔成交明细 (push2 details 接口)
 */
@Data
public class EastmoneyTickDetail {

    private String code;
    /** 昨收价 */
    private BigDecimal prePrice;
    /** 分笔成交（时间升序，已过滤 09:25 前的集合竞价虚拟撮合记录） */
    private List<Trade> trades = new ArrayList<>();

    @Data
    public static class Trade {
        /** 成交时间 HH:mm:ss */
        private String time;
        /** 成交价格 */
        private BigDecimal price;
        /** 成交量（手） */
        private BigDecimal volume;
        /** 成交单数 */
        private BigDecimal orderCount;
        /** 方向码：1 买盘 / 2 卖盘 / 4 中性盘 */
        private Integer direction;
    }
}
