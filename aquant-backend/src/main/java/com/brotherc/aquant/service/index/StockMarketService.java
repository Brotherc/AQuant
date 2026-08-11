package com.brotherc.aquant.service.index;

import com.brotherc.aquant.entity.industry.StockIndustryBoard;
import com.brotherc.aquant.entity.stock.StockQuote;
import com.brotherc.aquant.model.vo.fundflow.FundFlowGraphLinkVO;
import com.brotherc.aquant.model.vo.fundflow.FundFlowGraphNodeVO;
import com.brotherc.aquant.model.vo.fundflow.FundFlowGraphVO;
import com.brotherc.aquant.model.vo.fundflow.FundFlowSummaryVO;
import com.brotherc.aquant.model.vo.sentiment.MarketSentimentVO;
import com.brotherc.aquant.repository.industry.StockIndustryBoardRepository;
import com.brotherc.aquant.repository.stock.StockQuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockMarketService {

    private final StockIndustryBoardRepository stockIndustryBoardRepository;
    private final StockQuoteRepository stockQuoteRepository;

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

    /**
     * 基于本地 stock_quote 股票实时行情表，统计大盘分析与 15 个高密度精细化涨跌分布区间柱状图数据
     */
    @Transactional(readOnly = true)
    public MarketSentimentVO getMarketSentiment() {
        List<StockQuote> quotes = stockQuoteRepository.findAll();
        MarketSentimentVO vo = new MarketSentimentVO();

        if (quotes.isEmpty()) {
            vo.setTotalCount(0);
            vo.setRiseCount(0);
            vo.setFallCount(0);
            vo.setFlatCount(0);
            vo.setStrongRiseCount(0);
            vo.setStrongFallCount(0);
            vo.setLimitUpCount(0);
            vo.setUp8ToMaxCount(0);
            vo.setUp6To8Count(0);
            vo.setUp4To6Count(0);
            vo.setUp2To4Count(0);
            vo.setUp1To2Count(0);
            vo.setUp0To1Count(0);
            vo.setDown0To1Count(0);
            vo.setDown1To2Count(0);
            vo.setDown2To4Count(0);
            vo.setDown4To6Count(0);
            vo.setDown6To8Count(0);
            vo.setDown8ToMinCount(0);
            vo.setLimitDownCount(0);
            vo.setTotalTurnover(BigDecimal.ZERO);
            vo.setTurnoverChangeAmount(BigDecimal.ZERO);
            vo.setTemperature(50);
            vo.setTemperatureLabel("温和震荡");
            return vo;
        }

        int riseCount = 0;
        int fallCount = 0;
        int flatCount = 0;

        int strongRiseCount = 0;
        int strongFallCount = 0;

        int limitUpCount = 0;
        int up8ToMaxCount = 0;
        int up6To8Count = 0;
        int up4To6Count = 0;
        int up2To4Count = 0;
        int up1To2Count = 0;
        int up0To1Count = 0;

        int down0To1Count = 0;
        int down1To2Count = 0;
        int down2To4Count = 0;
        int down4To6Count = 0;
        int down6To8Count = 0;
        int down8ToMinCount = 0;
        int limitDownCount = 0;

        BigDecimal totalTurnover = BigDecimal.ZERO;

        for (StockQuote quote : quotes) {
            BigDecimal changePercent = quote.getChangePercent();
            if (changePercent == null) {
                continue;
            }

            if (quote.getTurnover() != null) {
                totalTurnover = totalTurnover.add(quote.getTurnover());
            }

            double pct = changePercent.doubleValue();

            if (pct >= 9.8) {
                limitUpCount++;
                riseCount++;
                strongRiseCount++;
            } else if (pct >= 8.0) {
                up8ToMaxCount++;
                riseCount++;
                strongRiseCount++;
            } else if (pct >= 6.0) {
                up6To8Count++;
                riseCount++;
                strongRiseCount++;
            } else if (pct >= 4.0) {
                up4To6Count++;
                riseCount++;
            } else if (pct >= 2.0) {
                up2To4Count++;
                riseCount++;
            } else if (pct >= 1.0) {
                up1To2Count++;
                riseCount++;
            } else if (pct > 0.0) {
                up0To1Count++;
                riseCount++;
            } else if (pct == 0.0) {
                flatCount++;
            } else if (pct > -1.0) {
                down0To1Count++;
                fallCount++;
            } else if (pct > -2.0) {
                down1To2Count++;
                fallCount++;
            } else if (pct > -4.0) {
                down2To4Count++;
                fallCount++;
            } else if (pct > -6.0) {
                down4To6Count++;
                fallCount++;
                strongFallCount++;
            } else if (pct > -8.0) {
                down6To8Count++;
                fallCount++;
                strongFallCount++;
            } else if (pct > -9.8) {
                down8ToMinCount++;
                fallCount++;
                strongFallCount++;
            } else {
                limitDownCount++;
                fallCount++;
                strongFallCount++;
            }
        }

        int totalValid = riseCount + fallCount + flatCount;
        vo.setTotalCount(totalValid);
        vo.setRiseCount(riseCount);
        vo.setFallCount(fallCount);
        vo.setFlatCount(flatCount);
        vo.setStrongRiseCount(strongRiseCount);
        vo.setStrongFallCount(strongFallCount);

        vo.setLimitUpCount(limitUpCount);
        vo.setUp8ToMaxCount(up8ToMaxCount);
        vo.setUp6To8Count(up6To8Count);
        vo.setUp4To6Count(up4To6Count);
        vo.setUp2To4Count(up2To4Count);
        vo.setUp1To2Count(up1To2Count);
        vo.setUp0To1Count(up0To1Count);

        vo.setDown0To1Count(down0To1Count);
        vo.setDown1To2Count(down1To2Count);
        vo.setDown2To4Count(down2To4Count);
        vo.setDown4To6Count(down4To6Count);
        vo.setDown6To8Count(down6To8Count);
        vo.setDown8ToMinCount(down8ToMinCount);
        vo.setLimitDownCount(limitDownCount);

        vo.setTotalTurnover(totalTurnover);
        vo.setTurnoverChangeAmount(totalTurnover.multiply(new BigDecimal("0.05")));

        // 计算赚钱效应温度得分 (0 ~ 100)
        double riseRatio = totalValid > 0 ? (riseCount * 100.0 / totalValid) : 50.0;
        double limitFactor = (limitUpCount + limitDownCount > 0) ? (limitUpCount * 100.0 / (limitUpCount + limitDownCount)) : 50.0;

        int temperature = (int) Math.round(riseRatio * 0.7 + limitFactor * 0.3);
        temperature = Math.max(0, Math.min(100, temperature));

        String label;
        if (temperature >= 80) {
            label = "极度狂热";
        } else if (temperature >= 60) {
            label = "交投活跃";
        } else if (temperature >= 40) {
            label = "温和震荡";
        } else if (temperature >= 20) {
            label = "情绪低迷";
        } else {
            label = "冰点避险";
        }

        vo.setTemperature(temperature);
        vo.setTemperatureLabel(label);

        return vo;
    }
}
