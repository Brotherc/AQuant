package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.industry.entity.StockBoardConstituentEm;
import com.brotherc.aquant.industry.entity.StockIndustryBoardEm;
import com.brotherc.aquant.industry.entity.StockIndustryBoardHistoryEm;
import com.brotherc.aquant.industry.repository.StockBoardConstituentEmRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardHistoryEmRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardEmRepository;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryConsEm;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryHistEm;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryNameEm;
import com.brotherc.aquant.integration.akshare.service.AKShareIndustryService;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockIndustryBoardEmSyncService {

    public static final String WATERMARK = "stock_board_industry_em_latest";
    private static final long REQUEST_INTERVAL_MILLIS = 200L;
    private static final long[] RETRY_BACKOFF_MILLIS = {300L, 900L};

    private final StockHelper stockHelper;
    private final StockSyncRepository stockSyncRepository;
    private final StockIndustryBoardEmRepository boardRepository;
    private final StockIndustryBoardHistoryEmRepository historyRepository;
    private final StockBoardConstituentEmRepository constituentRepository;
    private final AKShareIndustryService aKShareIndustryService;

    public void synchronizeIfRequired(LocalDateTime now) {
        long targetWatermark = stockHelper.getLatestClosedTradeDaySyncWatermark(now);
        Long currentWatermark = StockUtils.parseSyncTimestamp(stockSyncRepository.findByName(WATERMARK));
        if (currentWatermark != null && currentWatermark >= targetWatermark
                && !boardRepository.findAll().isEmpty()) {
            return;
        }

        List<StockBoardIndustryNameEm> boards;
        try {
            boards = callWithRetry(aKShareIndustryService::stockBoardIndustryNameEm);
        } catch (RuntimeException exception) {
            log.warn("东方财富行业列表同步最终失败，已保留缓存；请检查 AKTools 日志中的东方财富连接异常", exception);
            return;
        }
        if (CollectionUtils.isEmpty(boards)) {
            log.warn("东方财富行业列表为空，保留缓存");
            return;
        }
        saveBoards(boards, now);
        boolean completed = true;
        for (StockBoardIndustryNameEm board : boards) {
            if (board == null || board.getSectorName() == null || board.getSectorName().isBlank()) {
                continue;
            }
            completed &= synchronizeBoard(board.getSectorName(), stockHelper.latestClosedTradeDay(now), now);
            if (!sleep(REQUEST_INTERVAL_MILLIS)) {
                return;
            }
        }
        if (!completed) {
            log.warn("东方财富行业同步未完整成功，保留既有水位");
            return;
        }
        StockSync watermark = stockSyncRepository.findByName(WATERMARK);
        if (watermark == null) {
            watermark = new StockSync();
            watermark.setName(WATERMARK);
        }
        watermark.setValue(String.valueOf(targetWatermark));
        stockSyncRepository.save(watermark);
    }

    private boolean synchronizeBoard(String sectorName, LocalDate targetTradeDate, LocalDateTime now) {
        try {
            StockIndustryBoardHistoryEm latestHistory = historyRepository
                    .findTopBySectorNameOrderByTradeDateDesc(sectorName);
            boolean requiresHistory = latestHistory == null || latestHistory.getTradeDate() == null
                    || LocalDate.parse(latestHistory.getTradeDate()).isBefore(targetTradeDate);
            List<StockBoardIndustryHistEm> history = requiresHistory ? callWithRetry(() ->
                    aKShareIndustryService.stockBoardIndustryHistEm(sectorName,
                            latestHistory == null ? "19900101" : LocalDate.parse(latestHistory.getTradeDate())
                                    .plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE),
                            targetTradeDate.format(DateTimeFormatter.BASIC_ISO_DATE))) : List.of();
            List<StockBoardIndustryConsEm> constituents = callWithRetry(
                    () -> aKShareIndustryService.stockBoardIndustryConstituentsEm(sectorName)
            );
            if ((requiresHistory && CollectionUtils.isEmpty(history)) || CollectionUtils.isEmpty(constituents)) {
                log.warn("东方财富行业同步返回空数据，sectorName={}，保留缓存", sectorName);
                return false;
            }
            if (requiresHistory) {
                saveHistory(sectorName, history, now);
            }
            saveConstituents(sectorName, constituents, now);
            return true;
        } catch (RuntimeException exception) {
            log.warn("东方财富行业同步失败，sectorName={}，保留缓存", sectorName, exception);
            return false;
        }
    }

    private <T> T callWithRetry(java.util.function.Supplier<T> request) {
        RuntimeException lastException = null;
        for (int attempt = 0; attempt <= RETRY_BACKOFF_MILLIS.length; attempt++) {
            try {
                return request.get();
            } catch (RuntimeException exception) {
                lastException = exception;
                if (attempt < RETRY_BACKOFF_MILLIS.length) {
                    long backoffMillis = RETRY_BACKOFF_MILLIS[attempt];
                    log.warn("东方财富请求失败，准备退避重试，attempt={}/{}, backoffMillis={}",
                            attempt + 1, RETRY_BACKOFF_MILLIS.length + 1, backoffMillis);
                    if (!sleep(backoffMillis)) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        throw lastException == null ? new IllegalStateException("东方财富请求失败") : lastException;
    }

    @Transactional(rollbackFor = Exception.class)
    void saveBoards(List<StockBoardIndustryNameEm> boards, LocalDateTime now) {
        Map<String, StockIndustryBoardEm> existing = boardRepository.findAll().stream()
                .collect(Collectors.toMap(StockIndustryBoardEm::getSectorCode, item -> item, (first, second) -> first));
        List<StockIndustryBoardEm> saves = new ArrayList<>();
        for (StockBoardIndustryNameEm board : boards) {
            if (board == null || board.getSectorName() == null || board.getSectorName().isBlank()) {
                continue;
            }
            if (board.getSectorCode() == null || board.getSectorCode().isBlank()) {
                continue;
            }
            StockIndustryBoardEm entity = existing.getOrDefault(board.getSectorCode(), new StockIndustryBoardEm());
            entity.setSeqNo(board.getRank());
            entity.setSectorName(board.getSectorName());
            entity.setSectorCode(board.getSectorCode());
            entity.setAveragePrice(board.getLatestPrice());
            entity.setChangePercent(board.getChangePercent());
            entity.setRiseCount(board.getRiseCount());
            entity.setFallCount(board.getFallCount());
            entity.setTotalAmount(board.getTotalMarketValue());
            entity.setLeadingStock(board.getLeadingStock());
            entity.setLeadingStockChangePercent(board.getLeadingStockChangePercent());
            entity.setTradeDate(stockHelper.latestTradeDayFallback(now.toLocalDate()));
            entity.setCreateTime(now);
            saves.add(entity);
        }
        boardRepository.saveAll(saves);
    }

    @Transactional(rollbackFor = Exception.class)
    void saveHistory(String sectorName, List<StockBoardIndustryHistEm> source, LocalDateTime now) {
        Map<String, StockIndustryBoardHistoryEm> existing = historyRepository
                .findBySectorNameOrderByTradeDateAsc(sectorName).stream()
                .collect(Collectors.toMap(StockIndustryBoardHistoryEm::getTradeDate, item -> item, (first, second) -> first));
        List<StockIndustryBoardHistoryEm> saves = new ArrayList<>();
        for (StockBoardIndustryHistEm item : source) {
            if (item == null || item.getTradeDate() == null || item.getTradeDate().isBlank()) {
                continue;
            }
            StockIndustryBoardHistoryEm entity = existing.getOrDefault(item.getTradeDate(), new StockIndustryBoardHistoryEm());
            entity.setSectorName(sectorName);
            entity.setTradeDate(item.getTradeDate());
            entity.setOpenPrice(item.getOpenPrice());
            entity.setClosePrice(item.getClosePrice());
            entity.setHighPrice(item.getHighPrice());
            entity.setLowPrice(item.getLowPrice());
            entity.setVolume(item.getVolume());
            entity.setAmount(item.getAmount());
            entity.setChangeAmount(item.getChangeAmount());
            entity.setChangePercent(item.getChangePercent());
            entity.setCreateTime(now);
            saves.add(entity);
        }
        historyRepository.saveAll(saves);
    }

    @Transactional(rollbackFor = Exception.class)
    void saveConstituents(String sectorName, List<StockBoardIndustryConsEm> source, LocalDateTime now) {
        String storageBoardCode = sectorName;
        Map<String, StockBoardConstituentEm> existing = constituentRepository
                .findByBoardCodeOrderByStockCodeAsc(storageBoardCode).stream()
                .collect(Collectors.toMap(StockBoardConstituentEm::getStockCode, item -> item, (first, second) -> first));
        Map<String, StockBoardIndustryConsEm> valid = source.stream()
                .filter(item -> item != null && item.getStockCode() != null && !item.getStockCode().isBlank())
                .filter(item -> item.getStockName() != null && !item.getStockName().isBlank())
                .collect(Collectors.toMap(StockBoardIndustryConsEm::getStockCode, item -> item, (first, second) -> second, LinkedHashMap::new));
        if (valid.isEmpty()) {
            throw new IllegalStateException("东方财富行业成分股上游未包含有效股票代码");
        }
        List<StockBoardConstituentEm> saves = valid.values().stream().map(item -> {
            StockBoardConstituentEm entity = existing.getOrDefault(item.getStockCode(), new StockBoardConstituentEm());
            entity.setBoardCode(storageBoardCode);
            entity.setStockCode(item.getStockCode());
            entity.setStockName(item.getStockName());
            entity.setSourceUpdatedAt(now);
            return entity;
        }).toList();
        constituentRepository.saveAll(saves);
        constituentRepository.deleteByBoardCodeAndStockCodeNotIn(storageBoardCode, List.copyOf(valid.keySet()));
    }

    private boolean sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
