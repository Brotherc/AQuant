package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.service.StockSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Operation(summary = "获取最新同步时间【基金数据】")
    @GetMapping("/stockFundInfoLatest")
    public ResponseDTO<String> getStockFundInfoLatest() {
        return ResponseDTO.success(stockSyncService.getStockFundInfoLatest());
    }

}
