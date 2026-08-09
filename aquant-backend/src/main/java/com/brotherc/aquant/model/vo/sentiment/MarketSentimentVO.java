package com.brotherc.aquant.model.vo.sentiment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "大盘分析与精细化涨跌分布 VO")
public class MarketSentimentVO {

    @Schema(description = "统计总股票数")
    private Integer totalCount;

    @Schema(description = "总上涨家数")
    private Integer riseCount;

    @Schema(description = "总下跌家数")
    private Integer fallCount;

    @Schema(description = "平盘家数")
    private Integer flatCount;

    @Schema(description = "全市场股票总成交额")
    private BigDecimal totalTurnover;

    @Schema(description = "较昨日放量/缩量金额 (正数表示放量，负数表示缩量)")
    private BigDecimal turnoverChangeAmount;

    @Schema(description = "市场温度得分 (0~100)")
    private Integer temperature;

    @Schema(description = "市场情绪状态标签")
    private String temperatureLabel;

    @Schema(description = "大涨 >5% 家数")
    private Integer strongRiseCount;

    @Schema(description = "大跌 <-5% 家数")
    private Integer strongFallCount;

    // ----- 15 个高密度精细化涨跌分布区间统计 -----

    @Schema(description = "涨停家数 (>= 9.8%)")
    private Integer limitUpCount;

    @Schema(description = "上涨 8%~9.8% 家数")
    private Integer up8ToMaxCount;

    @Schema(description = "上涨 6%~8% 家数")
    private Integer up6To8Count;

    @Schema(description = "上涨 4%~6% 家数")
    private Integer up4To6Count;

    @Schema(description = "上涨 2%~4% 家数")
    private Integer up2To4Count;

    @Schema(description = "上涨 1%~2% 家数")
    private Integer up1To2Count;

    @Schema(description = "微涨 0%~1% 家数")
    private Integer up0To1Count;

    @Schema(description = "微跌 0%~-1% 家数")
    private Integer down0To1Count;

    @Schema(description = "下跌 -1%~-2% 家数")
    private Integer down1To2Count;

    @Schema(description = "下跌 -2%~-4% 家数")
    private Integer down2To4Count;

    @Schema(description = "下跌 -4%~-6% 家数")
    private Integer down4To6Count;

    @Schema(description = "下跌 -6%~-8% 家数")
    private Integer down6To8Count;

    @Schema(description = "下跌 -8%~-9.8% 家数")
    private Integer down8ToMinCount;

    @Schema(description = "跌停家数 (<= -9.8%)")
    private Integer limitDownCount;

}
