package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockIndustryBoard;
import com.brotherc.aquant.model.vo.fundflow.FundFlowGraphLinkVO;
import com.brotherc.aquant.model.vo.fundflow.FundFlowGraphNodeVO;
import com.brotherc.aquant.model.vo.fundflow.FundFlowGraphVO;
import com.brotherc.aquant.model.vo.fundflow.FundFlowSummaryVO;
import com.brotherc.aquant.repository.StockIndustryBoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundFlowService {

    private final StockIndustryBoardRepository stockIndustryBoardRepository;

    public FundFlowGraphVO getGraphData() {
        List<StockIndustryBoard> boards = stockIndustryBoardRepository.findAll();
        FundFlowGraphVO vo = new FundFlowGraphVO();
        if (CollectionUtils.isEmpty(boards)) {
            vo.setNodes(Collections.emptyList());
            vo.setLinks(Collections.emptyList());
            return vo;
        }

        // 挑选交易活跃或净流入/流出较大的板块（前 30 个）
        List<StockIndustryBoard> activeBoards = boards.stream()
                .filter(b -> b.getNetInflow() != null || b.getTotalAmount() != null)
                .sorted(Comparator.comparing(
                        (StockIndustryBoard b) -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO
                ).reversed())
                .limit(30)
                .toList();

        if (activeBoards.isEmpty()) {
            activeBoards = boards.stream().limit(20).toList();
        }

        // 计算最大最小成交额，用于归一化计算气泡大小 symbolSize (35 ~ 85)
        BigDecimal maxAmount = activeBoards.stream()
                .map(StockIndustryBoard::getTotalAmount)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(new BigDecimal("1000000000"));

        BigDecimal minAmount = activeBoards.stream()
                .map(StockIndustryBoard::getTotalAmount)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal amountRange = maxAmount.subtract(minAmount);
        if (amountRange.compareTo(BigDecimal.ZERO) == 0) {
            amountRange = BigDecimal.ONE;
        }

        List<FundFlowGraphNodeVO> nodes = new ArrayList<>();
        List<FundFlowGraphLinkVO> links = new ArrayList<>();

        Map<String, String> boardNodeIdMap = new HashMap<>();

        // 1. 构建行业板块节点 (Board Nodes)
        for (StockIndustryBoard b : activeBoards) {
            String nodeId = "board_" + b.getSectorName();
            boardNodeIdMap.put(b.getSectorName(), nodeId);

            FundFlowGraphNodeVO node = new FundFlowGraphNodeVO();
            node.setId(nodeId);
            node.setName(b.getSectorName());
            node.setCategory("board");
            node.setChangePercent(b.getChangePercent());
            node.setNetInflow(b.getNetInflow());
            node.setTotalAmount(b.getTotalAmount());
            node.setCode(b.getLeadingStock());

            // 动态气泡尺寸计算 (适中比例以容纳所有板块)
            int symbolSize = 40;
            if (b.getTotalAmount() != null) {
                double ratio = b.getTotalAmount().subtract(minAmount)
                        .divide(amountRange, 4, RoundingMode.HALF_UP).doubleValue();
                symbolSize = (int) (35 + ratio * 35);
            }
            node.setSymbolSize(symbolSize);
            nodes.add(node);
        }

        // 3. 构建板块间的资金轮动流向连线 (Net Outflow Boards ➔ Net Inflow Boards)
        List<StockIndustryBoard> outflowBoards = activeBoards.stream()
                .filter(b -> b.getNetInflow() != null && b.getNetInflow().compareTo(BigDecimal.ZERO) < 0)
                .sorted(Comparator.comparing(StockIndustryBoard::getNetInflow)) // 净流出最多在前
                .toList();

        List<StockIndustryBoard> inflowBoards = activeBoards.stream()
                .filter(b -> b.getNetInflow() != null && b.getNetInflow().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(StockIndustryBoard::getNetInflow).reversed()) // 净流入最多在前
                .toList();

        int linkCount = Math.min(outflowBoards.size(), inflowBoards.size());
        for (int i = 0; i < linkCount; i++) {
            StockIndustryBoard outflow = outflowBoards.get(i);
            StockIndustryBoard inflow = inflowBoards.get(i);

            String sourceId = boardNodeIdMap.get(outflow.getSectorName());
            String targetId = boardNodeIdMap.get(inflow.getSectorName());

            if (sourceId != null && targetId != null && !sourceId.equals(targetId)) {
                FundFlowGraphLinkVO link = new FundFlowGraphLinkVO();
                link.setSource(sourceId);
                link.setTarget(targetId);
                BigDecimal flowValue = outflow.getNetInflow().abs().min(inflow.getNetInflow().abs());
                link.setValue(flowValue);
                link.setWeight(Math.min(10, Math.max(2, i + 1)));
                link.setLabel("板块博弈");
                links.add(link);
            }
        }

        vo.setNodes(nodes);
        vo.setLinks(links);
        return vo;
    }

    public FundFlowSummaryVO getSummaryData() {
        List<StockIndustryBoard> boards = stockIndustryBoardRepository.findAll();
        FundFlowSummaryVO summary = new FundFlowSummaryVO();

        if (CollectionUtils.isEmpty(boards)) {
            summary.setTotalMarketAmount(BigDecimal.ZERO);
            summary.setRiseCountTotal(0);
            summary.setFallCountTotal(0);
            summary.setTopInflowSectors(Collections.emptyList());
            summary.setTopOutflowSectors(Collections.emptyList());
            return summary;
        }

        // 大盘总成交额
        BigDecimal totalMarketAmount = boards.stream()
                .map(StockIndustryBoard::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 全市场上涨/下跌家数
        int riseTotal = boards.stream().mapToInt(b -> b.getRiseCount() != null ? b.getRiseCount() : 0).sum();
        int fallTotal = boards.stream().mapToInt(b -> b.getFallCount() != null ? b.getFallCount() : 0).sum();

        // 净流入最高的板块
        List<StockIndustryBoard> sortedByInflow = boards.stream()
                .filter(b -> b.getNetInflow() != null)
                .sorted(Comparator.comparing(StockIndustryBoard::getNetInflow).reversed())
                .toList();

        if (!sortedByInflow.isEmpty()) {
            StockIndustryBoard topInflow = sortedByInflow.get(0);
            summary.setTopInflowSector(topInflow.getSectorName());
            summary.setTopInflowAmount(topInflow.getNetInflow());
        }

        if (!sortedByInflow.isEmpty()) {
            StockIndustryBoard topOutflow = sortedByInflow.get(sortedByInflow.size() - 1);
            summary.setTopOutflowSector(topOutflow.getSectorName());
            summary.setTopOutflowAmount(topOutflow.getNetInflow());
        }

        summary.setTotalMarketAmount(totalMarketAmount);
        summary.setRiseCountTotal(riseTotal);
        summary.setFallCountTotal(fallTotal);

        // Top 5 净流入板块
        List<FundFlowGraphNodeVO> topInflowNodes = sortedByInflow.stream()
                .limit(5)
                .map(this::toNodeVO)
                .toList();

        // Top 5 净流出板块
        List<FundFlowGraphNodeVO> topOutflowNodes = boards.stream()
                .filter(b -> b.getNetInflow() != null)
                .sorted(Comparator.comparing(StockIndustryBoard::getNetInflow))
                .limit(5)
                .map(this::toNodeVO)
                .toList();

        summary.setTopInflowSectors(topInflowNodes);
        summary.setTopOutflowSectors(topOutflowNodes);

        return summary;
    }

    private FundFlowGraphNodeVO toNodeVO(StockIndustryBoard b) {
        FundFlowGraphNodeVO vo = new FundFlowGraphNodeVO();
        vo.setId("board_" + b.getSectorName());
        vo.setName(b.getSectorName());
        vo.setCategory("board");
        vo.setChangePercent(b.getChangePercent());
        vo.setNetInflow(b.getNetInflow());
        vo.setTotalAmount(b.getTotalAmount());
        vo.setCode(b.getLeadingStock());
        return vo;
    }

}
