package com.brotherc.aquant.portfolio.model.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PortfolioTradeBatchReqVO {

    @NotNull(message = "券商账户ID不能为空")
    private Long accountId;

    private String source = "MANUAL";

    private String sourceFileName;

    private String sourceFileHash;

    @Valid
    @NotEmpty(message = "交易流水不能为空")
    private List<PortfolioTradeSaveReqVO> trades;
}
