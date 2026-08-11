package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardKVO;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardPageReqVO;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardVO;
import com.brotherc.aquant.service.industry.StockIndustryBoardHistoryService;
import com.brotherc.aquant.service.stock.StockClusterService;
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

import java.util.List;

@Validated
@Tag(name = "板块数据")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockIndustryBoard")
public class StockIndustryBoardController {

    private final StockClusterService stockClusterService;
    private final StockIndustryBoardHistoryService stockIndustryBoardHistoryService;

    @Operation(summary = "分页查询板块数据")
    @GetMapping("/page")
    public ResponseDTO<Page<StockIndustryBoardVO>> page(@ParameterObject StockIndustryBoardPageReqVO reqVO, @ParameterObject Pageable pageable) {
        return ResponseDTO.success(stockClusterService.stockIndustryBoardPage(reqVO, pageable));
    }

    @Operation(summary = "获取板块K线数据")
    @GetMapping("/history/kline")
    public ResponseDTO<List<StockIndustryBoardKVO>> kline(
            @Parameter(description = "板块代码") @RequestParam String boardCode,
            @Parameter(description = "频率: 1d, 1w, 1M, 1Q, 1Y") @RequestParam(required = false, defaultValue = "1d") String frequency) {
        return ResponseDTO.success(stockIndustryBoardHistoryService.getHistory(boardCode, frequency));
    }

}
