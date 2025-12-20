package com.brotherc.aquant.controller;

import com.brotherc.aquant.entity.StockDupontAnalysis;
import com.brotherc.aquant.entity.StockValuationMetrics;
import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.stockindicator.DupontAnalysisPageReqVO;
import com.brotherc.aquant.model.vo.stockindicator.ValuationMetricsPageReqVO;
import com.brotherc.aquant.service.StockDupontAnalysisService;
import com.brotherc.aquant.service.StockValuationMetricsService;
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
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "股票指标")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockIndicator")
public class StockIndicatorController {

    private final StockDupontAnalysisService stockDupontAnalysisService;
    private final StockValuationMetricsService stockValuationMetricsService;

    @Operation(summary = "杜邦分析指标分页查询")
    @GetMapping("/dupontAnalysis/page")
    public ResponseDTO<Page<StockDupontAnalysis>> dupontAnalysisPage(
            @Valid @ParameterObject DupontAnalysisPageReqVO reqVO, @ParameterObject Pageable pageable
    ) {
        return ResponseDTO.success(stockDupontAnalysisService.pageQuery(reqVO, pageable));
    }

    @Operation(summary = "估值指标分页查询")
    @GetMapping("/valuationMetrics/page")
    public ResponseDTO<Page<StockValuationMetrics>> valuationMetricsPage(
            @Valid @ParameterObject ValuationMetricsPageReqVO reqVO, @ParameterObject Pageable pageable
    ) {
        return ResponseDTO.success(stockValuationMetricsService.pageQuery(reqVO, pageable));
    }

}
