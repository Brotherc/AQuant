package com.brotherc.aquant.dividend.controller;

import com.brotherc.aquant.common.model.dto.ResponseDTO;
import com.brotherc.aquant.dividend.model.vo.DividendOverviewVO;
import com.brotherc.aquant.dividend.model.vo.StockDividendDetailReqVO;
import com.brotherc.aquant.dividend.model.vo.StockDividendDetailVO;
import com.brotherc.aquant.dividend.model.vo.StockDividendStatPageReqVO;
import com.brotherc.aquant.dividend.model.vo.StockDividendStatVO;
import com.brotherc.aquant.dividend.service.StockDividendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@Tag(name = "股票分红")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockDividend")
public class StockDividendController {

    private final StockDividendService stockDividendService;

    @Operation(summary = "获取分红概览看板数据")
    @GetMapping("/overview")
    public ResponseDTO<DividendOverviewVO> getOverview(
            @RequestParam(value = "watchlistGroupId", required = false) Long watchlistGroupId) {
        return ResponseDTO.success(stockDividendService.getOverview(watchlistGroupId));
    }

    @Operation(summary = "分页查询股票分红数据")
    @GetMapping("/page")
    public ResponseDTO<Page<StockDividendStatVO>> page(
            @ParameterObject StockDividendStatPageReqVO reqVO, @ParameterObject Pageable pageable) {
        return ResponseDTO.success(stockDividendService.pageDividendStats(reqVO, pageable));
    }

    @Operation(summary = "查询股票分红详情")
    @GetMapping("/getDetailByCode")
    public ResponseDTO<List<StockDividendDetailVO>> getDetailByCode(@Valid @ParameterObject StockDividendDetailReqVO reqVO) {
        return ResponseDTO.success(stockDividendService.getDetailByCode(reqVO));
    }

}
