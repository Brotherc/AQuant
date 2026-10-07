package com.brotherc.aquant.portfolio.model.vo;

import com.brotherc.aquant.portfolio.entity.UserPortfolio;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户投资组合信息")
public class UserPortfolioVO {

    @Schema(description = "投资组合 ID")
    private Long id;

    @Schema(description = "投资组合名称")
    private String name;

    @Schema(description = "组合基准币种，使用 ISO 4217 代码")
    private String baseCurrency;

    @Schema(description = "业绩比较基准代码")
    private String benchmarkCode;

    @Schema(description = "是否为用户默认投资组合")
    private Boolean defaultPortfolio;

    @Schema(description = "组合下的券商账户数量")
    private Integer accountCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 根据投资组合及其账户数量构造前端展示对象。
     */
    public static UserPortfolioVO from(UserPortfolio portfolio, int accountCount) {
        UserPortfolioVO result = new UserPortfolioVO();
        result.setId(portfolio.getId());
        result.setName(portfolio.getName());
        result.setBaseCurrency(portfolio.getBaseCurrency());
        result.setBenchmarkCode(portfolio.getBenchmarkCode());
        result.setDefaultPortfolio(portfolio.getDefaultPortfolio());
        result.setAccountCount(accountCount);
        result.setCreateTime(portfolio.getCreateTime());
        return result;
    }

}
