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
@Schema(description = "投资组合交易及资金流水信息")
public class PortfolioTradeVO {

    @Schema(description = "交易流水主键 ID")
    private Long id;

    @Schema(description = "所属券商账户 ID")
    private Long accountId;

    @Schema(description = "所属券商账户名称")
    private String accountName;

    @Schema(description = "导入批次 ID；手工录入或期初建仓也可能关联系统生成的批次")
    private Long importBatchId;

    @Schema(description = "资产类型", allowableValues = {"STOCK", "ETF", "FUND", "BOND", "CASH"})
    private String assetType;

    @Schema(description = "交易市场", allowableValues = {"SH", "SZ", "BJ"})
    private String market;

    @Schema(description = "资产代码")
    private String assetCode;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(
            description = "交易类型",
            allowableValues = {
                    "BUY", "SELL", "SUBSCRIBE", "REDEEM", "TRANSFER_IN", "TRANSFER_OUT",
                    "DIVIDEND_SHARE", "DIVIDEND_CASH", "FEE", "TAX", "INTEREST",
                    "CASH_DEPOSIT", "CASH_WITHDRAW", "POSITION_INIT"
            }
    )
    private String tradeType;

    @Schema(description = "交易发生时间")
    private LocalDateTime tradeTime;

    @Schema(description = "交收或清算日期")
    private LocalDate settlementDate;

    @Schema(description = "成交数量")
    private BigDecimal quantity;

    @Schema(description = "成交价格")
    private BigDecimal price;

    @Schema(description = "成交金额，不含交易费用")
    private BigDecimal grossAmount;

    @Schema(description = "交易总费用，包括佣金、印花税、过户费及其他费用")
    private BigDecimal totalFee;

    @Schema(description = "现金净发生额；正数表示资金流入，负数表示资金流出")
    private BigDecimal netAmount;

    @Schema(description = "交易币种，使用 ISO 4217 代码")
    private String currency;

    @Schema(description = "流水来源；MANUAL 表示手工录入，FILE 表示文件导入", allowableValues = {"MANUAL", "FILE"})
    private String source;

    @Schema(description = "券商或外部系统提供的原始流水号")
    private String sourceTradeId;

    @Schema(description = "流水状态", allowableValues = {"NORMAL", "REVERSED"})
    private String status;

    @Schema(description = "流水备注或券商业务摘要")
    private String remark;

    /**
     * 根据交易流水构造前端展示对象。
     */
    public static PortfolioTradeVO from(UserPortfolioTrade trade, String accountName) {
        PortfolioTradeVO result = new PortfolioTradeVO();
        result.setId(trade.getId());
        result.setAccountId(trade.getAccountId());
        result.setAccountName(accountName);
        result.setImportBatchId(trade.getImportBatchId());
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
        result.setSourceTradeId(trade.getSourceTradeId());
        result.setStatus(trade.getStatus());
        result.setRemark(trade.getRemark());
        return result;
    }

}
