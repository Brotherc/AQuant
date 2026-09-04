package com.brotherc.aquant.stock.service;

import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.integration.akshare.model.StockZhAMinute;
import com.brotherc.aquant.integration.akshare.service.AKShareService;
import com.brotherc.aquant.integration.tencent.model.TencentMinuteQuote;
import com.brotherc.aquant.integration.tencent.model.TencentOrderBook;
import com.brotherc.aquant.integration.tencent.service.TencentFinanceService;
import com.brotherc.aquant.stock.entity.StockMinuteBar;
import com.brotherc.aquant.stock.model.vo.StockMinutePointVO;
import com.brotherc.aquant.stock.model.vo.StockMinuteRealtimeVO;
import com.brotherc.aquant.stock.model.vo.StockOrderBookVO;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 分钟数据编排（闸①：水位 + 尝试负缓存 + tryLock；实时分时：内存 TTL 缓存代理）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockMinuteService {

    private static final String WATER_MARK_PREFIX = "minute_sync:";
    private static final String TRY_MARK_PREFIX = "minute_sync_try:";
    private static final long RETRY_COOLDOWN_MILLIS = 3 * 60 * 1000L;
    private static final long REALTIME_TTL_MILLIS = 10 * 1000L;
    private static final long ORDER_BOOK_TTL_MILLIS = 5 * 1000L;

    private final StockMinuteBarService stockMinuteBarService;
    private final StockSyncRepository stockSyncRepository;
    private final AKShareService akShareService;
    private final TencentFinanceService tencentFinanceService;
    private final StockHelper stockHelper;

    private final ConcurrentHashMap<String, ReentrantLock> syncLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry<StockMinuteRealtimeVO>> realtimeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry<StockOrderBookVO>> orderBookCache = new ConcurrentHashMap<>();

    /**
     * 近 N 个已收盘交易日的 1 分钟 K 线（"1分"K线与"五日分时"共用数据源）。上游同步失败时降级返回已有 DB 数据。
     */
    public List<StockMinuteBar> getMinuteKline(String code, Integer days, LocalDateTime now) {
        String symbol = StockUtils.wrapExchangePrefix(code);
        int dayCount = Math.min(Math.max(days == null ? 5 : days, 1), 8);
        syncIfStale(symbol, now);
        return stockMinuteBarService.findRecentBars(symbol, dayCount);
    }

    /**
     * 当日实时分时（10 秒 TTL 缓存，避免轮询打爆上游）
     */
    public StockMinuteRealtimeVO getRealtimeMinute(String code) {
        String symbol = StockUtils.wrapExchangePrefix(code);
        CacheEntry<StockMinuteRealtimeVO> cached = realtimeCache.get(symbol);
        if (cached != null && cached.expireAt > System.currentTimeMillis()) {
            return cached.value;
        }
        TencentMinuteQuote quote = tencentFinanceService.fetchMinute(symbol);
        StockMinuteRealtimeVO vo = buildVO(symbol, quote);
        realtimeCache.put(symbol, new CacheEntry<>(vo, System.currentTimeMillis() + REALTIME_TTL_MILLIS));
        return vo;
    }

    private StockMinuteRealtimeVO buildVO(String code, TencentMinuteQuote quote) {
        StockMinuteRealtimeVO vo = new StockMinuteRealtimeVO();
        vo.setCode(code);
        vo.setName(quote.getName());
        vo.setTradeDate(quote.getDate());
        vo.setPrevClose(quote.getPrevClose());

        List<StockMinutePointVO> points = new ArrayList<>(quote.getPoints().size());
        BigDecimal prevCumVolume = BigDecimal.ZERO;
        for (TencentMinuteQuote.Point quotePoint : quote.getPoints()) {
            StockMinutePointVO point = new StockMinutePointVO();
            point.setTime(quotePoint.getTime());
            point.setPrice(quotePoint.getPrice());

            BigDecimal cumVolume = quotePoint.getCumVolume();
            BigDecimal cumAmount = quotePoint.getCumAmount();
            if (cumVolume != null) {
                point.setVolume(cumVolume.subtract(prevCumVolume));
                prevCumVolume = cumVolume;
            }
            if (cumAmount != null && cumVolume != null && cumVolume.compareTo(BigDecimal.ZERO) > 0) {
                point.setAvgPrice(cumAmount
                        .divide(cumVolume.multiply(BigDecimal.valueOf(100)), 4, RoundingMode.HALF_UP));
            }
            points.add(point);
        }
        vo.setPoints(points);

        if (!points.isEmpty()) {
            vo.setOpen(points.get(0).getPrice());
            vo.setLatestPrice(points.get(points.size() - 1).getPrice());
        }
        return vo;
    }

    /**
     * 实时盘口（5 秒 TTL 缓存，多弹窗/多用户轮询共用一份上游结果）
     */
    public StockOrderBookVO getOrderBook(String code) {
        String symbol = StockUtils.wrapExchangePrefix(code);
        CacheEntry<StockOrderBookVO> cached = orderBookCache.get(symbol);
        if (cached != null && cached.expireAt > System.currentTimeMillis()) {
            return cached.value;
        }
        StockOrderBookVO vo = buildVO(symbol, tencentFinanceService.fetchOrderBook(symbol));
        orderBookCache.put(symbol, new CacheEntry<>(vo, System.currentTimeMillis() + ORDER_BOOK_TTL_MILLIS));
        return vo;
    }

    private StockOrderBookVO buildVO(String code, TencentOrderBook book) {
        StockOrderBookVO vo = new StockOrderBookVO();
        vo.setCode(code);
        vo.setName(book.getName());
        vo.setLatestPrice(book.getLatestPrice());
        vo.setChange(book.getChange());
        vo.setChangePercent(book.getChangePercent());
        vo.setPrevClose(book.getPrevClose());
        vo.setOpen(book.getOpen());
        vo.setHigh(book.getHigh());
        vo.setLow(book.getLow());
        vo.setVolume(book.getVolume());
        vo.setTurnover(book.getTurnover());
        vo.setTurnoverRate(book.getTurnoverRate());
        vo.setQuantityRatio(book.getQuantityRatio());
        vo.setQuoteTime(formatQuoteTime(book.getQuoteTime()));

        for (TencentOrderBook.Level bid : book.getBids()) {
            vo.getBids().add(toLevel(bid));
        }
        for (TencentOrderBook.Level ask : book.getAsks()) {
            vo.getAsks().add(toLevel(ask));
        }
        return vo;
    }

    private StockOrderBookVO.Level toLevel(TencentOrderBook.Level level) {
        StockOrderBookVO.Level vo = new StockOrderBookVO.Level();
        vo.setPrice(level.getPrice());
        vo.setVolume(level.getVolume());
        return vo;
    }

    /**
     * "yyyyMMddHHmmss" 转 "HH:mm:ss"
     */
    private String formatQuoteTime(String raw) {
        if (raw == null || raw.length() < 14) {
            return raw;
        }
        return raw.substring(8, 10) + ":" + raw.substring(10, 12) + ":" + raw.substring(12, 14);
    }

    /**
     * 闸①：水位达标直接返回；3 分钟尝试负缓存挡失败风暴；tryLock 防并发重复拉上游。
     * 上游 HTTP 在事务外执行，落库事务在 StockMinuteBarService.save 内。
     */
    private void syncIfStale(String code, LocalDateTime now) {
        try {
            String target = stockHelper.latestClosedTradeDay(now).toString();
            String waterMarkName = WATER_MARK_PREFIX + code;
            String tryMarkName = TRY_MARK_PREFIX + code;

            StockSync waterMark = stockSyncRepository.findByName(waterMarkName);
            if (isWaterMarkReached(waterMark, target)) {
                return;
            }

            if (isInRetryCooldown(stockSyncRepository.findByName(tryMarkName))) {
                return;
            }

            ReentrantLock lock = syncLocks.computeIfAbsent(code, k -> new ReentrantLock());
            if (!lock.tryLock()) {
                return;
            }
            try {
                waterMark = stockSyncRepository.findByName(waterMarkName);
                if (isWaterMarkReached(waterMark, target)) {
                    return;
                }

                upsertSync(tryMarkName, String.valueOf(System.currentTimeMillis()));
                List<StockZhAMinute> rows = akShareService.stockZhAMinute(code, "1", "");
                String latestDay = stockMinuteBarService.save(rows, code, now);
                if (latestDay != null) {
                    upsertSync(waterMarkName, latestDay);
                    log.info("分钟K线同步完成, code={}, latestDay={}", code, latestDay);
                }
                // 上游窗口内没有目标日数据（停牌等）：水位直接推到目标，避免每次点击都重打上游
                String upstreamMaxDay = rows.stream()
                        .map(StockZhAMinute::getDay)
                        .filter(day -> day != null && day.length() >= 10)
                        .max(String::compareTo)
                        .map(day -> day.substring(0, 10))
                        .orElse(null);
                if (upstreamMaxDay == null || upstreamMaxDay.compareTo(target) < 0) {
                    upsertSync(waterMarkName, target);
                }
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.warn("分钟K线同步失败,降级读取已有数据, code={}, msg={}", code, e.getMessage());
        }
    }

    private boolean isWaterMarkReached(StockSync waterMark, String target) {
        return waterMark != null
                && waterMark.getValue() != null
                && waterMark.getValue().compareTo(target) >= 0;
    }

    private boolean isInRetryCooldown(StockSync tryMark) {
        if (tryMark == null || tryMark.getValue() == null) {
            return false;
        }
        try {
            long lastTry = Long.parseLong(tryMark.getValue().trim());
            return System.currentTimeMillis() - lastTry < RETRY_COOLDOWN_MILLIS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void upsertSync(String name, String value) {
        StockSync sync = stockSyncRepository.findByName(name);
        if (sync == null) {
            sync = new StockSync();
            sync.setName(name);
        }
        sync.setValue(value);
        stockSyncRepository.save(sync);
    }

    private static final class CacheEntry<T> {
        private final T value;
        private final long expireAt;

        private CacheEntry(T value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }

}
