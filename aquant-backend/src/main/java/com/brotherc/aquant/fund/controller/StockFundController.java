package com.brotherc.aquant.fund.controller;

import com.brotherc.aquant.fund.entity.StockFundNetValue;
import com.brotherc.aquant.common.model.dto.ResponseDTO;
import com.brotherc.aquant.fund.model.vo.StockFundInfoPageReqVO;
import com.brotherc.aquant.fund.model.vo.StockFundInfoVO;
import com.brotherc.aquant.fund.model.vo.StockFundPurchaseLimitVO;
import com.brotherc.aquant.fund.entity.StockFundPortfolioHolding;
import com.brotherc.aquant.fund.service.StockFundPortfolioHoldingService;
import com.brotherc.aquant.fund.service.StockFundInfoService;
import com.brotherc.aquant.fund.service.StockFundNetValueService;
import com.brotherc.aquant.fund.service.StockFundPurchaseLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "股票基金数据")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockFund")
public class StockFundController {

    private final StockFundInfoService stockFundInfoService;
    private final StockFundNetValueService stockFundNetValueService;
    private final StockFundPortfolioHoldingService stockFundPortfolioHoldingService;
    private final StockFundPurchaseLimitService stockFundPurchaseLimitService;

    @Operation(summary = "分页查询基金基本信息")
    @GetMapping("/page")
    public ResponseDTO<Page<StockFundInfoVO>> page(
            @ParameterObject StockFundInfoPageReqVO reqVO, @ParameterObject Pageable pageable
    ) {
        return ResponseDTO.success(stockFundInfoService.getPage(reqVO, pageable));
    }

    @Operation(summary = "获取基金历史净值")
    @GetMapping("/history/netValue")
    public ResponseDTO<List<StockFundNetValue>> getFundNetValues(
            @RequestParam String fundCode) {
        return ResponseDTO.success(stockFundNetValueService.getFundNetValues(fundCode));
    }

    @Operation(summary = "获取基金最新持仓")
    @GetMapping("/portfolio/latest")
    public ResponseDTO<List<StockFundPortfolioHolding>> getLatestFundHoldings(
            @RequestParam String fundCode) {
        return ResponseDTO.success(stockFundPortfolioHoldingService.getLatestFundHoldings(fundCode));
    }

    @Operation(summary = "获取基金当前官方申购限制")
    @GetMapping("/purchaseLimits")
    public ResponseDTO<List<StockFundPurchaseLimitVO>> getPurchaseLimits(@RequestParam String fundCode) {
        return ResponseDTO.success(stockFundPurchaseLimitService.getCurrentLimits(fundCode));
    }

}
