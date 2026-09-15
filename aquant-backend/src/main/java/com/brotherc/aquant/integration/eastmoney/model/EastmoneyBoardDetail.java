package com.brotherc.aquant.integration.eastmoney.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 东方财富行业板块详情快照 (push2 stock/get 接口，板块详情页顶部的盘口指标)。
 * 板块列表接口（clist）只有涨跌幅/成交/资金等汇总列，今开/昨收/最高/最低/换手/量比/
 * 内外盘/流通市值/流通股本仅在单板块详情接口返回。
 */
@Data
public class EastmoneyBoardDetail {

    /** 板块代码，如 BK1201 (f57) */
    private String sectorCode;
    /** 板块名称 (f58) */
    private String sectorName;
    /** 最新价 (f43) */
    private BigDecimal latestPrice;
    /** 今开 (f46) */
    private BigDecimal openPrice;
    /** 昨收 (f60) */
    private BigDecimal preClosePrice;
    /** 最高 (f44) */
    private BigDecimal highPrice;
    /** 最低 (f45) */
    private BigDecimal lowPrice;
    /** 涨跌额 (f169) */
    private BigDecimal changeAmount;
    /** 涨跌幅（百分比）(f170) */
    private BigDecimal changePercent;
    /** 振幅（百分比）(f171) */
    private BigDecimal amplitude;
    /** 成交量（手）(f47) */
    private BigDecimal volume;
    /** 成交额（元）(f48) */
    private BigDecimal amount;
    /** 换手率（百分比）(f168) */
    private BigDecimal turnoverRate;
    /** 量比 (f50) */
    private BigDecimal volumeRatio;
    /** 外盘（手）(f49)。上游无独立内盘字段，内盘=成交量-外盘 */
    private BigDecimal outerDisc;
    /** 流通市值（元）(f117) */
    private BigDecimal circulatingMarketValue;
    /** 流通股本（股）(f85) */
    private BigDecimal circulatingShares;
}
