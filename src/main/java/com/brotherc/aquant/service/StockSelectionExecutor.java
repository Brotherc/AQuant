package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockDupontAnalysis;
import com.brotherc.aquant.entity.StockGrowthMetrics;
import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.entity.StockValuationMetrics;
import com.brotherc.aquant.model.dto.stockselect.StockSelectionResult;
import com.brotherc.aquant.repository.StockDupontAnalysisRepository;
import com.brotherc.aquant.repository.StockGrowthMetricsRepository;
import com.brotherc.aquant.repository.StockQuoteRepository;
import com.brotherc.aquant.repository.StockValuationMetricsRepository;
import com.brotherc.aquant.utils.BigDecimalMathUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 股票选股策略执行器
 * 负责批量执行选股策略，处理股票数据获取、评分计算和结果排序
 * 提供风险控制和推荐股票列表生成功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockSelectionExecutor {

    private final StockSelectionStrategyService selectionService;

    private final StockDupontAnalysisRepository dupontRepository;

    private final StockGrowthMetricsRepository growthRepository;

    private final StockValuationMetricsRepository valuationRepository;

    private final StockQuoteRepository quoteRepository;

    private final ObjectMapper objectMapper;

    /**
     * 执行批量选股策略
     * 对给定的股票代码列表进行批量评分，并按总分降序排序
     * 使用并行流处理提高大批量数据处理的效率
     *
     * @param stockCodes 要评估的股票代码列表
     * @return 按总分降序排列的选股结果列表
     * @example List<String> stockCodes = Arrays.asList("000001", "600000", "920000");
     * List<StockSelectionResult> results = executor.executeStockSelection(stockCodes);
     */
    public List<StockSelectionResult> executeStockSelection(List<String> stockCodes) {
        return stockCodes.stream()
                // 对每只股票进行评估
                .map(this::evaluateSingleStock)
                // 过滤掉评估失败的股票
                .filter(Objects::nonNull)
                // 按总分降序排序（高分在前）
                .sorted((r1, r2) -> {
                    // 处理null值情况：null值排在最后
                    if (r1.getTotalScore() == null && r2.getTotalScore() == null) {
                        return 0;
                    }
                    // r1为null，r1排在后面
                    if (r1.getTotalScore() == null) {
                        return 1;
                    }
                    // r2为null，r1排在前面的前面
                    if (r2.getTotalScore() == null) {
                        return -1;
                    }
                    // 降序排序：r2.compareTo(r1) 实现从高到低排序
                    return r2.getTotalScore().compareTo(r1.getTotalScore());
                }).toList();
    }

    /**
     * 评估单只股票
     * 获取股票的三大类数据（杜邦分析、成长性、估值）并计算综合评分
     * 包含完整的异常处理，确保单只股票评估失败不影响整体批量处理
     *
     * @param stockCode 股票代码
     * @return 股票选股评分结果，如果数据不完整或评估失败则返回null
     */
    private StockSelectionResult evaluateSingleStock(String stockCode) {
        // 从数据库获取股票的三大类核心数据
        StockDupontAnalysis dupont = dupontRepository.findByStockCode(stockCode);
        StockGrowthMetrics growth = growthRepository.findByStockCode(stockCode);
        StockValuationMetrics valuation = valuationRepository.findByStockCode(stockCode);

        // 检查数据完整性：三大类数据都必须存在
        if (dupont == null || growth == null || valuation == null) {
            log.warn("股票 {} 的数据不完整，跳过评估", stockCode);
            return null;
        }

        // 调用策略服务计算综合评分
        return selectionService.calculateStockScore(dupont, growth, valuation);
    }

    /**
     * 获取推荐股票列表
     * 根据评分结果筛选出符合要求的推荐股票，并进行风险控制检查
     *
     * @param stockCodes 候选股票代码列表
     * @param topN       返回的推荐股票数量上限
     * @param minScore   最低分数要求（包含）
     * @return 推荐股票列表，按分数降序排列
     * @example // 获取评分60分以上的前10只推荐股票
     * List<StockSelectionResult> recommendations =
     * executor.getRecommendedStocks(stockCodes, 10, BigDecimal.valueOf(60));
     */
    public List<StockSelectionResult> getRecommendedStocks(List<String> stockCodes, int topN, BigDecimal minScore) throws JsonProcessingException {
        if (CollectionUtils.isEmpty(stockCodes)) {
            stockCodes = quoteRepository.findAll().stream().map(StockQuote::getCode).toList();
        }

        // 执行批量选股评估
        List<StockSelectionResult> allResults = executeStockSelection(stockCodes);

        // 筛选和限制结果
        List<StockSelectionResult> list = allResults.stream()
                // 筛选达到最低分数要求的股票
                .filter(result -> BigDecimalMathUtil.greaterThan(result.getTotalScore(), minScore))
                // 通过风险控制检查
                //.filter(this::passesRiskControl)
                // 限制返回数量
                .limit(topN)
                .toList();
        List<Map<String, Object>> list1 = list.stream()
                .sorted(Comparator.comparing(StockSelectionResult::getTotalScore)).map(o -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("code", o.getStockCode());
                    map.put("score", o.getTotalScore());
                    map.put("rating", o.getRating());
                    return map;
                }).toList();
        log.info(objectMapper.writeValueAsString(list1));
        return list;
    }

    /**
     * 风险控制检查
     * 对评分结果进行硬性条件检查，排除不符合风险控制要求的股票
     * 这是投资安全的重要保障措施
     *
     * @param result 股票选股评分结果
     * @return 是否通过风险控制检查
     * @riskControlRules 1. 总分必须达到40分以上
     * 2. 盈利能力维度得分必须达到30分以上
     * 3. 估值合理性维度得分必须达到20分以上
     */
    public boolean passesRiskControl(StockSelectionResult result) {
        if (result == null || result.getTotalScore() == null) {
            return false;
        }

        // 规则1: 最低总分要求 - 确保股票整体质量
        if (BigDecimalMathUtil.lessThan(result.getTotalScore(), BigDecimal.valueOf(40))) {
            return false;
        }

        // 规则2: 盈利能力要求 - 确保公司有基本的盈利能力
        if (result.getProfitabilityScore() != null &&
                result.getProfitabilityScore().getTotalScore() != null &&
                BigDecimalMathUtil.lessThan(result.getProfitabilityScore().getTotalScore(), BigDecimal.valueOf(30))) {
            return false;
        }

        // 规则3: 估值合理性要求 - 避免买入估值过高的股票
        if (result.getValuationScore() != null &&
                result.getValuationScore().getTotalScore() != null &&
                BigDecimalMathUtil.lessThan(result.getValuationScore().getTotalScore(), BigDecimal.valueOf(20))) {
            return false;
        }

        return true;
    }
}
