package com.brotherc.aquant.industry.controller;

import com.brotherc.aquant.common.model.dto.ResponseDTO;
import com.brotherc.aquant.industry.model.vo.StockIndustryBoardKVO;
import com.brotherc.aquant.industry.model.vo.StockIndustryBoardPageReqVO;
import com.brotherc.aquant.industry.model.vo.StockIndustryBoardVO;
import com.brotherc.aquant.industry.model.vo.IndustryRiseAnalysisVO;
import com.brotherc.aquant.industry.service.StockIndustryBoardAnalysisService;
import com.brotherc.aquant.industry.service.StockIndustryBoardHistoryService;
import com.brotherc.aquant.stock.service.StockClusterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@Validated
@Tag(name = "板块数据")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockIndustryBoard")
public class StockIndustryBoardController {

    private final StockClusterService stockClusterService;
    private final StockIndustryBoardHistoryService stockIndustryBoardHistoryService;
    private final StockIndustryBoardAnalysisService stockIndustryBoardAnalysisService;

    @Operation(summary = "分页查询板块数据")
    @GetMapping("/page")
    public ResponseDTO<Page<StockIndustryBoardVO>> page(@ParameterObject StockIndustryBoardPageReqVO reqVO, @ParameterObject Pageable pageable) {
        return ResponseDTO.success(stockClusterService.stockIndustryBoardPage(reqVO, pageable));
    }

    @Operation(summary = "获取板块K线数据")
    @GetMapping("/history/kline")
    public ResponseDTO<List<StockIndustryBoardKVO>> kline(
            @Parameter(description = "板块代码") @RequestParam String boardCode,
            @Parameter(description = "频率: 1d, 1w, 1M, 1Q, 1Y") @RequestParam(required = false, defaultValue = "1d") String frequency) {
        return ResponseDTO.success(stockIndustryBoardHistoryService.getHistory(boardCode, frequency));
    }

    @Operation(summary = "获取行业涨幅排名分析数据")
    @GetMapping("/analysis")
    public ResponseDTO<List<IndustryRiseAnalysisVO>> analysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "20") Integer rankLimit) {
        return ResponseDTO.success(stockIndustryBoardAnalysisService.analysis(startDate, endDate, rankLimit));
    }

}
