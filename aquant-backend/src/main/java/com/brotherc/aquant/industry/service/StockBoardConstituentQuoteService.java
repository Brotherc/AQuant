package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.industry.entity.StockBoardConstituentQuote;
import com.brotherc.aquant.industry.model.vo.StockIndustryConstituentSnapshotVO;
import com.brotherc.aquant.industry.model.vo.StockIndustryConstituentVO;
import com.brotherc.aquant.industry.repository.StockBoardConstituentQuoteRepository;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryConsThs;
import com.brotherc.aquant.integration.akshare.service.AKShareIndustryService;
import com.brotherc.aquant.stock.model.dto.StockQuoteHistoryProjection;
import com.brotherc.aquant.stock.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockBoardConstituentQuoteService {

    private final StockBoardConstituentQuoteRepository stockBoardConstituentQuoteRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;
    private final StockSyncRepository stockSyncRepository;
    private final AKShareIndustryService aKShareIndustryService;
    private final StockBoardConstituentQuoteSyncService stockBoardConstituentQuoteSyncService;

    private final ConcurrentMap<String, Object> boardSyncLocks = new ConcurrentHashMap<>();

    public StockIndustryConstituentSnapshotVO getSnapshot(String industry, LocalDate tradeDate) {
        List<StockBoardConstituentQuote> quotes = loadQuotes(industry);
        boolean stale = !isCurrent(quotes);
        String message = null;

        if (stale) {
            synchronized (boardSyncLocks.computeIfAbsent(industry, ignored -> new Object())) {
                quotes = loadQuotes(industry);
                if (!isCurrent(quotes)) {
                    try {
                        sync(industry);
                        quotes = loadQuotes(industry);
                        stale = false;
                    } catch (RuntimeException exception) {
                        log.warn("行业成分股同步失败，industry={}, cachedCount={}", industry, quotes.size(), exception);
                        message = quotes.isEmpty() ? "行业成分股数据暂不可用" : "成分股行情更新失败，正在展示缓存数据";
                    }
                } else {
                    stale = false;
                }
            }
        }

        StockIndustryConstituentSnapshotVO snapshot = new StockIndustryConstituentSnapshotVO();
        snapshot.setIndustry(industry);
        snapshot.setAvailable(!quotes.isEmpty());
        snapshot.setStale(stale);
        snapshot.setMessage(message);
        snapshot.setSourceUpdatedAt(getSourceUpdatedAt(quotes));
        snapshot.setContent(toViewList(quotes, tradeDate));
        return snapshot;
    }

    private void sync(String industry) {
        List<StockBoardIndustryConsThs> sourceList = aKShareIndustryService.stockBoardIndustryConstituentsThs(industry);
        stockBoardConstituentQuoteSyncService.sync(industry, sourceList);
    }

    private List<StockBoardConstituentQuote> loadQuotes(String industry) {
        return stockBoardConstituentQuoteRepository.findByBoardCodeOrderByChangePercentDesc(industry);
    }

    private boolean isCurrent(List<StockBoardConstituentQuote> quotes) {
        if (quotes.isEmpty()) {
            return false;
        }
        LocalDateTime latestSourceTime = getSourceUpdatedAt(quotes);
        StockSync boardSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST);
        Long boardSyncTimestamp = StockUtils.parseSyncTimestamp(boardSync);
        if (latestSourceTime == null || boardSyncTimestamp == null) {
            return latestSourceTime != null;
        }
        LocalDateTime boardSyncTime = Instant.ofEpochMilli(boardSyncTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        return !latestSourceTime.isBefore(boardSyncTime);
    }

    private LocalDateTime getSourceUpdatedAt(List<StockBoardConstituentQuote> quotes) {
        return quotes.stream()
                .map(StockBoardConstituentQuote::getCreatedAt)
                .filter(item -> item != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private List<StockIndustryConstituentVO> toViewList(
            List<StockBoardConstituentQuote> quotes, LocalDate tradeDate
    ) {
        if (quotes.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> quoteCodes = quotes.stream()
                .map(StockBoardConstituentQuote::getStockCode)
                .map(StockUtils::wrapExchangePrefix)
                .toList();
        Map<String, List<HistoryPoint>> historyByCode = getHistoryByCode(quoteCodes, tradeDate);
        List<StockIndustryConstituentVO> result = new ArrayList<>(quotes.size());
        for (StockBoardConstituentQuote quote : quotes) {
            String quoteCode = StockUtils.wrapExchangePrefix(quote.getStockCode());
            List<HistoryPoint> history = historyByCode.getOrDefault(quoteCode, Collections.emptyList());
            StockIndustryConstituentVO item = new StockIndustryConstituentVO();
            item.setCode(quote.getStockCode());
            item.setName(quote.getStockName());
            item.setLatestPrice(quote.getLatestPrice());
            item.setChangeAmount(quote.getChangeAmount());
            item.setChangePercent(quote.getChangePercent());
            item.setHistoryPrices(history.stream().map(HistoryPoint::closePrice).toList());
            if (tradeDate != null) {
                applyHistoricalQuote(item, history, tradeDate.toString());
            }
            result.add(item);
        }
        result.sort(Comparator.comparing(
                StockIndustryConstituentVO::getChangePercent,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return result;
    }

    private Map<String, List<HistoryPoint>> getHistoryByCode(List<String> quoteCodes, LocalDate tradeDate) {
        List<String> recentTradeDates = tradeDate == null
                ? stockQuoteHistoryRepository.findRecentTradeDates(10)
                : stockQuoteHistoryRepository.findRecentTradeDatesBefore(tradeDate.toString(), 10);
        if (recentTradeDates.isEmpty()) {
            return Collections.emptyMap();
        }
        return stockQuoteHistoryRepository
                .findByTradeDateInAndCodeInOrderByTradeDateAsc(recentTradeDates, quoteCodes)
                .stream()
                .filter(history -> history.getClosePrice() != null)
                .collect(Collectors.groupingBy(
                        StockQuoteHistoryProjection::getCode,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                history -> new HistoryPoint(history.getTradeDate(), history.getClosePrice()),
                                Collectors.toList()
                        )
                ));
    }

    private void applyHistoricalQuote(
            StockIndustryConstituentVO item, List<HistoryPoint> history, String tradeDate
    ) {
        if (history.isEmpty() || !tradeDate.equals(history.get(history.size() - 1).tradeDate())) {
            item.setLatestPrice(null);
            item.setChangeAmount(null);
            item.setChangePercent(null);
            return;
        }
        BigDecimal latestPrice = history.get(history.size() - 1).closePrice();
        item.setLatestPrice(latestPrice);
        if (history.size() < 2) {
            item.setChangeAmount(null);
            item.setChangePercent(null);
            return;
        }
        BigDecimal previousPrice = history.get(history.size() - 2).closePrice();
        BigDecimal changeAmount = latestPrice.subtract(previousPrice);
        item.setChangeAmount(changeAmount);
        item.setChangePercent(previousPrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : changeAmount.divide(previousPrice, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
    }

    private record HistoryPoint(String tradeDate, BigDecimal closePrice) {
    }
}
