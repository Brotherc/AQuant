package com.brotherc.aquant.portfolio.model.vo;

import com.brotherc.aquant.portfolio.entity.UserBrokerAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户券商账户信息")
public class BrokerAccountVO {

    @Schema(description = "券商账户 ID")
    private Long id;

    @Schema(description = "所属投资组合 ID")
    private Long portfolioId;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "券商代码")
    private String brokerCode;

    @Schema(description = "券商名称")
    private String brokerName;

    @Schema(description = "脱敏后的券商账号")
    private String accountNoMasked;

    @Schema(description = "账户类型")
    private String accountType;

    @Schema(description = "数据同步方式")
    private String syncMode;

    @Schema(description = "账户状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 根据券商账户构造前端展示对象。
     */
    public static BrokerAccountVO from(UserBrokerAccount account) {
        BrokerAccountVO result = new BrokerAccountVO();
        result.setId(account.getId());
        result.setPortfolioId(account.getPortfolioId());
        result.setAccountName(account.getAccountName());
        result.setBrokerCode(account.getBrokerCode());
        result.setBrokerName(account.getBrokerName());
        result.setAccountNoMasked(account.getAccountNoMasked());
        result.setAccountType(account.getAccountType());
        result.setSyncMode(account.getSyncMode());
        result.setStatus(account.getStatus());
        result.setCreateTime(account.getCreateTime());
        return result;
    }

}
