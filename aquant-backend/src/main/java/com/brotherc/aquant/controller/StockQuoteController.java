package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.stockquote.StockQuotePageReqVO;
import com.brotherc.aquant.model.vo.stockquote.StockQuoteVO;
import com.brotherc.aquant.service.StockClusterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "股票数据")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockQuote")
public class StockQuoteController {

    private final StockClusterService stockClusterService;

    @Operation(summary = "分页查询股票数据")
    @GetMapping("/page")
    public ResponseDTO<Page<StockQuoteVO>> page(StockQuotePageReqVO reqVO, Pageable pageable) {
        return ResponseDTO.success(stockClusterService.stockQuotePage(reqVO, pageable));
    }

}
