package com.brotherc.aquant.model.vo.fundflow;

import lombok.Data;

import java.util.List;

/**
 * 资金流动网络全景 VO
 */
@Data
public class FundFlowGraphVO {

    /**
     * 节点列表
     */
    private List<FundFlowGraphNodeVO> nodes;

    /**
     * 流向边列表
     */
    private List<FundFlowGraphLinkVO> links;

}
