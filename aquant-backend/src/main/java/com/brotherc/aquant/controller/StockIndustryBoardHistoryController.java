package com.brotherc.aquant.controller;

import com.brotherc.aquant.entity.StockIndustryBoardHistory;
import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.service.StockIndustryBoardHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@Tag(name = "板块历史行情")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockIndustryBoard/history")
public class StockIndustryBoardHistoryController {

    private final StockIndustryBoardHistoryService stockIndustryBoardHistoryService;

    @Operation(summary = "获取板块K线数据")
    @GetMapping("/kline")
    public ResponseDTO<List<StockIndustryBoardHistory>> kline(
            @Parameter(description = "板块代码") @RequestParam String boardCode,
            @Parameter(description = "频率: 1d, 1w, 1M, 1Q, 1Y") @RequestParam(required = false, defaultValue = "1d") String frequency) {
        return ResponseDTO.success(stockIndustryBoardHistoryService.getHistory(boardCode, frequency));
    }
}
