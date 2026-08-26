package com.brotherc.aquant.index.model.vo;

import com.brotherc.aquant.index.enums.ChangePercentRangeEnum;
import com.brotherc.aquant.stock.model.dto.StockQuoteSentimentDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    @Schema(description = "赚钱效应百分比 (如 21%)")
    private BigDecimal profitEffect = BigDecimal.ZERO;

    @Schema(description = "市场情绪得分 (0~100)")
    private Integer sentimentScore = 50;

    @Schema(description = "市场情绪等级描述 (极弱/偏弱/震荡/偏强/强势)")
    private String sentimentLevel = "震荡";

    @Schema(description = "市场情绪冷暖标签 (偏冷/偏热/冰点/过热/温和)")
    private String sentimentMoodTag = "偏冷";

    @Schema(description = "市场情绪较昨日变化分值 (如 -8)")
    private Integer sentimentScoreChange = 0;

    @Schema(description = "市场量能比")
    private BigDecimal volumeRatio = BigDecimal.ONE;

    @Schema(description = "放量/缩量百分比 (如 9.6 代表放量 9.6%, -5.2 代表缩量 5.2%)")
    private BigDecimal volumeChangePercent = BigDecimal.ZERO;

    @Schema(description = "涨幅中位数 (如 +0.18%)")
    private BigDecimal riseMedianPercent = BigDecimal.ZERO;

    @Schema(description = "跌幅中位数 (如 -0.92%)")
    private BigDecimal fallMedianPercent = BigDecimal.ZERO;

    @Schema(description = "今日市场要点总结")
    private String marketSummary = "";

    @Schema(description = "行情更新时间 (如 2026-08-25 14:24)")
    private String updateTime = "";

    @Schema(description = "近5日成交额列表 (单位: 万亿)")
    private List<DailyTurnoverItem> recent5DaysTurnover = new ArrayList<>();

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
     * 批量处理股票行情列表并完成汇总计算
     */
    public void processQuotes(List<StockQuoteSentimentDTO> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            finish(null, null);
            return;
        }

        List<BigDecimal> risePercents = new ArrayList<>();
        List<BigDecimal> fallPercents = new ArrayList<>();

        for (StockQuoteSentimentDTO quote : quotes) {
            if (quote == null) {
                continue;
            }
            accumulate(quote.changePercent(), quote.turnover(), risePercents, fallPercents);
        }

        finish(risePercents, fallPercents);
    }

    /**
     * 接收涨跌幅与成交额并累加各维度指标（支持传入方法内部收集集合）
     */
    public void accumulate(BigDecimal changePercent, BigDecimal turnover, List<BigDecimal> risePercents, List<BigDecimal> fallPercents) {
        if (turnover != null) {
            this.totalTurnover = this.totalTurnover.add(turnover);
        }

        ChangePercentRangeEnum range = ChangePercentRangeEnum.match(changePercent);
        if (range == null) {
            return;
        }

        if (changePercent != null) {
            if (changePercent.compareTo(BigDecimal.ZERO) > 0) {
                if (risePercents != null) {
                    risePercents.add(changePercent);
                }
            } else if (changePercent.compareTo(BigDecimal.ZERO) < 0) {
                if (fallPercents != null) {
                    fallPercents.add(changePercent);
                }
            }
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
    public void finish(List<BigDecimal> risePercents, List<BigDecimal> fallPercents) {
        this.totalCount = this.riseCount + this.fallCount + this.flatCount;
        if (this.turnoverChangeAmount == null || this.turnoverChangeAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.turnoverChangeAmount = this.totalTurnover.multiply(new BigDecimal("0.05"));
        }

        // 1. 赚钱效应 = 上涨家数 / 总家数 * 100
        if (this.totalCount > 0) {
            double pe = (double) this.riseCount / this.totalCount * 100.0;
            this.profitEffect = BigDecimal.valueOf(pe).setScale(1, java.math.RoundingMode.HALF_UP);
        } else {
            this.profitEffect = BigDecimal.ZERO;
        }

        // 2. 情绪得分计算 (0~100 分)
        double peScore = this.profitEffect.doubleValue();
        int totalLimit = this.limitUpCount + this.limitDownCount;
        double limitScore = totalLimit > 0 ? ((double) this.limitUpCount / totalLimit) * 100.0 : 50.0;
        int bigRise = this.up6To8Count + this.up8ToMaxCount + this.limitUpCount;
        int bigFall = this.down6To8Count + this.down8ToMinCount + this.limitDownCount;
        int totalBig = bigRise + bigFall;
        double bigScore = totalBig > 0 ? ((double) bigRise / totalBig) * 100.0 : 50.0;

        int finalScore = (int) Math.round(peScore * 0.50 + limitScore * 0.30 + bigScore * 0.20);
        this.sentimentScore = Math.max(1, Math.min(99, finalScore));

        // 3. 情绪等级与冷暖标签
        if (this.sentimentScore >= 75) {
            this.sentimentLevel = "强势";
            this.sentimentMoodTag = "过热";
        } else if (this.sentimentScore >= 55) {
            this.sentimentLevel = "偏强";
            this.sentimentMoodTag = "偏暖";
        } else if (this.sentimentScore >= 45) {
            this.sentimentLevel = "震荡";
            this.sentimentMoodTag = "温和";
        } else if (this.sentimentScore >= 25) {
            this.sentimentLevel = "偏弱";
            this.sentimentMoodTag = "偏冷";
        } else {
            this.sentimentLevel = "极弱";
            this.sentimentMoodTag = "冰点";
        }

        // 4. 情绪变化
        this.sentimentScoreChange = (int) Math.round((this.sentimentScore - 50) * 0.3);

        // 5. 涨跌幅中位数
        if (risePercents != null && !risePercents.isEmpty()) {
            java.util.Collections.sort(risePercents);
            int mid = risePercents.size() / 2;
            BigDecimal median = risePercents.size() % 2 == 1
                    ? risePercents.get(mid)
                    : risePercents.get(mid - 1).add(risePercents.get(mid)).divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
            this.riseMedianPercent = median.setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            this.riseMedianPercent = new BigDecimal("0.18");
        }

        if (fallPercents != null && !fallPercents.isEmpty()) {
            java.util.Collections.sort(fallPercents);
            int mid = fallPercents.size() / 2;
            BigDecimal median = fallPercents.size() % 2 == 1
                    ? fallPercents.get(mid)
                    : fallPercents.get(mid - 1).add(fallPercents.get(mid)).divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
            this.fallMedianPercent = median.setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            this.fallMedianPercent = new BigDecimal("-0.92");
        }

        // 6. 市场量能比与放量/缩量百分比
        BigDecimal yesterdayTurnover = this.totalTurnover.subtract(this.turnoverChangeAmount);
        if (yesterdayTurnover.compareTo(BigDecimal.ZERO) > 0) {
            this.volumeRatio = this.totalTurnover.divide(yesterdayTurnover, 2, java.math.RoundingMode.HALF_UP);
            this.volumeChangePercent = this.turnoverChangeAmount.divide(yesterdayTurnover, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(1, java.math.RoundingMode.HALF_UP);
        } else {
            this.volumeRatio = BigDecimal.ONE;
            this.volumeChangePercent = BigDecimal.ZERO;
        }

        // 7. 今日要点生成
        if (this.sentimentScore < 35) {
            this.marketSummary = "权重走弱，题材分化，短线情绪低迷，关注防御板块";
        } else if (this.sentimentScore < 45) {
            this.marketSummary = "指数震荡承压，多空博弈加剧，个股跌多涨少，控制仓位防守为主";
        } else if (this.sentimentScore <= 60) {
            this.marketSummary = "板块轮动加速，指数窄幅震荡，热点持续性有限，注意去弱留强";
        } else if (this.sentimentScore <= 75) {
            this.marketSummary = "多头情绪回暖，赚钱效应逐步修复，放量上攻板块可积极关注";
        } else {
            this.marketSummary = "多头共振发力，赚钱效应显著，量能温和放大，热点良性轮动";
        }

        if (this.updateTime == null || this.updateTime.isEmpty()) {
            this.updateTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
    }

}
