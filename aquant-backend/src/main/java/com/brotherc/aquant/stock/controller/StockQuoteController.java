package com.brotherc.aquant.stock.controller;

import com.brotherc.aquant.stock.entity.StockMinuteBar;
import com.brotherc.aquant.stock.entity.StockQuoteHistory;
import com.brotherc.aquant.common.model.dto.ResponseDTO;
import com.brotherc.aquant.stock.model.vo.StockMinuteRealtimeVO;
import com.brotherc.aquant.stock.model.vo.StockOrderBookVO;
import com.brotherc.aquant.stock.model.vo.StockQuotePageReqVO;
import com.brotherc.aquant.stock.model.vo.StockQuoteVO;
import com.brotherc.aquant.stock.model.vo.StockTickTradeVO;
import com.brotherc.aquant.stock.service.StockClusterService;
import com.brotherc.aquant.stock.service.StockMinuteService;
import com.brotherc.aquant.stock.service.StockQuoteHistoryService;
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

import java.time.LocalDateTime;
import java.util.List;

@Validated
@Tag(name = "股票数据")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockQuote")
public class StockQuoteController {

    private final StockClusterService stockClusterService;
    private final StockQuoteHistoryService stockQuoteHistoryService;
    private final StockMinuteService stockMinuteService;

    @Operation(summary = "分页查询股票数据")
    @GetMapping("/page")
    public ResponseDTO<Page<StockQuoteVO>> page(@ParameterObject StockQuotePageReqVO reqVO, @ParameterObject Pageable pageable) {
        return ResponseDTO.success(stockClusterService.stockQuotePage(reqVO, pageable));
    }

    @Operation(summary = "获取个股K线数据")
    @GetMapping("/history/kline")
    public ResponseDTO<List<StockQuoteHistory>> kline(
            @Parameter(description = "股票代码") @RequestParam String code,
            @Parameter(description = "频率: 1d, 1w, 1M, 1Q, 1Y") @RequestParam(required = false, defaultValue = "1d") String frequency) {
        return ResponseDTO.success(stockQuoteHistoryService.getHistory(code, frequency));
    }

    @Operation(summary = "获取个股当日分时(实时)")
    @GetMapping("/minute/realtime")
    public ResponseDTO<StockMinuteRealtimeVO> minuteRealtime(
            @Parameter(description = "股票代码") @RequestParam String code) {
        return ResponseDTO.success(stockMinuteService.getRealtimeMinute(code));
    }

    @Operation(summary = "获取个股实时盘口(五档买卖盘+成交信息)")
    @GetMapping("/minute/orderbook")
    public ResponseDTO<StockOrderBookVO> orderBook(
            @Parameter(description = "股票代码") @RequestParam String code) {
        return ResponseDTO.success(stockMinuteService.getOrderBook(code));
    }

    @Operation(summary = "获取个股当日分笔成交明细(时间升序,已过滤集合竞价虚拟撮合)")
    @GetMapping("/minute/trades")
    public ResponseDTO<StockTickTradeVO> tickTrades(
            @Parameter(description = "股票代码") @RequestParam String code) {
        return ResponseDTO.success(stockMinuteService.getTickTrades(code));
    }

    @Operation(summary = "获取个股1分钟K线(近N个已收盘交易日,默认5;'1分'K线与'五日分时'共用)")
    @GetMapping("/minute/kline")
    public ResponseDTO<List<StockMinuteBar>> minuteKline(
            @Parameter(description = "股票代码") @RequestParam String code,
            @Parameter(description = "天数 1-8") @RequestParam(required = false, defaultValue = "5") Integer days) {
        return ResponseDTO.success(stockMinuteService.getMinuteKline(code, days, LocalDateTime.now()));
    }

}
