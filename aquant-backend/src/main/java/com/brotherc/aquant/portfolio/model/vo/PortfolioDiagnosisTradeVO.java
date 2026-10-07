package com.brotherc.aquant.portfolio.model.vo;

import com.brotherc.aquant.portfolio.entity.UserPortfolioTrade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Stream;

@Data
@Schema(description = "持仓会诊使用的近期交易，不包含外部流水号等非必要字段")
public class PortfolioDiagnosisTradeVO {

    @Schema(description = "交易流水 ID")
    private Long id;

    @Schema(description = "券商账户 ID")
    private Long accountId;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "资产类型")
    private String assetType;

    @Schema(description = "交易市场")
    private String market;

    @Schema(description = "资产代码")
    private String assetCode;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "交易类型")
    private String tradeType;

    @Schema(description = "交易时间")
    private LocalDateTime tradeTime;

    @Schema(description = "交收日期")
    private LocalDate settlementDate;

    @Schema(description = "成交数量")
    private BigDecimal quantity;

    @Schema(description = "成交价格")
    private BigDecimal price;

    @Schema(description = "成交金额")
    private BigDecimal grossAmount;

    @Schema(description = "交易总费用")
    private BigDecimal totalFee;

    @Schema(description = "现金净发生额")
    private BigDecimal netAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "数据来源")
    private String source;

    /**
     * 根据交易流水构造会诊所需的交易信息，不复制外部流水号和备注。
     */
    public static PortfolioDiagnosisTradeVO from(UserPortfolioTrade trade, String accountName) {
        PortfolioDiagnosisTradeVO result = new PortfolioDiagnosisTradeVO();
        result.setId(trade.getId());
        result.setAccountId(trade.getAccountId());
        result.setAccountName(accountName);
        result.setAssetType(trade.getAssetType());
        result.setMarket(trade.getMarket());
        result.setAssetCode(trade.getAssetCode());
        result.setAssetName(trade.getAssetName());
        result.setTradeType(trade.getTradeType());
        result.setTradeTime(trade.getTradeTime());
        result.setSettlementDate(trade.getSettlementDate());
        result.setQuantity(trade.getQuantity());
        result.setPrice(trade.getPrice());
        result.setGrossAmount(trade.getGrossAmount());
        result.setTotalFee(Stream.of(
                        trade.getCommission(), trade.getStampDuty(), trade.getTransferFee(), trade.getOtherFee())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setNetAmount(trade.getNetAmount());
        result.setCurrency(trade.getCurrency());
        result.setSource(trade.getSource());
        return result;
    }

}
