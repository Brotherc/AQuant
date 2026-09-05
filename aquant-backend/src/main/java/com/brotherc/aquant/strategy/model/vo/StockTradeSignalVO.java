package com.brotherc.aquant.strategy.model.vo;

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

    @Schema(description = "MACD快慢线差值(DIF)")
    private BigDecimal dif;

    @Schema(description = "MACD信号线(DEA)")
    private BigDecimal dea;

    @Schema(description = "MACD柱值，按2*(DIF-DEA)计算")
    private BigDecimal macdHistogram;

    @Schema(description = "网格参考价")
    private BigDecimal gridReferencePrice;

    @Schema(description = "下一买入网格价")
    private BigDecimal lowerGridPrice;

    @Schema(description = "下一卖出网格价")
    private BigDecimal upperGridPrice;

    @Schema(description = "当前网格仓位层级，正数表示加仓，负数表示减仓")
    private Integer gridPosition;

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

    public StockTradeSignalVO(
            String code,
            String name,
            String signal,
            BigDecimal latestPrice,
            BigDecimal pir,
            BigDecimal dif,
            BigDecimal dea,
            BigDecimal macdHistogram
    ) {
        this(code, name, signal, latestPrice, pir);
        this.dif = dif;
        this.dea = dea;
        this.macdHistogram = macdHistogram;
    }

    public StockTradeSignalVO(
            String code,
            String name,
            String signal,
            BigDecimal latestPrice,
            BigDecimal pir,
            BigDecimal gridReferencePrice,
            BigDecimal lowerGridPrice,
            BigDecimal upperGridPrice,
            Integer gridPosition
    ) {
        this(code, name, signal, latestPrice, pir);
        this.gridReferencePrice = gridReferencePrice;
        this.lowerGridPrice = lowerGridPrice;
        this.upperGridPrice = upperGridPrice;
        this.gridPosition = gridPosition;
    }

}
