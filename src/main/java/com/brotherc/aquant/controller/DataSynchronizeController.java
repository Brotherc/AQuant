package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.service.DataSynchronizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dataSynchronize")
public class DataSynchronizeController {

    private final DataSynchronizeService dataSynchronizeService;

    @GetMapping("/stockQuote")
    public ResponseDTO<Void> stockQuote() {
        dataSynchronizeService.stockQuote();
        return ResponseDTO.success();
    }

    @GetMapping("/stock/dupont/growth/valuation")
    public ResponseDTO<Void> stockDupontGrowthValuation(@RequestParam Integer count) {
        dataSynchronizeService.stockDupontGrowthValuation(count);
        return ResponseDTO.success();
    }

}
