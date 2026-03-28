package com.brotherc.aquant.model.vo.strategy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Schema(description = "策略信号查询返参")
public class StockTradeSignalVO {

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "股票名称")
    private String name;

    @Schema(description = "交易信号")
    private String signal;

    @Schema(description = "最新价")
    private BigDecimal latestPrice;

    @Schema(description = "当前收盘价占历史最高价与最低价的区间位置指标")
    private BigDecimal pir;

    @Schema(description = "动量值(%)")
    private BigDecimal momentumValue;

    public StockTradeSignalVO(String code, String name, String signal, BigDecimal latestPrice, BigDecimal pir) {
        this.code = code;
        this.name = name;
        this.signal = signal;
        this.latestPrice = latestPrice;
        this.pir = pir;
    }

    public StockTradeSignalVO(String code, String name, String signal, BigDecimal latestPrice, BigDecimal pir, BigDecimal momentumValue) {
        this(code, name, signal, latestPrice, pir);
        this.momentumValue = momentumValue;
    }

}
