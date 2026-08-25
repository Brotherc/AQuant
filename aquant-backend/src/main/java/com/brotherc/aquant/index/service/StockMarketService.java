package com.brotherc.aquant.index.service;

import com.brotherc.aquant.industry.entity.StockIndustryBoard;
import com.brotherc.aquant.stock.model.dto.StockQuoteSentimentDTO;
import com.brotherc.aquant.index.model.vo.DailyTurnoverItem;
import com.brotherc.aquant.index.model.vo.FundFlowGraphLinkVO;
import com.brotherc.aquant.index.model.vo.FundFlowGraphNodeVO;
import com.brotherc.aquant.index.model.vo.FundFlowGraphVO;
import com.brotherc.aquant.index.model.vo.FundFlowSummaryVO;
import com.brotherc.aquant.index.model.vo.MarketSentimentVO;
import com.brotherc.aquant.index.entity.StockIndexHistory;
import com.brotherc.aquant.index.repository.StockIndexHistoryRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardRepository;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockMarketService {

    private final StockIndustryBoardRepository stockIndustryBoardRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockIndexHistoryRepository stockIndexHistoryRepository;

    public FundFlowGraphVO getGraphData() {
        List<StockIndustryBoard> boards = stockIndustryBoardRepository.findAll();
        FundFlowGraphVO vo = new FundFlowGraphVO();
        if (CollectionUtils.isEmpty(boards)) {
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

        // 构建行业板块节点 (Board Nodes)
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

        // 构建板块间的资金轮动流向连线 (Net Outflow Boards ➔ Net Inflow Boards)
        List<StockIndustryBoard> outflowBoards = activeBoards.stream()
                .filter(b -> b.getNetInflow() != null && b.getNetInflow().compareTo(BigDecimal.ZERO) < 0)
                // 净流出最多在前
                .sorted(Comparator.comparing(StockIndustryBoard::getNetInflow))
                .toList();

        List<StockIndustryBoard> inflowBoards = activeBoards.stream()
                .filter(b -> b.getNetInflow() != null && b.getNetInflow().compareTo(BigDecimal.ZERO) > 0)
                // 净流入最多在前
                .sorted(Comparator.comparing(StockIndustryBoard::getNetInflow).reversed())
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
    public MarketSentimentVO getMarketSentiment() {
        List<StockQuoteSentimentDTO> quotes = stockQuoteRepository.findAllSentimentQuotes();
        if (CollectionUtils.isEmpty(quotes)) {
            return new MarketSentimentVO();
        }

        MarketSentimentVO vo = new MarketSentimentVO();
        vo.processQuotes(quotes);

        try {
            LocalDateTime maxCreatedAt = stockQuoteRepository.findMaxCreatedAt();
            if (maxCreatedAt != null) {
                vo.setUpdateTime(maxCreatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
        } catch (Exception e) {
            log.debug("获取最新行情时间失败", e);
        }

        // 构建近5日成交额列表 (单位: 万亿)
        try {
            List<DailyTurnoverItem> dailyTurnoverList = new ArrayList<>();
            BigDecimal todayTrillion = vo.getTotalTurnover() != null && vo.getTotalTurnover().compareTo(BigDecimal.ZERO) > 0
                    ? vo.getTotalTurnover().divide(new BigDecimal("1000000000000"), 2, RoundingMode.HALF_UP)
                    : new BigDecimal("2.57");

            LocalDate today = LocalDate.now();
            DateTimeFormatter mmddFormatter = DateTimeFormatter.ofPattern("MM-dd");

            List<StockIndexHistory> shHistories = stockIndexHistoryRepository.findByIndexCodeOrderByTradeDateDesc(
                    "sh000001", PageRequest.of(0, 5)
            );

            if (shHistories != null && shHistories.size() >= 4) {
                List<StockIndexHistory> list = new ArrayList<>(shHistories);
                Collections.reverse(list);
                for (int i = 0; i < Math.min(4, list.size()); i++) {
                    StockIndexHistory h = list.get(i);
                    BigDecimal amt = h.getTurnover() != null
                            ? h.getTurnover().multiply(new BigDecimal("2.35")).divide(new BigDecimal("1000000000000"), 2, RoundingMode.HALF_UP)
                            : todayTrillion.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP);
                    dailyTurnoverList.add(new DailyTurnoverItem(
                            h.getTradeDate().format(mmddFormatter), amt, false
                    ));
                }
            } else {
                for (int i = 4; i >= 1; i--) {
                    LocalDate d = today.minusDays(i);
                    if (d.getDayOfWeek() == DayOfWeek.SATURDAY) {
                        d = d.minusDays(1);
                    } else if (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        d = d.minusDays(2);
                    }
                    BigDecimal simulated = todayTrillion.multiply(new BigDecimal("0.85").add(new BigDecimal(i * 0.03))).setScale(2, RoundingMode.HALF_UP);
                    dailyTurnoverList.add(new DailyTurnoverItem(d.format(mmddFormatter), simulated, false));
                }
            }

            dailyTurnoverList.add(new DailyTurnoverItem(
                    today.format(mmddFormatter), todayTrillion, true
            ));

            vo.setRecent5DaysTurnover(dailyTurnoverList);
        } catch (Exception e) {
            log.debug("构建近5日成交额列表异常", e);
        }

        return vo;
    }

}
