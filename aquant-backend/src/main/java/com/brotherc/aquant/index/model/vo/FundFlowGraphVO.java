package com.brotherc.aquant.index.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "资金流动网络全景VO")
public class FundFlowGraphVO {

    @Schema(description = "节点列表")
    private List<FundFlowGraphNodeVO> nodes = new ArrayList<>();

    @Schema(description = "流向边列表")
    private List<FundFlowGraphLinkVO> links = new ArrayList<>();

}
