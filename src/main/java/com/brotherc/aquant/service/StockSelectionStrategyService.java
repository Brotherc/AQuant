package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockDupontAnalysis;
import com.brotherc.aquant.entity.StockGrowthMetrics;
import com.brotherc.aquant.entity.StockValuationMetrics;
import com.brotherc.aquant.model.dto.stockselect.*;
import com.brotherc.aquant.utils.BigDecimalMathUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockSelectionStrategyService {

    // 各维度权重配置
    // 盈利能力权重35%
    private static final BigDecimal PROFITABILITY_WEIGHT = BigDecimal.valueOf(0.35);
    // 成长性权重30%
    private static final BigDecimal GROWTH_WEIGHT = BigDecimal.valueOf(0.30);
    // 估值合理性权重25%
    private static final BigDecimal VALUATION_WEIGHT = BigDecimal.valueOf(0.25);
    // 运营效率权重10%
    private static final BigDecimal OPERATION_WEIGHT = BigDecimal.valueOf(0.10);

    /**
     * 计算股票综合评分
     * 整合四个维度的评分，按权重计算总分
     *
     * @param dupontAnalysis   杜邦分析数据（盈利能力和运营效率）
     * @param growthMetrics    成长性指标数据
     * @param valuationMetrics 估值指标数据
     * @return 股票选股综合评分结果
     */
    public StockSelectionResult calculateStockScore(
            StockDupontAnalysis dupontAnalysis,
            StockGrowthMetrics growthMetrics,
            StockValuationMetrics valuationMetrics) {

        StockSelectionResult result = new StockSelectionResult();
        result.setStockCode(dupontAnalysis.getStockCode());
        result.setStockName(dupontAnalysis.getStockName());

        // 分别计算四个维度的评分
        ProfitabilityScore profitScore = calculateProfitabilityScore(dupontAnalysis);
        GrowthScore growthScore = calculateGrowthScore(growthMetrics);
        ValuationScore valuationScore = calculateValuationScore(valuationMetrics);
        OperationScore operationScore = calculateOperationScore(dupontAnalysis);

        // 按权重计算综合总分
        BigDecimal totalScore = BigDecimal.ZERO;
        totalScore = BigDecimalMathUtil.safeAdd(totalScore,
                BigDecimalMathUtil.safeMultiply(profitScore.getTotalScore(), PROFITABILITY_WEIGHT));
        totalScore = BigDecimalMathUtil.safeAdd(totalScore,
                BigDecimalMathUtil.safeMultiply(growthScore.getTotalScore(), GROWTH_WEIGHT));
        totalScore = BigDecimalMathUtil.safeAdd(totalScore,
                BigDecimalMathUtil.safeMultiply(valuationScore.getTotalScore(), VALUATION_WEIGHT));
        totalScore = BigDecimalMathUtil.safeAdd(totalScore,
                BigDecimalMathUtil.safeMultiply(operationScore.getTotalScore(), OPERATION_WEIGHT));

        // 设置评分结果
        result.setTotalScore(totalScore);
        result.setProfitabilityScore(profitScore);
        result.setGrowthScore(growthScore);
        result.setValuationScore(valuationScore);
        result.setOperationScore(operationScore);
        result.setEvaluationDate(LocalDateTime.now());

        return result;
    }

    /**
     * 计算盈利能力维度评分
     * 评估公司的盈利能力和盈利质量
     *
     * @param dupont 杜邦分析数据
     * @return 盈利能力评分结果
     */
    private ProfitabilityScore calculateProfitabilityScore(StockDupontAnalysis dupont) {
        ProfitabilityScore score = new ProfitabilityScore();
        // 存储各项得分用于计算平均分
        List<BigDecimal> scores = new ArrayList<>();

        // 1. ROE评分：基于3年平均ROE相对于行业中值的表现
        if (BigDecimalMathUtil.isValid(dupont.getRoe3yAvg()) &&
                BigDecimalMathUtil.isValid(dupont.getRoe3yAvgIndustryMed())) {
            BigDecimal roeScore = calculateRelativeScore(dupont.getRoe3yAvg(),
                    dupont.getRoe3yAvgIndustryMed(),
                    BigDecimal.valueOf(10));
            score.setRoeScore(roeScore);
            scores.add(roeScore);
        }

        // 2. 净利率评分：基于3年平均净利率相对于行业中值的表现
        if (BigDecimalMathUtil.isValid(dupont.getNetMargin3yAvg()) &&
                BigDecimalMathUtil.isValid(dupont.getNetMargin3yAvgIndustryMed())) {
            BigDecimal netMarginScore = calculateRelativeScore(dupont.getNetMargin3yAvg(),
                    dupont.getNetMargin3yAvgIndustryMed(),
                    BigDecimal.valueOf(10));
            score.setNetMarginScore(netMarginScore);
            scores.add(netMarginScore);
        }

        // 3. ROE趋势评分：评估ROE是否在改善（最新年度ROE vs 3年平均ROE）
        if (BigDecimalMathUtil.isValid(dupont.getRoeLastYA()) && BigDecimalMathUtil.isValid(dupont.getRoe3yAvg())) {
            BigDecimal trendScore = calculateTrendScore(dupont.getRoeLastYA(),
                    dupont.getRoe3yAvg(),
                    BigDecimal.valueOf(5));
            score.setRoeTrendScore(trendScore);
            scores.add(trendScore);
        }

        // 4. 盈利稳定性评分：评估盈利的稳定性和连续性
        BigDecimal stabilityScore = calculateProfitStability(dupont);
        score.setStabilityScore(stabilityScore);
        scores.add(stabilityScore);

        // 计算盈利能力维度的平均分（各项得分的平均值）
        BigDecimal avgScore = CollectionUtils.isEmpty(scores) ? BigDecimal.ZERO :
                BigDecimalMathUtil.safeDivide(
                        scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                        BigDecimal.valueOf(scores.size())
                );
        score.setTotalScore(avgScore);

        return score;
    }

    /**
     * 计算成长性维度评分
     * 评估公司的成长潜力和增长质量
     *
     * @param growth 成长性指标数据
     * @return 成长性评分结果
     */
    private GrowthScore calculateGrowthScore(StockGrowthMetrics growth) {
        GrowthScore score = new GrowthScore();
        List<BigDecimal> scores = new ArrayList<>();

        // 1. 净利润增长率评分：基于3年复合增长率相对于行业中值的表现
        if (BigDecimalMathUtil.isValid(growth.getNetProfitGrowth3yCagr()) &&
                BigDecimalMathUtil.isValid(growth.getNetProfitGrowth3yCagrIndustryMed())) {
            BigDecimal netProfitGrowthScore = calculateRelativeScore(
                    growth.getNetProfitGrowth3yCagr(),
                    growth.getNetProfitGrowth3yCagrIndustryMed(),
                    BigDecimal.valueOf(8));
            score.setNetProfitGrowthScore(netProfitGrowthScore);
            scores.add(netProfitGrowthScore);
        }

        // 2. 营收增长率评分：基于3年复合增长率相对于行业中值的表现
        if (BigDecimalMathUtil.isValid(growth.getRevenueGrowth3yCagr()) &&
                BigDecimalMathUtil.isValid(growth.getRevenueGrowth3yCagrIndustryMed())) {
            BigDecimal revenueGrowthScore = calculateRelativeScore(
                    growth.getRevenueGrowth3yCagr(),
                    growth.getRevenueGrowth3yCagrIndustryMed(),
                    BigDecimal.valueOf(7));
            score.setRevenueGrowthScore(revenueGrowthScore);
            scores.add(revenueGrowthScore);
        }

        // 3. 最新年度增长评分：基于最新年度净利润增长率的绝对表现
        if (BigDecimalMathUtil.isValid(growth.getNetProfitGrowthLastYA())) {
            BigDecimal latestGrowthScore = calculateAbsoluteScore(
                    growth.getNetProfitGrowthLastYA(),
                    BigDecimal.valueOf(15.0),
                    BigDecimal.valueOf(5));
            score.setLatestGrowthScore(latestGrowthScore);
            scores.add(latestGrowthScore);
        }

        // 4. 增长质量评分：评估净利润增长是否超过营收增长（盈利质量）
        BigDecimal growthQualityScore = calculateGrowthQuality(growth);
        score.setGrowthQualityScore(growthQualityScore);
        scores.add(growthQualityScore);

        // 5. 增长加速评分：评估增长是否在加速（最新增长 vs 平均增长）
        BigDecimal growthAccelerationScore = calculateGrowthAcceleration(growth);
        score.setGrowthAccelerationScore(growthAccelerationScore);
        scores.add(growthAccelerationScore);

        // 计算成长性维度的平均分
        BigDecimal avgScore = scores.isEmpty() ? BigDecimal.ZERO :
                BigDecimalMathUtil.safeDivide(
                        scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                        BigDecimal.valueOf(scores.size())
                );
        score.setTotalScore(avgScore);

        return score;
    }

    /**
     * 计算估值合理性维度评分
     * 评估公司当前的估值水平是否合理（估值指标越低越好）
     *
     * @param valuation 估值指标数据
     * @return 估值合理性评分结果
     */
    private ValuationScore calculateValuationScore(StockValuationMetrics valuation) {
        ValuationScore score = new ValuationScore();
        List<BigDecimal> scores = new ArrayList<>();

        // 1. 市盈率评分：PE相对于行业中值越低越好
        if (BigDecimalMathUtil.isValid(valuation.getPeLastYearA()) &&
                BigDecimalMathUtil.isValid(valuation.getPeLastYearIndustryMed())) {
            BigDecimal peScore = calculateInverseRelativeScore(
                    valuation.getPeLastYearA(),
                    valuation.getPeLastYearIndustryMed(),
                    BigDecimal.valueOf(7));
            score.setPeScore(peScore);
            scores.add(peScore);
        }

        // 2. 市净率评分：PB相对于行业中值越低越好
        if (BigDecimalMathUtil.isValid(valuation.getPbLastYA()) &&
                BigDecimalMathUtil.isValid(valuation.getPbLastYAIndustryMed())) {
            BigDecimal pbScore = calculateInverseRelativeScore(
                    valuation.getPbLastYA(),
                    valuation.getPbLastYAIndustryMed(),
                    BigDecimal.valueOf(6));
            score.setPbScore(pbScore);
            scores.add(pbScore);
        }

        // 3. PEG评分：PEG指标越低越好，理想值小于1
        if (BigDecimalMathUtil.isValid(valuation.getPeg())) {
            BigDecimal pegScore = calculatePEGScore(valuation.getPeg(), BigDecimal.valueOf(6));
            score.setPegScore(pegScore);
            scores.add(pegScore);
        }

        // 4. 市销率评分：PS相对于行业中值越低越好
        if (BigDecimalMathUtil.isValid(valuation.getPsLastYA()) &&
                BigDecimalMathUtil.isValid(valuation.getPsLastYAIndustryMed())) {
            BigDecimal psScore = calculateInverseRelativeScore(
                    valuation.getPsLastYA(),
                    valuation.getPsLastYAIndustryMed(),
                    BigDecimal.valueOf(6));
            score.setPsScore(psScore);
            scores.add(psScore);
        }

        // 计算估值合理性维度的平均分
        BigDecimal avgScore = scores.isEmpty() ? BigDecimal.ZERO :
                BigDecimalMathUtil.safeDivide(
                        scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                        BigDecimal.valueOf(scores.size())
                );
        score.setTotalScore(avgScore);

        return score;
    }

    /**
     * 计算运营效率维度评分
     * 评估公司的资产运营效率和财务杠杆水平
     *
     * @param dupont 杜邦分析数据
     * @return 运营效率评分结果
     */
    private OperationScore calculateOperationScore(StockDupontAnalysis dupont) {
        OperationScore score = new OperationScore();
        List<BigDecimal> scores = new ArrayList<>();

        // 1. 总资产周转率评分：评估资产运营效率（越高越好）
        if (BigDecimalMathUtil.isValid(dupont.getAssetTurnover3yAvg()) &&
                BigDecimalMathUtil.isValid(dupont.getAssetTurnover3yAvgIndustryMed())) {
            BigDecimal turnoverScore = calculateRelativeScore(
                    dupont.getAssetTurnover3yAvg(),
                    dupont.getAssetTurnover3yAvgIndustryMed(),
                    BigDecimal.valueOf(5));
            score.setAssetTurnoverScore(turnoverScore);
            scores.add(turnoverScore);
        }

        // 2. 权益乘数评分：评估财务杠杆合理性（适中为好，避免过高或过低）
        if (BigDecimalMathUtil.isValid(dupont.getEquityMultiplier3yAvg()) &&
                BigDecimalMathUtil.isValid(dupont.getEquityMultiplier3yAvgIndustryMed())) {
            BigDecimal multiplierScore = calculateLeverageScore(
                    dupont.getEquityMultiplier3yAvg(),
                    dupont.getEquityMultiplier3yAvgIndustryMed(),
                    BigDecimal.valueOf(5));
            score.setEquityMultiplierScore(multiplierScore);
            scores.add(multiplierScore);
        }

        // 计算运营效率维度的平均分
        BigDecimal avgScore = scores.isEmpty() ? BigDecimal.ZERO :
                BigDecimalMathUtil.safeDivide(
                        scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                        BigDecimal.valueOf(scores.size())
                );
        score.setTotalScore(avgScore);

        return score;
    }

    // ========== 评分计算核心算法 ==========

    /**
     * 相对评分算法：实际值相对于行业中值的表现（越高越好）
     * 用于盈利能力、成长性、运营效率等正向指标
     *
     * @param actual         实际值
     * @param industryMedian 行业中值
     * @param maxScore       该项最大得分
     * @return 评分结果
     */
    private BigDecimal calculateRelativeScore(BigDecimal actual, BigDecimal industryMedian, BigDecimal maxScore) {
        if (!BigDecimalMathUtil.isValid(actual) || !BigDecimalMathUtil.isValid(industryMedian)) {
            return BigDecimal.ZERO;
        }

        // 计算相对比率：实际值 / 行业中值
        BigDecimal ratio = BigDecimalMathUtil.safeDivide(actual, industryMedian);

        // 根据相对比率分级评分
        if (BigDecimalMathUtil.greaterThan(ratio, BigDecimal.valueOf(1.5))) {
            // 超过50%：满分
            return maxScore;
        }
        if (BigDecimalMathUtil.greaterThan(ratio, BigDecimal.valueOf(1.2))) {
            // 超过20%：80%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.8));
        }
        if (BigDecimalMathUtil.greaterThan(ratio, BigDecimal.ONE)) {
            // // 超过行业中值：60%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.6));
        }
        if (BigDecimalMathUtil.greaterThan(ratio, BigDecimal.valueOf(0.8))) {
            // // 达到80%：40%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.4));
        }
        // 低于80%：20%分数
        return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.2));
    }

    /**
     * 反向相对评分算法：实际值相对于行业中值的表现（越低越好）
     * 用于估值指标，如PE、PB、PS等
     *
     * @param actual         实际值
     * @param industryMedian 行业中值
     * @param maxScore       该项最大得分
     * @return 评分结果
     */
    private BigDecimal calculateInverseRelativeScore(BigDecimal actual, BigDecimal industryMedian, BigDecimal maxScore) {
        if (!BigDecimalMathUtil.isValid(actual) || !BigDecimalMathUtil.isValid(industryMedian)) {
            return BigDecimal.ZERO;
        }

        // 计算相对比率：实际值 / 行业中值
        BigDecimal ratio = BigDecimalMathUtil.safeDivide(actual, industryMedian);

        // 根据相对比率分级评分（比率越低得分越高）
        if (BigDecimalMathUtil.lessThan(ratio, BigDecimal.valueOf(0.7))) {
            // 低于70%：满分
            return maxScore;
        }
        if (BigDecimalMathUtil.lessThan(ratio, BigDecimal.valueOf(0.9))) {
            // 低于90%：80%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.8));
        }
        if (BigDecimalMathUtil.lessThan(ratio, BigDecimal.ONE)) {
            // 低于行业中值：60%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.6));
        }
        if (BigDecimalMathUtil.lessThan(ratio, BigDecimal.valueOf(1.2))) {
            // 低于120%：40%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.4));
        }
        // 高于120%：20%分数
        return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.2));
    }

    /**
     * 绝对评分算法：基于绝对阈值的评分
     * 用于需要绝对标准的指标，如增长率阈值
     *
     * @param actual    实际值
     * @param threshold 评分阈值
     * @param maxScore  该项最大得分
     * @return 评分结果
     */
    private BigDecimal calculateAbsoluteScore(BigDecimal actual, BigDecimal threshold, BigDecimal maxScore) {
        if (!BigDecimalMathUtil.isValid(actual)) return BigDecimal.ZERO;

        // 根据实际值与阈值的比较分级评分
        if (BigDecimalMathUtil.greaterThan(actual, threshold)) {
            // 超过阈值：满分
            return maxScore;
        }
        if (BigDecimalMathUtil.greaterThan(actual, BigDecimalMathUtil.safeMultiply(threshold, BigDecimal.valueOf(0.8)))) {
            // 达到80%阈值：80%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.8));
        }
        if (BigDecimalMathUtil.greaterThan(actual, BigDecimalMathUtil.safeMultiply(threshold, BigDecimal.valueOf(0.6)))) {
            // 达到60%阈值：60%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.6));
        }
        if (BigDecimalMathUtil.greaterThan(actual, BigDecimalMathUtil.safeMultiply(threshold, BigDecimal.valueOf(0.4)))) {
            // 达到40%阈值：40%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.4));
        }
        // 低于40%阈值：20%分数
        return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.2));
    }

    /**
     * PEG评分算法：PEG指标评分（市盈率相对盈利增长比率）
     * PEG = PE / 盈利增长率，理想值小于1
     *
     * @param peg      PEG值
     * @param maxScore 该项最大得分
     * @return 评分结果
     */
    private BigDecimal calculatePEGScore(BigDecimal peg, BigDecimal maxScore) {
        if (!BigDecimalMathUtil.isValid(peg)) return BigDecimal.ZERO;

        // 根据PEG值分级评分（PEG越低越好）
        if (BigDecimalMathUtil.lessThan(peg, BigDecimal.valueOf(0.5))) {
            // PEG < 0.5：满分
            return maxScore;
        }
        if (BigDecimalMathUtil.lessThan(peg, BigDecimal.valueOf(0.8))) {
            // PEG < 0.8：80%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.8));
        }
        if (BigDecimalMathUtil.lessThan(peg, BigDecimal.ONE)) {
            // PEG < 1.0：60%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.6));
        }
        if (BigDecimalMathUtil.lessThan(peg, BigDecimal.valueOf(1.2))) {
            // PEG < 1.2：40%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.4));
        }
        // PEG ≥ 1.2：20%分数
        return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.2));
    }

    /**
     * 杠杆评分算法：评估权益乘数的合理性
     * 权益乘数适中为好，避免过高杠杆或过低杠杆
     *
     * @param actual         实际权益乘数
     * @param industryMedian 行业权益乘数中值
     * @param maxScore       该项最大得分
     * @return 评分结果
     */
    private BigDecimal calculateLeverageScore(BigDecimal actual, BigDecimal industryMedian, BigDecimal maxScore) {
        if (!BigDecimalMathUtil.isValid(actual) || !BigDecimalMathUtil.isValid(industryMedian)) {
            return BigDecimal.ZERO;
        }

        // 计算相对比率：实际值 / 行业中值
        BigDecimal ratio = BigDecimalMathUtil.safeDivide(actual, industryMedian);

        // 理想范围：行业均值的80%-120%（适中的财务杠杆）
        if (BigDecimalMathUtil.greaterThan(ratio, BigDecimal.valueOf(0.8)) &&
                BigDecimalMathUtil.lessThan(ratio, BigDecimal.valueOf(1.2))) {
            // 适中范围：满分
            return maxScore;
        }
        // 可接受范围：行业均值的70%-130%
        if (BigDecimalMathUtil.greaterThan(ratio, BigDecimal.valueOf(0.7)) &&
                BigDecimalMathUtil.lessThan(ratio, BigDecimal.valueOf(1.3))) {
            // 可接受范围：60%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.6));
        }
        // 边缘范围：行业均值的60%-140%
        if (BigDecimalMathUtil.greaterThan(ratio, BigDecimal.valueOf(0.6)) &&
                BigDecimalMathUtil.lessThan(ratio, BigDecimal.valueOf(1.4))) {
            // 边缘范围：30%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.3));
        }
        // 超出边缘范围：0分（风险过高或杠杆过低）
        return BigDecimal.ZERO;
    }

    /**
     * 盈利稳定性评分：评估盈利的连续性和稳定性
     * 通过分析ROE的历史变化趋势来评估
     *
     * @param dupont 杜邦分析数据
     * @return 稳定性评分
     */
    private BigDecimal calculateProfitStability(StockDupontAnalysis dupont) {
        // 获取近三年的ROE历史数据
        List<BigDecimal> roeHistory = Arrays.asList(
                dupont.getRoeLast3yA(), dupont.getRoeLast2yA(), dupont.getRoeLastYA()
        );

        // 检查所有年份数据是否完整
        if (roeHistory.stream().allMatch(BigDecimalMathUtil::isValid)) {
            boolean increasing = true;
            // 检查ROE是否连续增长或保持稳定
            for (int i = 1; i < roeHistory.size(); i++) {
                if (BigDecimalMathUtil.lessThan(roeHistory.get(i), roeHistory.get(i - 1))) {
                    // 发现下降趋势
                    increasing = false;
                    break;
                }
            }
            // 连续增长：高分；有波动：中等分数
            return increasing ? BigDecimal.TEN : BigDecimal.valueOf(5);
        }
        // 数据不完整：低分
        return BigDecimal.valueOf(3);
    }

    /**
     * 增长质量评分：评估增长的质量和可持续性
     * 净利润增长超过营收增长说明盈利质量高
     *
     * @param growth 成长性指标数据
     * @return 增长质量评分
     */
    private BigDecimal calculateGrowthQuality(StockGrowthMetrics growth) {
        if (BigDecimalMathUtil.isValid(growth.getNetProfitGrowth3yCagr()) &&
                BigDecimalMathUtil.isValid(growth.getRevenueGrowth3yCagr())) {
            // 净利润增长 > 营收增长，说明盈利质量高（利润率提升）
            if (BigDecimalMathUtil.greaterThan(growth.getNetProfitGrowth3yCagr(), growth.getRevenueGrowth3yCagr())) {
                // 高质量增长：满分
                return BigDecimal.valueOf(5);
            } else {
                // 一般质量增长：一半分数
                return BigDecimal.valueOf(2.5);
            }
        }
        // 数据缺失：0分
        return BigDecimal.ZERO;
    }

    /**
     * 增长加速评分：评估增长是否在加速
     * 最新增长率超过历史平均增长率说明增长在加速
     *
     * @param growth 成长性指标数据
     * @return 增长加速评分
     */
    private BigDecimal calculateGrowthAcceleration(StockGrowthMetrics growth) {
        if (BigDecimalMathUtil.isValid(growth.getNetProfitGrowthLastYA()) &&
                BigDecimalMathUtil.isValid(growth.getNetProfitGrowth3yCagr())) {
            // 最新增长 > 平均增长，说明增长在加速
            if (BigDecimalMathUtil.greaterThan(growth.getNetProfitGrowthLastYA(), growth.getNetProfitGrowth3yCagr())) {
                // 加速增长：满分
                return BigDecimal.valueOf(5);
            } else {
                // 平稳或减速增长：一半分数
                return BigDecimal.valueOf(2.5);
            }
        }
        // 数据缺失：0分
        return BigDecimal.ZERO;
    }

    /**
     * 趋势评分：评估指标的最新趋势
     * 最新值相对于平均值的表现
     *
     * @param latest   最新值
     * @param average  平均值
     * @param maxScore 该项最大得分
     * @return 趋势评分
     */
    private BigDecimal calculateTrendScore(BigDecimal latest, BigDecimal average, BigDecimal maxScore) {
        if (!BigDecimalMathUtil.isValid(latest) || !BigDecimalMathUtil.isValid(average)) {
            return BigDecimal.ZERO;
        }

        // 计算趋势比率：最新值 / 平均值
        BigDecimal ratio = BigDecimalMathUtil.safeDivide(latest, average);

        // 根据趋势比率分级评分
        if (BigDecimalMathUtil.greaterThan(ratio, BigDecimal.valueOf(1.1))) {
            // 显著改善：满分
            return maxScore;
        }
        if (BigDecimalMathUtil.greaterThan(ratio, BigDecimal.ONE)) {
            // 轻微改善：70%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.7));
        }

        if (BigDecimalMathUtil.greaterThan(ratio, BigDecimal.valueOf(0.9))) {
            // 轻微下降：40%分数
            return BigDecimalMathUtil.safeMultiply(maxScore, BigDecimal.valueOf(0.4));
        }
        // 显著下降：0分
        return BigDecimal.ZERO;
    }
}
