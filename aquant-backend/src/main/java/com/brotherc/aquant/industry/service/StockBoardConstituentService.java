package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.industry.entity.StockBoardConstituent;
import com.brotherc.aquant.industry.model.vo.StockIndustryConstituentSnapshotVO;
import com.brotherc.aquant.industry.model.vo.StockIndustryConstituentVO;
import com.brotherc.aquant.industry.repository.StockBoardConstituentRepository;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.stock.model.dto.StockQuoteHistoryProjection;
import com.brotherc.aquant.stock.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockBoardConstituentService {

    private final StockBoardConstituentRepository stockBoardConstituentRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;
    private final StockSyncRepository stockSyncRepository;
    private final StockHelper stockHelper;

    public StockIndustryConstituentSnapshotVO getSnapshot(String industry, LocalDate tradeDate) {
        String storageBoardCode = industry;
        List<StockBoardConstituent> members = stockBoardConstituentRepository
                .findByBoardCodeOrderByStockCodeAsc(storageBoardCode);
        boolean available = !members.isEmpty();
        boolean stale = available && isStale(industry);

        StockIndustryConstituentSnapshotVO snapshot = new StockIndustryConstituentSnapshotVO();
        snapshot.setIndustry(industry);
        snapshot.setAvailable(available);
        snapshot.setStale(stale);
        snapshot.setSourceUpdatedAt(findSourceUpdatedAt(members));
        snapshot.setMessage(available
                ? (stale ? "成分股缓存已过期，正在等待下一次同步" : null)
                : "行业成分股数据暂不可用，请等待后台同步完成");
        snapshot.setContent(toViewList(members, tradeDate));
        return snapshot;
    }

    private boolean isStale(String industry) {
        Long completedTradeDayWatermark = stockHelper.getLatestClosedTradeDaySyncWatermark(LocalDateTime.now());
        String watermarkName = StockSyncConstant.STOCK_BOARD_CONSTITUENT_LATEST_PREFIX + industry;
        StockSync watermark = stockSyncRepository.findByName(watermarkName);
        Long value = StockUtils.parseSyncTimestamp(watermark);
        return value == null || value < completedTradeDayWatermark;
    }

    private LocalDateTime findSourceUpdatedAt(List<StockBoardConstituent> members) {
        return members.stream()
                .map(StockBoardConstituent::getSourceUpdatedAt)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private List<StockIndustryConstituentVO> toViewList(List<StockBoardConstituent> members, LocalDate tradeDate) {
        if (members.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> quoteCodes = members.stream()
                .map(StockBoardConstituent::getStockCode)
                .map(StockUtils::wrapExchangePrefix)
                .distinct()
                .toList();
        Map<String, List<HistoryPoint>> historyByCode = getHistoryByCode(quoteCodes, tradeDate);
        Map<String, StockQuote> latestQuoteByCode = tradeDate == null
                ? stockQuoteRepository.findByCodeIn(quoteCodes).stream().collect(Collectors.toMap(
                        StockQuote::getCode, Function.identity(), (first, second) -> first
                ))
                : Collections.emptyMap();

        List<StockIndustryConstituentVO> result = new ArrayList<>(members.size());
        for (StockBoardConstituent member : members) {
            String quoteCode = StockUtils.wrapExchangePrefix(member.getStockCode());
            List<HistoryPoint> history = historyByCode.getOrDefault(quoteCode, Collections.emptyList());
            StockIndustryConstituentVO item = new StockIndustryConstituentVO();
            item.setCode(member.getStockCode());
            item.setName(member.getStockName());
            item.setHistoryPrices(history.stream().map(HistoryPoint::closePrice).toList());
            if (tradeDate == null) {
                applyLatestQuote(item, latestQuoteByCode.get(quoteCode));
            } else {
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
        return stockQuoteHistoryRepository.findByTradeDateInAndCodeInOrderByTradeDateAsc(recentTradeDates, quoteCodes)
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

    private void applyLatestQuote(StockIndustryConstituentVO item, StockQuote quote) {
        if (quote == null) {
            item.setLatestPrice(null);
            item.setChangeAmount(null);
            item.setChangePercent(null);
            return;
        }
        item.setLatestPrice(quote.getLatestPrice());
        item.setChangeAmount(quote.getChangeAmount());
        item.setChangePercent(quote.getChangePercent());
    }

    private void applyHistoricalQuote(StockIndustryConstituentVO item, List<HistoryPoint> history, String tradeDate) {
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
                ? null
                : changeAmount.multiply(BigDecimal.valueOf(100)).divide(previousPrice, 6, RoundingMode.HALF_UP));
    }

    private record HistoryPoint(String tradeDate, BigDecimal closePrice) {
    }
}
