package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.index.StockIndexCardVO;
import com.brotherc.aquant.service.StockIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@Tag(name = "股票指数")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockIndex")
public class StockIndexController {

    private final StockIndexService stockIndexService;

    @Operation(summary = "查询首页核心大盘指数卡片列表")
    @GetMapping("/cards")
    public ResponseDTO<List<StockIndexCardVO>> getCoreIndexCards() {
        return ResponseDTO.success(stockIndexService.getCoreIndexCards());
    }

}
