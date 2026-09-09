package com.brotherc.aquant.portfolio.model.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortfolioSaveReqVO {

    private Long id;

    @NotBlank(message = "投资组合名称不能为空")
    @Size(max = 100, message = "投资组合名称不能超过100个字符")
    private String name;

    @Size(max = 10, message = "基础币种不能超过10个字符")
    private String baseCurrency = "CNY";

    @Size(max = 20, message = "基准指数代码不能超过20个字符")
    private String benchmarkCode;

    private Boolean defaultPortfolio = false;
}
