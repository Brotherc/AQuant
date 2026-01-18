package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendStatPageReqVO;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendStatVO;
import com.brotherc.aquant.service.StockDividendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "股票分红")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockDividend")
public class StockDividendController {

    private final StockDividendService stockDividendService;

    @Operation(summary = "分页查询股票分红数据")
    @GetMapping("/page")
    public ResponseDTO<Page<StockDividendStatVO>> page(
            @ParameterObject StockDividendStatPageReqVO reqVO, @ParameterObject Pageable pageable) {
        return ResponseDTO.success(stockDividendService.pageDividendStats(reqVO, pageable));
    }

}
