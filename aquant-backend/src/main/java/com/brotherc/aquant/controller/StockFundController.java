package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.stockfund.StockFundInfoPageReqVO;
import com.brotherc.aquant.model.vo.stockfund.StockFundInfoVO;
import com.brotherc.aquant.repository.StockFundInfoRepository;
import com.brotherc.aquant.service.StockFundInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "股票基金数据")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockFund")
public class StockFundController {

    private final StockFundInfoService stockFundInfoService;

    @Operation(summary = "分页查询基金基本信息")
    @GetMapping("/page")
    public ResponseDTO<Page<StockFundInfoVO>> page(
            @ParameterObject StockFundInfoPageReqVO reqVO, @ParameterObject Pageable pageable
    ) {
        return ResponseDTO.success(stockFundInfoService.getPage(reqVO, pageable));
    }

}
