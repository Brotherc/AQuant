package com.brotherc.aquant.integration.tencent.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 腾讯 ifzq 分时接口数据（web.ifzq.gtimg.cn/appstock/app/minute/query）
 */
@Data
public class TencentMinuteQuote {

    /**
     * 股票代码，带交易所前缀，如 sh600519
     */
    private String code;

    /**
     * 交易日，格式 yyyy-MM-dd
     */
    private String date;

    /**
     * 股票名称
     */
    private String name;

    /**
     * 昨日收盘价
     */
    private BigDecimal prevClose;

    private List<Point> points = new ArrayList<>();

    @Data
    public static class Point {

        /**
         * 时间，格式 HH:mm
         */
        private String time;

        /**
         * 该分钟价格
         */
        private BigDecimal price;

        /**
         * 累计成交量，单位：手
         */
        private BigDecimal cumVolume;

        /**
         * 累计成交额，单位：元
         */
        private BigDecimal cumAmount;
    }
}
