package com.brotherc.aquant.index.controller;

import com.brotherc.aquant.common.model.dto.ResponseDTO;
import com.brotherc.aquant.index.model.vo.FundFlowGraphVO;
import com.brotherc.aquant.index.model.vo.FundFlowSummaryVO;
import com.brotherc.aquant.index.model.vo.MarketSentimentVO;
import com.brotherc.aquant.index.service.StockMarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "股票大盘数据")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockMarket")
public class StockMarketController {

    private final StockMarketService stockMarketService;

    @Operation(summary = "查询市场涨跌分布")
    @GetMapping("/current")
    public ResponseDTO<MarketSentimentVO> getMarketSentiment() {
        return ResponseDTO.success(stockMarketService.getMarketSentiment());
    }

    @Operation(summary = "查询资金流动网络关系图数据")
    @GetMapping("/fundFlow/graph")
    public ResponseDTO<FundFlowGraphVO> getGraphData() {
        return ResponseDTO.success(stockMarketService.getGraphData());
    }

    @Operation(summary = "查询大盘资金流动汇总数据")
    @GetMapping("/fundFlow/summary")
    public ResponseDTO<FundFlowSummaryVO> getSummaryData() {
        return ResponseDTO.success(stockMarketService.getSummaryData());
    }

}
