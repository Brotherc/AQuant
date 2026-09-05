package com.brotherc.aquant.strategy.controller;

import com.brotherc.aquant.common.model.dto.ResponseDTO;
import com.brotherc.aquant.strategy.model.vo.*;
import com.brotherc.aquant.strategy.service.StockStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "股票策略")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockStrategy")
public class StockStrategyController {

    private final StockStrategyService stockStrategyService;

    @Operation(summary = "双均线策略")
    @GetMapping("/dualMA")
    public ResponseDTO<Page<StockTradeSignalVO>> dualMA(DualMAReqVO reqVO, Pageable pageable) {
        return ResponseDTO.success(stockStrategyService.dualMA(reqVO, pageable));
    }

    @Operation(summary = "双均线策略回测")
    @GetMapping("/dualMABacktest")
    public ResponseDTO<Page<StockTradeBacktestVO>> dualMABacktest(DualMABacktestReqVO reqVO, Pageable pageable) {
        return ResponseDTO.success(stockStrategyService.dualMABacktest(reqVO, pageable));
    }

    @Operation(summary = "动量策略")
    @GetMapping("/momentum")
    public ResponseDTO<Page<StockTradeSignalVO>> momentum(MomentumReqVO reqVO, Pageable pageable) {
        return ResponseDTO.success(stockStrategyService.momentum(reqVO, pageable));
    }

    @Operation(summary = "动量策略回测")
    @GetMapping("/momentumBacktest")
    public ResponseDTO<Page<StockTradeBacktestVO>> momentumBacktest(MomentumBacktestReqVO reqVO, Pageable pageable) {
        return ResponseDTO.success(stockStrategyService.momentumBacktest(reqVO, pageable));
    }

    @Operation(summary = "MACD策略")
    @GetMapping("/macd")
    public ResponseDTO<Page<StockTradeSignalVO>> macd(MacdReqVO reqVO, Pageable pageable) {
        return ResponseDTO.success(stockStrategyService.macd(reqVO, pageable));
    }

    @Operation(summary = "MACD策略回测")
    @GetMapping("/macdBacktest")
    public ResponseDTO<Page<StockTradeBacktestVO>> macdBacktest(MacdBacktestReqVO reqVO, Pageable pageable) {
        return ResponseDTO.success(stockStrategyService.macdBacktest(reqVO, pageable));
    }

    @Operation(summary = "网格交易策略")
    @GetMapping("/grid")
    public ResponseDTO<Page<StockTradeSignalVO>> grid(GridReqVO reqVO, Pageable pageable) {
        return ResponseDTO.success(stockStrategyService.grid(reqVO, pageable));
    }

    @Operation(summary = "网格交易策略回测")
    @GetMapping("/gridBacktest")
    public ResponseDTO<Page<StockTradeBacktestVO>> gridBacktest(GridBacktestReqVO reqVO, Pageable pageable) {
        return ResponseDTO.success(stockStrategyService.gridBacktest(reqVO, pageable));
    }

}
