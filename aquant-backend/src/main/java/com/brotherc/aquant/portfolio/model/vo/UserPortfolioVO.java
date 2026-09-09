package com.brotherc.aquant.portfolio.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPortfolioVO {

    private Long id;
    private String name;
    private String baseCurrency;
    private String benchmarkCode;
    private Boolean defaultPortfolio;
    private Integer accountCount;
    private LocalDateTime createTime;
}
