package com.brotherc.aquant.portfolio.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioImportResultVO {

    private Long batchId;
    private Integer totalCount;
    private Integer successCount;
    private Integer skipCount;
}
