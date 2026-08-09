package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.fundflow.FundFlowGraphVO;
import com.brotherc.aquant.model.vo.fundflow.FundFlowSummaryVO;
import com.brotherc.aquant.service.FundFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "资金流向")
@RestController
@RequiredArgsConstructor
@RequestMapping("/fundFlow")
public class FundFlowController {

    private final FundFlowService fundFlowService;

    @Operation(summary = "查询资金流动网络关系图数据")
    @GetMapping("/graph")
    public ResponseDTO<FundFlowGraphVO> getGraphData() {
        return ResponseDTO.success(fundFlowService.getGraphData());
    }

    @Operation(summary = "查询大盘资金流动汇总数据")
    @GetMapping("/summary")
    public ResponseDTO<FundFlowSummaryVO> getSummaryData() {
        return ResponseDTO.success(fundFlowService.getSummaryData());
    }

}
