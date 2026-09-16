package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.industry.entity.StockBoardConstituentEm;
import com.brotherc.aquant.industry.entity.StockIndustryBoardEm;
import com.brotherc.aquant.industry.entity.StockIndustryBoardHistoryEm;
import com.brotherc.aquant.industry.repository.StockBoardConstituentEmRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardEmRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardHistoryEmRepository;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardDetail;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardKline;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 东方财富行业板块数据的落库服务。独立于同步编排服务（StockIndustryBoardEmSyncService）：
 * 落库方法若与调用方在同一个 Bean 里，自调用会绕过 Spring 事务代理，@Transactional 静默失效，
 * 派生删除（deleteBy.../deleteAll）将因无事务直接抛 TransactionRequiredException。
 * 跨 Bean 调用使事务代理生效，这也是同花顺侧 StockBoardConstituentPersistenceService 的既有模式。
 */
@Service
@RequiredArgsConstructor
public class StockIndustryBoardEmPersistenceService {

    private final StockHelper stockHelper;
    private final StockIndustryBoardEmRepository boardRepository;
    private final StockIndustryBoardHistoryEmRepository historyRepository;
    private final StockBoardConstituentEmRepository constituentRepository;

    @Transactional(rollbackFor = Exception.class)
    public void saveBoards(List<EastmoneyBoardList.Board> boards, LocalDateTime now) {
        Map<String, StockIndustryBoardEm> existing = boardRepository.findAll().stream()
                .collect(Collectors.toMap(StockIndustryBoardEm::getSectorCode, item -> item, (first, second) -> first));
        // 上游按涨跌幅排序分页拼接，排名变动会使页边界板块在单次响应中重复出现，须批内去重
        Map<String, StockIndustryBoardEm> saves = new LinkedHashMap<>();
        Set<String> sourceSectorCodes = new HashSet<>(boards.size());
        int rank = 0;
        for (EastmoneyBoardList.Board board : boards) {
            if (board == null || board.getSectorName() == null || board.getSectorName().isBlank()) {
                continue;
            }
            if (board.getSectorCode() == null || board.getSectorCode().isBlank()) {
                continue;
            }
            sourceSectorCodes.add(board.getSectorCode());
            rank++;
            StockIndustryBoardEm entity = existing.getOrDefault(board.getSectorCode(), new StockIndustryBoardEm());
            entity.setSeqNo(rank);
            entity.setSectorName(board.getSectorName());
            entity.setSectorCode(board.getSectorCode());
            entity.setAveragePrice(board.getLatestPrice());
            entity.setChangePercent(board.getChangePercent());
            entity.setRiseCount(board.getRiseCount());
            entity.setFallCount(board.getFallCount());
            // 东财 clist 返回的成交量为手、成交额/主力净流入为元；换算为与同花顺一致的
            // 万手 / 亿元 / 亿元，前端行情数据面板按此单位展示
            entity.setTotalVolume(yuanVolumeToWanShou(board.getTotalVolume()));
            entity.setTotalAmount(yuanAmountToYi(board.getTotalAmount()));
            entity.setNetInflow(yuanAmountToYi(board.getNetInflow()));
            entity.setLeadingStock(board.getLeadingStock());
            entity.setLeadingStockChangePercent(board.getLeadingStockChangePercent());
            entity.setTradeDate(stockHelper.latestTradeDayFallback(now.toLocalDate()));
            entity.setCreateTime(now);
            saves.put(board.getSectorCode(), entity);
        }
        boardRepository.saveAll(saves.values());
        // 与上游列表对账：上游已下线的板块从本地删除（历史/成分股以 sectorName 关联，孤儿数据保留不影响展示）
        List<StockIndustryBoardEm> stale = existing.values().stream()
                .filter(item -> !sourceSectorCodes.contains(item.getSectorCode()))
                .toList();
        if (!stale.isEmpty()) {
            boardRepository.deleteAll(stale);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveHistory(String sectorName, List<EastmoneyBoardKline.Bar> source, LocalDateTime now) {
        Map<String, StockIndustryBoardHistoryEm> existing = historyRepository
                .findBySectorNameOrderByTradeDateAsc(sectorName).stream()
                .collect(Collectors.toMap(StockIndustryBoardHistoryEm::getTradeDate, item -> item, (first, second) -> first));
        Map<String, StockIndustryBoardHistoryEm> saves = new LinkedHashMap<>();
        for (EastmoneyBoardKline.Bar item : source) {
            if (item == null || item.getTime() == null || item.getTime().isBlank()) {
                continue;
            }
            String tradeDate = item.getTime().length() >= 10 ? item.getTime().substring(0, 10) : item.getTime();
            StockIndustryBoardHistoryEm entity = existing.getOrDefault(tradeDate, new StockIndustryBoardHistoryEm());
            entity.setSectorName(sectorName);
            entity.setTradeDate(tradeDate);
            entity.setOpenPrice(item.getOpenPrice());
            entity.setClosePrice(item.getClosePrice());
            entity.setHighPrice(item.getHighPrice());
            entity.setLowPrice(item.getLowPrice());
            entity.setVolume(item.getVolume());
            entity.setAmount(item.getAmount());
            entity.setChangeAmount(null);
            entity.setChangePercent(null);
            entity.setCreateTime(now);
            saves.put(tradeDate, entity);
        }
        // 东财板块 K 线不返回涨跌幅，以收盘价相对前一交易日收盘推算当日涨跌幅（首条记录无昨收则留空）
        Map<String, StockIndustryBoardHistoryEm> merged = new LinkedHashMap<>(existing);
        merged.putAll(saves);
        List<StockIndustryBoardHistoryEm> ordered = new ArrayList<>(merged.values());
        ordered.sort(Comparator.comparing(StockIndustryBoardHistoryEm::getTradeDate));
        BigDecimal previousClose = null;
        for (StockIndustryBoardHistoryEm entity : ordered) {
            if (entity.getClosePrice() != null && previousClose != null) {
                BigDecimal changeAmount = entity.getClosePrice().subtract(previousClose);
                entity.setChangeAmount(changeAmount);
                entity.setChangePercent(previousClose.signum() == 0 ? BigDecimal.ZERO
                        : changeAmount.multiply(BigDecimal.valueOf(100)).divide(previousClose, 4, RoundingMode.HALF_UP));
            } else if (entity.getClosePrice() != null) {
                entity.setChangeAmount(null);
                entity.setChangePercent(null);
            }
            if (entity.getClosePrice() != null) {
                previousClose = entity.getClosePrice();
            }
        }
        historyRepository.saveAll(saves.values());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveConstituents(String sectorName, List<EastmoneyBoardList.Board> source, LocalDateTime now) {
        String storageBoardCode = sectorName;
        Map<String, StockBoardConstituentEm> existing = constituentRepository
                .findByBoardCodeOrderByStockCodeAsc(storageBoardCode).stream()
                .collect(Collectors.toMap(StockBoardConstituentEm::getStockCode, item -> item, (first, second) -> first));
        Map<String, EastmoneyBoardList.Board> valid = source.stream()
                .filter(item -> item != null && item.getSectorCode() != null && !item.getSectorCode().isBlank())
                .filter(item -> item.getSectorName() != null && !item.getSectorName().isBlank())
                .collect(Collectors.toMap(EastmoneyBoardList.Board::getSectorCode, item -> item, (first, second) -> second, LinkedHashMap::new));
        if (valid.isEmpty()) {
            throw new IllegalStateException("东方财富行业成分股上游未包含有效股票代码");
        }
        List<StockBoardConstituentEm> saves = valid.values().stream().map(item -> {
            StockBoardConstituentEm entity = existing.getOrDefault(item.getSectorCode(), new StockBoardConstituentEm());
            entity.setBoardCode(storageBoardCode);
            entity.setStockCode(item.getSectorCode());
            entity.setStockName(item.getSectorName());
            entity.setSourceUpdatedAt(now);
            return entity;
        }).toList();
        constituentRepository.saveAll(saves);
        constituentRepository.deleteByBoardCodeAndStockCodeNotIn(storageBoardCode, List.copyOf(valid.keySet()));
    }

    /**
     * 板块详情快照落库：详情页盘口指标（今开/昨收/最高/最低/换手/量比/外盘/流通市值/流通股本）。
     * 价格/换手/量比为原始值，外盘由手换算万手，流通市值由元换算亿元，流通股本由股换算亿股；
     * 内盘上游不单独提供，前端按 总成交量-外盘 推算
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyDetail(StockIndustryBoardEm boardRow, EastmoneyBoardDetail detail, LocalDateTime now) {
        boardRow.setChangeAmount(detail.getChangeAmount());
        boardRow.setOpenPrice(detail.getOpenPrice());
        boardRow.setPreClosePrice(detail.getPreClosePrice());
        boardRow.setHighPrice(detail.getHighPrice());
        boardRow.setLowPrice(detail.getLowPrice());
        boardRow.setTurnoverRate(detail.getTurnoverRate());
        boardRow.setVolumeRatio(detail.getVolumeRatio());
        boardRow.setOuterDisc(yuanVolumeToWanShou(detail.getOuterDisc()));
        boardRow.setCirculatingMarketValue(yuanAmountToYi(detail.getCirculatingMarketValue()));
        boardRow.setCirculatingShares(yuanAmountToYi(detail.getCirculatingShares()));
        boardRow.setDetailTradeDate(now.toLocalDate());
        boardRepository.save(boardRow);
    }

    private static final BigDecimal ONE_YI = BigDecimal.valueOf(100_000_000L);
    private static final BigDecimal ONE_WAN_SHOU = BigDecimal.valueOf(10_000L);

    /** 东财成交额/主力净流入单位换算：元 → 亿元 */
    private BigDecimal yuanAmountToYi(BigDecimal yuan) {
        if (yuan == null) return null;
        return yuan.divide(ONE_YI, 4, RoundingMode.HALF_UP);
    }

    /** 东财成交量单位换算：手 → 万手 */
    private BigDecimal yuanVolumeToWanShou(BigDecimal shou) {
        if (shou == null) return null;
        return shou.divide(ONE_WAN_SHOU, 4, RoundingMode.HALF_UP);
    }
}
