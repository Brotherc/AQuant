package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AKShare stock_zh_index_daily (股票指数历史日 K 线行情) DTO
 */
@Data
public class StockZhIndexDaily {

    @JsonProperty("date")
    private String date;

    @JsonProperty("open")
    private BigDecimal open;

    @JsonProperty("high")
    private BigDecimal high;

    @JsonProperty("low")
    private BigDecimal low;

    @JsonProperty("close")
    private BigDecimal close;

    @JsonProperty("volume")
    private BigDecimal volume;

}
