package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.dto.stockselect.StockSelectionResult;
import com.brotherc.aquant.service.StockSelectionExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stockSelection")
public class StockSelectionController {

    private final StockSelectionExecutor selectionExecutor;

    /**
     * 获取股票评分
     */
    @GetMapping("/stocks/score")
    public ResponseDTO<StockSelectionResult> getStockScore(@RequestParam String stockCode) {
        List<StockSelectionResult> results = selectionExecutor.executeStockSelection(Collections.singletonList(stockCode));

        if (CollectionUtils.isEmpty(results)) {
            return ResponseDTO.success();
        }
        return ResponseDTO.success(results.get(0));
    }

    /**
     * 获取推荐股票列表
     */
    @PostMapping("/stocks/recommendations")
    public ResponseDTO<List<StockSelectionResult>> getRecommendations(
            @RequestBody(required = false) List<String> stockCodes,
            @RequestParam(defaultValue = "10") int topN,
            @RequestParam(defaultValue = "60.0") BigDecimal minScore) throws JsonProcessingException {
        return ResponseDTO.success(selectionExecutor.getRecommendedStocks(stockCodes, topN, minScore));
    }

    /**
     * 批量评估股票
     */
    @PostMapping("/stocks/batch-evaluate")
    public ResponseDTO<List<StockSelectionResult>> batchEvaluate(@RequestBody List<String> stockCodes) {
        return ResponseDTO.success(selectionExecutor.executeStockSelection(stockCodes));
    }

}
