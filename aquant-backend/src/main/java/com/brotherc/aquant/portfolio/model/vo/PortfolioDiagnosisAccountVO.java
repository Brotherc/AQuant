package com.brotherc.aquant.portfolio.model.vo;

import com.brotherc.aquant.portfolio.entity.UserBrokerAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "持仓会诊使用的券商账户摘要，不包含账户号码等敏感信息")
public class PortfolioDiagnosisAccountVO {

    @Schema(description = "券商账户 ID")
    private Long id;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "券商代码")
    private String brokerCode;

    @Schema(description = "券商名称")
    private String brokerName;

    @Schema(description = "账户类型")
    private String accountType;

    @Schema(description = "数据同步方式")
    private String syncMode;

    @Schema(description = "账户状态")
    private String status;

    /**
     * 根据券商账户构造会诊所需的账户摘要，不复制账户号码等敏感字段。
     */
    public static PortfolioDiagnosisAccountVO from(UserBrokerAccount account) {
        PortfolioDiagnosisAccountVO result = new PortfolioDiagnosisAccountVO();
        result.setId(account.getId());
        result.setAccountName(account.getAccountName());
        result.setBrokerCode(account.getBrokerCode());
        result.setBrokerName(account.getBrokerName());
        result.setAccountType(account.getAccountType());
        result.setSyncMode(account.getSyncMode());
        result.setStatus(account.getStatus());
        return result;
    }

}
