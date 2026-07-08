package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockInfoShDelist {

    /**
     * 公司代码
     */
    @JsonProperty("公司代码")
    private String companyCode;

    /**
     * 公司简称
     */
    @JsonProperty("公司简称")
    private String companyName;

    /**
     * 上市日期，1998-01-22T00:00:00.000
     */
    @JsonProperty("上市日期")
    private String listingDate;

    /**
     * 暂停上市日期，2009-12-29T00:00:00.000
     */
    @JsonProperty("暂停上市日期")
    private String suspendListingDate;

}
