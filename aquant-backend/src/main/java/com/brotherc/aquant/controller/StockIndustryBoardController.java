package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardPageReqVO;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardVO;
import com.brotherc.aquant.service.StockClusterService;
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
@Tag(name = "板块数据")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockIndustryBoard")
public class StockIndustryBoardController {

    private final StockClusterService stockClusterService;

    @Operation(summary = "分页查询板块数据")
    @GetMapping("/page")
    public ResponseDTO<Page<StockIndustryBoardVO>> page(@ParameterObject StockIndustryBoardPageReqVO reqVO, @ParameterObject Pageable pageable) {
        return ResponseDTO.success(stockClusterService.stockIndustryBoardPage(reqVO, pageable));
    }

}
