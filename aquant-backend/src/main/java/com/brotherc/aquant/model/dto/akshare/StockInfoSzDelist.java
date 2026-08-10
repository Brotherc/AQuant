package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockInfoSzDelist {

    @JsonProperty("证券代码")
    private String stockCode;

    @JsonProperty("证券简称")
    private String stockName;

    /**
     * 上市日期，1991-01-14T00:00:00.000
     */
    @JsonProperty("上市日期")
    private String listingDate;

    /**
     * 终止上市日期，2002-06-14T00:00:00.000
     */
    @JsonProperty("终止上市日期")
    private String delistingDate;

}
