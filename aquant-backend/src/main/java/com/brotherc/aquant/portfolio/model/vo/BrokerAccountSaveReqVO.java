package com.brotherc.aquant.portfolio.model.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BrokerAccountSaveReqVO {

    private Long id;

    @NotNull(message = "投资组合ID不能为空")
    private Long portfolioId;

    @NotBlank(message = "账户名称不能为空")
    @Size(max = 100, message = "账户名称不能超过100个字符")
    private String accountName;

    @NotBlank(message = "券商代码不能为空")
    @Size(max = 50, message = "券商代码不能超过50个字符")
    private String brokerCode;

    @Size(max = 100, message = "券商名称不能超过100个字符")
    private String brokerName;

    @Size(max = 100, message = "券商账号不能超过100个字符")
    private String accountNo;

    @Size(max = 30, message = "账户类型不能超过30个字符")
    private String accountType = "SECURITIES";

    @Size(max = 30, message = "同步方式不能超过30个字符")
    private String syncMode = "MANUAL";
}
