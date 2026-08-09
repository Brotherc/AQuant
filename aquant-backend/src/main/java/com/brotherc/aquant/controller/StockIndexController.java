package com.brotherc.aquant.controller;

import com.brotherc.aquant.entity.StockQuoteHistory;
import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.index.StockIndexCardVO;
import com.brotherc.aquant.service.StockIndexService;
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
@Tag(name = "股票指数")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockIndex")
public class StockIndexController {

    private final StockIndexService stockIndexService;

    @Operation(summary = "查询首页核心大盘指数卡片列表")
    @GetMapping("/cards")
    public ResponseDTO<List<StockIndexCardVO>> getCoreIndexCards() {
        return ResponseDTO.success(stockIndexService.getCoreIndexCards());
    }

    @Operation(summary = "获取大盘指数K线行情数据")
    @GetMapping("/history/kline")
    public ResponseDTO<List<StockQuoteHistory>> getIndexKline(
            @Parameter(description = "指数代码") @RequestParam String code,
            @Parameter(description = "频率: 1d, 1w, 1M, 1Q, 1Y") @RequestParam(required = false, defaultValue = "1d") String frequency) {
        return ResponseDTO.success(stockIndexService.getIndexKlineHistory(code, frequency));
    }

}
