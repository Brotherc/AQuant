package com.brotherc.aquant.portfolio.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 券商渠道实时持仓快照（多证券）。资产与持仓指标全部直取券商接口原值
 * （jywg queryAssetAndPositionV1 实测：total_asset/security_market_value/
 * enable_balance/fetch_balance/frozen_balance/money_balance/position_income/
 * day_income + positions[].security_code/holding_quantity/cost_price/last_price/
 * market_value/income/income_rate/day_income/day_income_rate 等），
 * 本地不做推算，避免与券商口径（如摊薄成本为负、盈亏比例缺省 0.000%）不一致。
 */
@Data
public class BrokerPositionSnapshotVO {

    @Schema(description = "总资产")
    private BigDecimal totalAsset;

    @Schema(description = "证券市值")
    private BigDecimal securityMarketValue;

    @Schema(description = "可用资金")
    private BigDecimal enableBalance;

    @Schema(description = "可取资金")
    private BigDecimal fetchBalance;

    @Schema(description = "冻结资金")
    private BigDecimal frozenBalance;

    @Schema(description = "资金余额")
    private BigDecimal moneyBalance;

    @Schema(description = "持仓盈亏")
    private BigDecimal positionIncome;

    @Schema(description = "当日盈亏")
    private BigDecimal dayIncome;

    @Schema(description = "渠道名（EM_WEB/EASYTRADER），前端按渠道渲染")
    private String channel;

    @Schema(description = "快照获取时间（ISO 8601）")
    private String fetchTime;

    @Schema(description = "持仓明细（多证券）")
    private List<BrokerPositionItemVO> positions = new ArrayList<>();

    @Data
    public static class BrokerPositionItemVO {

        @Schema(description = "证券代码")
        private String securityCode;

        @Schema(description = "证券名称")
        private String securityName;

        @Schema(description = "持仓数量")
        private BigDecimal holdingQuantity;

        @Schema(description = "可用数量")
        private BigDecimal enableQuantity;

        @Schema(description = "成本价（券商口径，摊薄后可为负）")
        private BigDecimal costPrice;

        @Schema(description = "最新价")
        private BigDecimal lastPrice;

        @Schema(description = "最新市值")
        private BigDecimal marketValue;

        @Schema(description = "持仓盈亏")
        private BigDecimal income;

        @Schema(description = "持仓盈亏比例(%)")
        private BigDecimal incomeRate;

        @Schema(description = "当日盈亏")
        private BigDecimal dayIncome;

        @Schema(description = "当日盈亏比例(%)")
        private BigDecimal dayIncomeRate;

        @Schema(description = "保本价")
        private BigDecimal keepCostPrice;
    }
}
