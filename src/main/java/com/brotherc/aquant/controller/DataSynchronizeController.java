package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.service.DataSynchronizeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "数据同步")
@RestController
@RequiredArgsConstructor
@RequestMapping("/dataSynchronize")
public class DataSynchronizeController {

    private final DataSynchronizeService dataSynchronizeService;

    @Operation(summary = "股票")
    @GetMapping("/stockQuote")
    public ResponseDTO<Void> stockQuote() {
        dataSynchronizeService.stockQuote();
        return ResponseDTO.success();
    }

    @Operation(summary = "股票分红")
    @GetMapping("/stockDividend")
    public ResponseDTO<Void> stockDividend() {
        dataSynchronizeService.stockDividend();
        return ResponseDTO.success();
    }

    @Operation(summary = "杜邦分析_成长性_估值")
    @GetMapping("/stock/dupont/growth/valuation")
    public ResponseDTO<Void> stockDupontGrowthValuation(@RequestParam Integer count) {
        dataSynchronizeService.stockDupontGrowthValuation(count);
        return ResponseDTO.success();
    }

}
