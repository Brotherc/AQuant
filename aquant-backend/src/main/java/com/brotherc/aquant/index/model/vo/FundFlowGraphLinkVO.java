package com.brotherc.aquant.index.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "资金流动网络边VO")
public class FundFlowGraphLinkVO {

    @Schema(description = "源节点 ID")
    private String source;

    @Schema(description = "目标节点 ID")
    private String target;

    @Schema(description = "流动金额 / 强度 (元)")
    private BigDecimal value;

    @Schema(description = "线条粗细权重 (1~10)")
    private Integer weight;

    @Schema(description = "流向描述")
    private String label;

}
