package com.brotherc.aquant.stock.model.dto;

import java.math.BigDecimal;

/**
 * 大盘情绪统计专用轻量投影 DTO (仅包含涨跌幅与成交额)
 *
 * @param changePercent 涨跌幅(%)
 * @param turnover      成交额(元)
 */
public record StockQuoteSentimentDTO(
        BigDecimal changePercent,
        BigDecimal turnover
) {
}
