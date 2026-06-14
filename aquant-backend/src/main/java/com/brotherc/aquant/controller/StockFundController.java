package com.brotherc.aquant.controller;

import com.brotherc.aquant.entity.StockFundNetValue;
import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.stockfund.StockFundInfoPageReqVO;
import com.brotherc.aquant.model.vo.stockfund.StockFundInfoVO;
import com.brotherc.aquant.service.StockFundInfoService;
import com.brotherc.aquant.service.StockFundNetValueService;
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

    @Operation(summary = "更新基金购买起点等附加信息")
    @GetMapping("/updateAdditionalInfo")
    public ResponseDTO<Void> updateAdditionalInfo() {
        stockFundInfoService.updateAdditionalInfo();
        return ResponseDTO.success();
    }

}
