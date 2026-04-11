package com.brotherc.aquant.model.dto.tencent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 腾讯财经股票行情 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TencentStockQuote {

    /**
     * 股票名称
     */
    private String name;

    /**
     * 最新价
     */
    private BigDecimal price;

}
