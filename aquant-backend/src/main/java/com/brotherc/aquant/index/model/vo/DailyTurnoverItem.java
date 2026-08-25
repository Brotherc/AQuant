package com.brotherc.aquant.index.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "单日成交额统计VO")
public class DailyTurnoverItem {

    @Schema(description = "交易日期 (如 06-09)")
    private String date;

    @Schema(description = "成交额 (单位: 万亿)")
    private BigDecimal amount;

    @Schema(description = "是否为今日/最新交易日")
    private Boolean isToday;

}
