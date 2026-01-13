package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.entity.StockQuoteHistory;
import com.brotherc.aquant.service.StockQuoteHistoryService;
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
@Tag(name = "股票历史行情")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockQuote/history")
public class StockQuoteHistoryController {

    private final StockQuoteHistoryService stockQuoteHistoryService;

    @Operation(summary = "获取个股K线数据")
    @GetMapping("/kline")
    public ResponseDTO<List<StockQuoteHistory>> kline(
            @Parameter(description = "股票代码") @RequestParam String code,
            @Parameter(description = "频率: 1d, 1w, 1M, 1Q, 1Y") @RequestParam(required = false, defaultValue = "1d") String frequency) {
        return ResponseDTO.success(stockQuoteHistoryService.getHistory(code, frequency));
    }

}
