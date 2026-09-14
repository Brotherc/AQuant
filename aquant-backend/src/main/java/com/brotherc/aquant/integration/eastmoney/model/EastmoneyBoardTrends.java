package com.brotherc.aquant.integration.eastmoney.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 东方财富板块分时走势 (push2his trends2 接口，ndays 指定天数)
 */
@Data
public class EastmoneyBoardTrends {

    private String code;
    private String name;
    /** 昨收 */
    private BigDecimal preClose;
    /** 分时点（时间升序） */
    private List<Trend> trends = new ArrayList<>();

    @Data
    public static class Trend {
        /** 时间 yyyy-MM-dd HH:mm */
        private String time;
        /** 分时价格（开盘/现价/最高/最低 逐分钟演进，取现价列） */
        private BigDecimal price;
        /** 均价 */
        private BigDecimal avgPrice;
        /** 成交量（手，单分钟增量） */
        private BigDecimal volume;
        /** 成交额（元，单分钟增量） */
        private BigDecimal amount;
    }
}
