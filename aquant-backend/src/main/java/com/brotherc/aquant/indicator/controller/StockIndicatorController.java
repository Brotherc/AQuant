package com.brotherc.aquant.indicator.controller;

import com.brotherc.aquant.indicator.entity.StockDupontAnalysis;
import com.brotherc.aquant.indicator.entity.StockGrowthMetrics;
import com.brotherc.aquant.common.model.dto.ResponseDTO;
import com.brotherc.aquant.indicator.model.vo.CalculatedValuationMetricsPageVO;
import com.brotherc.aquant.indicator.model.vo.CalculatedValuationMetricsVO;
import com.brotherc.aquant.indicator.model.vo.DupontAnalysisPageReqVO;
import com.brotherc.aquant.indicator.model.vo.GrowthMetricsPageReqVO;
import com.brotherc.aquant.indicator.model.vo.ValuationMetricsPageReqVO;
import com.brotherc.aquant.indicator.service.StockDupontAnalysisService;
import com.brotherc.aquant.indicator.service.StockGrowthMetricsService;
import com.brotherc.aquant.indicator.service.StockValuationMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.brotherc.aquant.indicator.model.vo.DupontOverviewVO;
import com.brotherc.aquant.indicator.model.vo.ValuationOverviewVO;
import com.brotherc.aquant.common.utils.UserContext;

import java.util.List;

@Validated
@Tag(name = "股票指标")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockIndicator")
public class StockIndicatorController {

    private final StockDupontAnalysisService stockDupontAnalysisService;
    private final StockValuationMetricsService stockValuationMetricsService;
    private final StockGrowthMetricsService stockGrowthMetricsService;

    @Operation(summary = "杜邦分析指标分页查询")
    @GetMapping("/dupontAnalysis/page")
    public ResponseDTO<Page<StockDupontAnalysis>> dupontAnalysisPage(
            @Valid @ParameterObject DupontAnalysisPageReqVO reqVO, @ParameterObject Pageable pageable) {
        return ResponseDTO.success(stockDupontAnalysisService.pageQuery(reqVO, pageable));
    }

    @Operation(summary = "杜邦分析顶部概览统计数据")
    @GetMapping("/dupontAnalysis/overview")
    public ResponseDTO<DupontOverviewVO> dupontAnalysisOverview() {
        Long userId = UserContext.getCurrentUserId();
        return ResponseDTO.success(stockDupontAnalysisService.getDupontOverview(userId));
    }

    @Operation(summary = "杜邦分析行业列表查询")
    @GetMapping("/dupontAnalysis/industries")
    public ResponseDTO<List<String>> dupontAnalysisIndustries() {
        return ResponseDTO.success(stockDupontAnalysisService.getIndustries());
    }

    @Operation(summary = "估值指标分页查询")
    @GetMapping("/valuationMetrics/page")
    public ResponseDTO<Page<CalculatedValuationMetricsPageVO>> valuationMetricsPage(
            @Valid @ParameterObject ValuationMetricsPageReqVO reqVO, @ParameterObject Pageable pageable) {
        Long userId = UserContext.getCurrentUserId();
        return ResponseDTO.success(stockValuationMetricsService.pageQuery(reqVO, pageable, userId));
    }

    @Operation(summary = "估值指标顶部概览统计数据")
    @GetMapping("/valuationMetrics/overview")
    public ResponseDTO<ValuationOverviewVO> valuationMetricsOverview() {
        Long userId = UserContext.getCurrentUserId();
        return ResponseDTO.success(stockValuationMetricsService.getOverview(userId));
    }

    @Operation(summary = "估值指标行业列表查询")
    @GetMapping("/valuationMetrics/industries")
    public ResponseDTO<List<String>> valuationMetricsIndustries() {
        return ResponseDTO.success(stockValuationMetricsService.getIndustries());
    }

    @Operation(summary = "估值指标详情查询")
    @GetMapping("/valuationMetrics/detail")
    public ResponseDTO<CalculatedValuationMetricsVO> valuationMetricsDetail(@RequestParam String stockCode) {
        return ResponseDTO.success(stockValuationMetricsService.detail(stockCode));
    }

    @Operation(summary = "成长性指标分页查询")
    @GetMapping("/growthMetrics/page")
    public ResponseDTO<Page<StockGrowthMetrics>> growthMetricsPage(
            @Valid @ParameterObject GrowthMetricsPageReqVO reqVO, @ParameterObject Pageable pageable) {
        return ResponseDTO.success(stockGrowthMetricsService.pageQuery(reqVO, pageable));
    }

}
