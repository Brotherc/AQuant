package com.brotherc.aquant.model.vo.fundflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "大盘全景汇总统计VO")
public class FundFlowSummaryVO {

    @Schema(description = "大盘总成交额 (元)")
    private BigDecimal totalMarketAmount;

    @Schema(description = "净流入最高行业名称")
    private String topInflowSector;

    @Schema(description = "净流入最高行业金额 (元)")
    private BigDecimal topInflowAmount;

    @Schema(description = "净流出最高行业名称")
    private String topOutflowSector;

    @Schema(description = "净流出最高行业金额 (元)")
    private BigDecimal topOutflowAmount;

    @Schema(description = "全市场上涨家数")
    private Integer riseCountTotal;

    @Schema(description = "全市场下跌家数")
    private Integer fallCountTotal;

    @Schema(description = "净流入前 5 行业")
    private List<FundFlowGraphNodeVO> topInflowSectors;

    @Schema(description = "净流出前 5 行业")
    private List<FundFlowGraphNodeVO> topOutflowSectors;

}
