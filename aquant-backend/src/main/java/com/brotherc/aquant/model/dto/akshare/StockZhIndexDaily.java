package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
