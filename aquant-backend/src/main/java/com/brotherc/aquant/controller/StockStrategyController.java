package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.strategy.DualMAReqVO;
import com.brotherc.aquant.model.vo.strategy.MomentumReqVO;
import com.brotherc.aquant.model.vo.strategy.MomentumBacktestReqVO;
import com.brotherc.aquant.model.vo.strategy.StockTradeSignalVO;
import com.brotherc.aquant.model.vo.strategy.StockTradeBacktestVO;
import com.brotherc.aquant.service.StockStrategyService;
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
    public ResponseDTO<Page<StockTradeBacktestVO>> dualMABacktest(
            com.brotherc.aquant.model.vo.strategy.DualMABacktestReqVO reqVO, Pageable pageable) {
        return ResponseDTO.success(stockStrategyService.dualMABacktest(reqVO, pageable));
    }

    @Operation(summary = "动量策略")
    @GetMapping("/momentum")
    public ResponseDTO<Page<StockTradeSignalVO>> momentum(MomentumReqVO reqVO, Pageable pageable) {
        return ResponseDTO.success(stockStrategyService.momentum(reqVO, pageable));
    }

    @Operation(summary = "动量策略回测")
    @GetMapping("/momentumBacktest")
    public ResponseDTO<Page<StockTradeBacktestVO>> momentumBacktest(
            MomentumBacktestReqVO reqVO, Pageable pageable) {
        return ResponseDTO.success(stockStrategyService.momentumBacktest(reqVO, pageable));
    }

}
