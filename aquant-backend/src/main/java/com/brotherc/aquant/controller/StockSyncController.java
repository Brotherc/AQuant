package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.service.StockSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "股票数据同步")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockSync")
public class StockSyncController {

    private final StockSyncService stockSyncService;

    @Operation(summary = "获取最新同步时间【股票行情数据】")
    @GetMapping("/stockDailyLatest")
    public ResponseDTO<String> getStockDailyLatest() {
        return ResponseDTO.success(stockSyncService.getStockDailyLatest());
    }

    @Operation(summary = "获取最新同步时间【股票板块行情数据】")
    @GetMapping("/stockBoardIndustryLatest")
    public ResponseDTO<String> getStockBoardIndustryLatest() {
        return ResponseDTO.success(stockSyncService.getStockBoardIndustryLatest());
    }

    @Operation(summary = "股票分红")
    @GetMapping("/stockDividend")
    public ResponseDTO<Void> stockDividend() {
        stockSyncService.stockDividend();
        return ResponseDTO.success();
    }

    @Operation(summary = "杜邦分析_成长性_估值")
    @GetMapping("/stock/dupont/growth/valuation")
    public ResponseDTO<Void> stockDupontGrowthValuation(@RequestParam Integer count) {
        stockSyncService.stockDupontGrowthValuation(count);
        return ResponseDTO.success();
    }

    @Operation(summary = "股票板块行情历史")
    @GetMapping("/stockIndustryBoardHistory")
    public ResponseDTO<Void> stockIndustryBoardHistory(
            @RequestParam String boardName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        stockSyncService.stockIndustryBoardHistory(boardName, startDate, endDate);
        return ResponseDTO.success();
    }

}
