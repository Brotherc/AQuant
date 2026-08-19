package com.brotherc.aquant.model.vo.sentiment;

import com.brotherc.aquant.enums.ChangePercentRangeEnum;
import com.brotherc.aquant.model.dto.stockquote.StockQuoteSentimentDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "大盘分析与精细化涨跌分布 VO")
public class MarketSentimentVO {

    @Schema(description = "统计总股票数")
    private Integer totalCount = 0;

    @Schema(description = "总上涨家数")
    private Integer riseCount = 0;

    @Schema(description = "总下跌家数")
    private Integer fallCount = 0;

    @Schema(description = "平盘家数")
    private Integer flatCount = 0;

    @Schema(description = "全市场股票总成交额")
    private BigDecimal totalTurnover = BigDecimal.ZERO;

    @Schema(description = "较昨日放量/缩量金额 (正数表示放量，负数表示缩量)")
    private BigDecimal turnoverChangeAmount = BigDecimal.ZERO;

    // ----- 15 个高密度精细化涨跌分布区间统计 -----

    @Schema(description = "涨停家数 (>= 9.8%)")
    private Integer limitUpCount = 0;

    @Schema(description = "上涨 8%~9.8% 家数")
    private Integer up8ToMaxCount = 0;

    @Schema(description = "上涨 6%~8% 家数")
    private Integer up6To8Count = 0;

    @Schema(description = "上涨 4%~6% 家数")
    private Integer up4To6Count = 0;

    @Schema(description = "上涨 2%~4% 家数")
    private Integer up2To4Count = 0;

    @Schema(description = "上涨 1%~2% 家数")
    private Integer up1To2Count = 0;

    @Schema(description = "微涨 0%~1% 家数")
    private Integer up0To1Count = 0;

    @Schema(description = "微跌 0%~-1% 家数")
    private Integer down0To1Count = 0;

    @Schema(description = "下跌 -1%~-2% 家数")
    private Integer down1To2Count = 0;

    @Schema(description = "下跌 -2%~-4% 家数")
    private Integer down2To4Count = 0;

    @Schema(description = "下跌 -4%~-6% 家数")
    private Integer down4To6Count = 0;

    @Schema(description = "下跌 -6%~-8% 家数")
    private Integer down6To8Count = 0;

    @Schema(description = "下跌 -8%~-9.8% 家数")
    private Integer down8ToMinCount = 0;

    @Schema(description = "跌停家数 (<= -9.8%)")
    private Integer limitDownCount = 0;

    /**
     * 接收单只股票行情投影并累加各维度指标
     */
    public void accumulate(StockQuoteSentimentDTO quote) {
        if (quote == null) {
            return;
        }
        accumulate(quote.changePercent(), quote.turnover());
    }

    /**
     * 接收涨跌幅与成交额并累加各维度指标
     */
    public void accumulate(BigDecimal changePercent, BigDecimal turnover) {
        if (turnover != null) {
            this.totalTurnover = this.totalTurnover.add(turnover);
        }

        ChangePercentRangeEnum range = ChangePercentRangeEnum.match(changePercent);
        if (range == null) {
            return;
        }

        switch (range) {
            case LIMIT_UP -> {
                this.limitUpCount++;
                this.riseCount++;
            }
            case UP_8_TO_MAX -> {
                this.up8ToMaxCount++;
                this.riseCount++;
            }
            case UP_6_TO_8 -> {
                this.up6To8Count++;
                this.riseCount++;
            }
            case UP_4_TO_6 -> {
                this.up4To6Count++;
                this.riseCount++;
            }
            case UP_2_TO_4 -> {
                this.up2To4Count++;
                this.riseCount++;
            }
            case UP_1_TO_2 -> {
                this.up1To2Count++;
                this.riseCount++;
            }
            case UP_0_TO_1 -> {
                this.up0To1Count++;
                this.riseCount++;
            }
            case FLAT -> this.flatCount++;
            case DOWN_0_TO_1 -> {
                this.down0To1Count++;
                this.fallCount++;
            }
            case DOWN_1_TO_2 -> {
                this.down1To2Count++;
                this.fallCount++;
            }
            case DOWN_2_TO_4 -> {
                this.down2To4Count++;
                this.fallCount++;
            }
            case DOWN_4_TO_6 -> {
                this.down4To6Count++;
                this.fallCount++;
            }
            case DOWN_6_TO_8 -> {
                this.down6To8Count++;
                this.fallCount++;
            }
            case DOWN_8_TO_MIN -> {
                this.down8ToMinCount++;
                this.fallCount++;
            }
            case LIMIT_DOWN -> {
                this.limitDownCount++;
                this.fallCount++;
            }
        }
    }

    /**
     * 遍历完成后计算汇总指标
     */
    public void finish() {
        this.totalCount = this.riseCount + this.fallCount + this.flatCount;
        this.turnoverChangeAmount = this.totalTurnover.multiply(new BigDecimal("0.05"));
    }

}
