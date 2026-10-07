package com.brotherc.aquant.portfolio.model.vo;

import com.brotherc.aquant.portfolio.entity.UserPortfolio;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "持仓会诊使用的投资组合基本信息")
public class PortfolioDiagnosisPortfolioVO {

    @Schema(description = "投资组合 ID")
    private Long id;

    @Schema(description = "投资组合名称")
    private String name;

    @Schema(description = "组合基准币种")
    private String baseCurrency;

    @Schema(description = "业绩比较基准代码")
    private String benchmarkCode;

    /**
     * 根据用户投资组合构造会诊所需的基本信息。
     */
    public static PortfolioDiagnosisPortfolioVO from(UserPortfolio portfolio) {
        PortfolioDiagnosisPortfolioVO result = new PortfolioDiagnosisPortfolioVO();
        result.setId(portfolio.getId());
        result.setName(portfolio.getName());
        result.setBaseCurrency(portfolio.getBaseCurrency());
        result.setBenchmarkCode(portfolio.getBenchmarkCode());
        return result;
    }

}
