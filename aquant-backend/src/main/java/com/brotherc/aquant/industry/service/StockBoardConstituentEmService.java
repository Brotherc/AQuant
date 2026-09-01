package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.industry.entity.StockBoardConstituentEm;
import com.brotherc.aquant.industry.model.vo.StockIndustryConstituentSnapshotVO;
import com.brotherc.aquant.industry.model.vo.StockIndustryConstituentVO;
import com.brotherc.aquant.industry.repository.StockBoardConstituentEmRepository;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockBoardConstituentEmService {
    private final StockBoardConstituentEmRepository repository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;
    private final StockSyncRepository stockSyncRepository;
    private final StockHelper stockHelper;

    public StockIndustryConstituentSnapshotVO getSnapshot(String industry, LocalDate tradeDate) {
        List<StockBoardConstituentEm> members = repository.findByBoardCodeOrderByStockCodeAsc(industry);
        boolean available = !members.isEmpty();
        StockIndustryConstituentSnapshotVO snapshot = new StockIndustryConstituentSnapshotVO();
        snapshot.setIndustry(industry);
        snapshot.setAvailable(available);
        snapshot.setStale(available && isStale());
        snapshot.setSourceUpdatedAt(members.stream().map(StockBoardConstituentEm::getSourceUpdatedAt)
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null));
        snapshot.setMessage(available ? (snapshot.isStale() ? "东方财富成分股缓存已过期" : null)
                : "东方财富行业成分股数据暂不可用，请等待后台同步完成");
        snapshot.setContent(toViews(members, tradeDate));
        return snapshot;
    }

    private boolean isStale() {
        Long target = stockHelper.getLatestClosedTradeDaySyncWatermark(LocalDateTime.now());
        StockSync watermark = stockSyncRepository.findByName(StockIndustryBoardEmSyncService.WATERMARK);
        Long value = StockUtils.parseSyncTimestamp(watermark);
        return value == null || value < target;
    }

    private List<StockIndustryConstituentVO> toViews(List<StockBoardConstituentEm> members, LocalDate tradeDate) {
        if (members.isEmpty()) return Collections.emptyList();
        List<String> quoteCodes = members.stream().map(StockBoardConstituentEm::getStockCode)
                .map(StockUtils::wrapExchangePrefix).distinct().toList();
        Map<String, List<HistoryPoint>> history = historyByCode(quoteCodes, tradeDate);
        Map<String, StockQuote> latest = tradeDate == null ? stockQuoteRepository.findByCodeIn(quoteCodes).stream()
                .collect(Collectors.toMap(StockQuote::getCode, Function.identity(), (first, second) -> first)) : Map.of();
        List<StockIndustryConstituentVO> result = new ArrayList<>(members.size());
        for (StockBoardConstituentEm member : members) {
            String quoteCode = StockUtils.wrapExchangePrefix(member.getStockCode());
            List<HistoryPoint> series = history.getOrDefault(quoteCode, List.of());
            StockIndustryConstituentVO item = new StockIndustryConstituentVO();
            item.setCode(member.getStockCode());
            item.setName(member.getStockName());
            item.setHistoryPrices(series.stream().map(HistoryPoint::closePrice).toList());
            if (tradeDate == null) applyLatest(item, latest.get(quoteCode));
            else applyHistorical(item, series, tradeDate.toString());
            result.add(item);
        }
        result.sort(Comparator.comparing(StockIndustryConstituentVO::getChangePercent,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private Map<String, List<HistoryPoint>> historyByCode(List<String> codes, LocalDate tradeDate) {
        List<String> dates = tradeDate == null ? stockQuoteHistoryRepository.findRecentTradeDates(10)
                : stockQuoteHistoryRepository.findRecentTradeDatesBefore(tradeDate.toString(), 10);
        if (dates.isEmpty()) return Map.of();
        return stockQuoteHistoryRepository.findByTradeDateInAndCodeInOrderByTradeDateAsc(dates, codes).stream()
                .filter(row -> row.getClosePrice() != null)
                .collect(Collectors.groupingBy(StockQuoteHistoryProjection::getCode, LinkedHashMap::new,
                        Collectors.mapping(row -> new HistoryPoint(row.getTradeDate(), row.getClosePrice()), Collectors.toList())));
    }

    private void applyLatest(StockIndustryConstituentVO item, StockQuote quote) {
        item.setLatestPrice(quote == null ? null : quote.getLatestPrice());
        item.setChangeAmount(quote == null ? null : quote.getChangeAmount());
        item.setChangePercent(quote == null ? null : quote.getChangePercent());
    }

    private void applyHistorical(StockIndustryConstituentVO item, List<HistoryPoint> series, String tradeDate) {
        if (series.isEmpty() || !tradeDate.equals(series.get(series.size() - 1).tradeDate())) {
            item.setLatestPrice(null); item.setChangeAmount(null); item.setChangePercent(null); return;
        }
        BigDecimal latest = series.get(series.size() - 1).closePrice();
        item.setLatestPrice(latest);
        if (series.size() < 2) { item.setChangeAmount(null); item.setChangePercent(null); return; }
        BigDecimal previous = series.get(series.size() - 2).closePrice();
        BigDecimal amount = latest.subtract(previous);
        item.setChangeAmount(amount);
        item.setChangePercent(previous.signum() == 0 ? null
                : amount.multiply(BigDecimal.valueOf(100)).divide(previous, 6, RoundingMode.HALF_UP));
    }

    private record HistoryPoint(String tradeDate, BigDecimal closePrice) { }
}
