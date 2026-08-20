package com.brotherc.aquant.index.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "资金流动网络节点VO")
public class FundFlowGraphNodeVO {

    @Schema(description = "节点唯一 ID (如 board_半导体, stock_688981)")
    private String id;

    @Schema(description = "显示名称 (如 半导体, 中芯国际)")
    private String name;

    @Schema(description = "节点类型 (board: 行业板块, stock: 核心个股)")
    private String category;

    @Schema(description = "气泡大小尺寸")
    private Integer symbolSize;

    @Schema(description = "涨跌幅 (%)")
    private BigDecimal changePercent;

    @Schema(description = "净流入金额 (元)")
    private BigDecimal netInflow;

    @Schema(description = "成交额 (元)")
    private BigDecimal totalAmount;

    @Schema(description = "领涨股 / 股票代码")
    private String code;

}
