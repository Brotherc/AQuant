package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.sentiment.MarketSentimentVO;
import com.brotherc.aquant.service.MarketSentimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "市场情绪")
@RestController
@RequiredArgsConstructor
@RequestMapping("/marketSentiment")
public class MarketSentimentController {

    private final MarketSentimentService marketSentimentService;

    @Operation(summary = "查询基于本地股票实时表统计的市场情绪与温度计")
    @GetMapping("/current")
    public ResponseDTO<MarketSentimentVO> getMarketSentiment() {
        return ResponseDTO.success(marketSentimentService.getMarketSentiment());
    }

}
