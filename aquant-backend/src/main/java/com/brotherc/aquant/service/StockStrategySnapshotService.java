package com.brotherc.aquant.service;

import com.brotherc.aquant.constant.StockSyncConstant;
import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.entity.StockStrategyDualMaBacktestSnapshot;
import com.brotherc.aquant.entity.StockSync;
import com.brotherc.aquant.model.vo.strategy.DualMABacktestReqVO;
import com.brotherc.aquant.model.vo.strategy.StockTradeBacktestVO;
import com.brotherc.aquant.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.repository.StockQuoteRepository;
import com.brotherc.aquant.repository.StockStrategyDualMaBacktestSnapshotRepository;
import com.brotherc.aquant.repository.StockSyncRepository;
import com.brotherc.aquant.repository.projection.StockQuoteHistoryProjection;
import com.brotherc.aquant.strategy.DualMovingAverageStrategy;
import com.brotherc.aquant.utils.StockHelper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockStrategySnapshotService {

    private static final String[] PRESET_MARKETS = {"sh", "sz", "bj"};
    private static final int[] PRESET_MA_OPTIONS = {5, 10, 20, 30, 60, 120};
    private static final int[] PRESET_RECENT_YEARS = {1, 2, 3, 5};
    private static final int SNAPSHOT_BATCH_SIZE = 200;
    private static final int MAX_NEED_DAYS = 5 * 250 + 120;

    private final DualMovingAverageStrategy dualMovingAverageStrategy;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;
    private final StockSyncRepository stockSyncRepository;
    private final StockStrategyDualMaBacktestSnapshotRepository snapshotRepository;
    private final StockHelper stockHelper;

    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    public Page<StockTradeBacktestVO> queryDualMABacktestSnapshot(
            DualMABacktestReqVO reqVO,
            Pageable pageable,
            Set<String> watchlistCodes
    ) {
        String market = normalizeMarket(reqVO.getMarket());
        if (!isPresetRequest(reqVO)) {
            return null;
        }

        Long batchNo = getLatestBatchNo();
        if (batchNo == null) {
            return null;
        }

        if (!snapshotRepository.existsByBatchNoAndMarketAndMaShortAndMaLongAndRecentYears(
                batchNo, market, reqVO.getMaShort(), reqVO.getMaLong(), reqVO.getRecentYears()
        )) {
            return null;
        }

        return snapshotRepository.findAll(buildSnapshotSpec(batchNo, market, reqVO, watchlistCodes), pageable)
                .map(this::toVO);
    }

    public boolean isPresetRequest(DualMABacktestReqVO reqVO) {
        return isPresetMarket(reqVO.getMarket())
                && isPresetMa(reqVO.getMaShort())
                && isPresetMa(reqVO.getMaLong())
                && reqVO.getMaShort() < reqVO.getMaLong()
                && isPresetRecentYears(reqVO.getRecentYears());
    }

    public void refreshDualMaBacktestSnapshots() {
        if (!refreshing.compareAndSet(false, true)) {
            log.info("双均线回测快照任务已在执行中，本次跳过");
            return;
        }

        long batchNo = System.currentTimeMillis();
        try {
            if (shouldSkipRefreshDualMaBacktestSnapshots()) {
                return;
            }

            List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(MAX_NEED_DAYS);
            if (CollectionUtils.isEmpty(recentDates)) {
                log.warn("双均线回测快照生成跳过，历史行情为空");
                return;
            }

            for (String market : PRESET_MARKETS) {
                refreshMarketSnapshots(batchNo, market, recentDates);
            }

            activateLatestBatch(batchNo);
            cleanupOldBatches(batchNo);
            log.info("双均线回测快照生成完成，batchNo={}", batchNo);
        } catch (Exception e) {
            log.error("双均线回测快照生成失败，batchNo={}", batchNo, e);
        } finally {
            refreshing.set(false);
        }
    }

    private void refreshMarketSnapshots(Long batchNo, String market, List<String> recentDates) {
        List<StockQuote> stocks = stockQuoteRepository.findByCodeStartingWithIgnoreCase(market);
        if (CollectionUtils.isEmpty(stocks)) {
            log.info("市场 {} 无股票数据，跳过双均线回测快照", market);
            return;
        }

        for (int b = 0; b < stocks.size(); b += SNAPSHOT_BATCH_SIZE) {
            List<StockQuote> batch = stocks.subList(b, Math.min(stocks.size(), b + SNAPSHOT_BATCH_SIZE));
            List<String> codes = batch.stream().map(StockQuote::getCode).toList();
            List<StockQuoteHistoryProjection> histories = stockQuoteHistoryRepository
                    .findByTradeDateInAndCodeInOrderByTradeDateAsc(recentDates, codes);

            var historyMap = dualMovingAverageStrategy.groupHistoriesByCode(histories);
            List<StockStrategyDualMaBacktestSnapshot> snapshots = new ArrayList<>();
            TTest tTest = new TTest();

            for (StockQuote stock : batch) {
                List<StockQuoteHistoryProjection> stockHistories = historyMap.getOrDefault(stock.getCode(), Collections.emptyList());
                BigDecimal[] closePrices = dualMovingAverageStrategy.extractClosePrices(stockHistories);

                for (int recentYears : PRESET_RECENT_YEARS) {
                    for (int maShort : PRESET_MA_OPTIONS) {
                        BigDecimal maShortDecimal = BigDecimal.valueOf(maShort);
                        for (int maLong : PRESET_MA_OPTIONS) {
                            if (maShort >= maLong) {
                                continue;
                            }
                            BigDecimal maLongDecimal = BigDecimal.valueOf(maLong);
                            StockTradeBacktestVO vo = dualMovingAverageStrategy.backtestSingle(
                                    stock, closePrices, maShort, maLong, recentYears, tTest, maShortDecimal, maLongDecimal
                            );
                            snapshots.add(toSnapshot(batchNo, market, maShort, maLong, recentYears, vo));
                        }
                    }
                }
            }

            for (StockStrategyDualMaBacktestSnapshot snapshot : snapshots) {
                if ((snapshot.getTValue() != null && !Double.isFinite(snapshot.getTValue()))
                        || (snapshot.getPValue() != null && !Double.isFinite(snapshot.getPValue()))) {
                    log.warn("invalid snapshot: market={}, code={}, maShort={}, maLong={}, recentYears={}, tValue={}, pValue={}",
                            snapshot.getMarket(), snapshot.getCode(), snapshot.getMaShort(),
                            snapshot.getMaLong(), snapshot.getRecentYears(),
                            snapshot.getTValue(), snapshot.getPValue());
                }
            }

            snapshotRepository.saveAll(snapshots);
            log.info("双均线回测快照已生成，market={}, batchNo={}, progress={}/{}", market, batchNo,
                    Math.min(b + SNAPSHOT_BATCH_SIZE, stocks.size()), stocks.size());
        }
    }

    private StockStrategyDualMaBacktestSnapshot toSnapshot(
            Long batchNo,
            String market,
            int maShort,
            int maLong,
            int recentYears,
            StockTradeBacktestVO vo
    ) {
        StockStrategyDualMaBacktestSnapshot snapshot = new StockStrategyDualMaBacktestSnapshot();
        snapshot.setBatchNo(batchNo);
        snapshot.setMarket(market);
        snapshot.setCode(vo.getCode());
        snapshot.setName(vo.getName());
        snapshot.setMaShort(maShort);
        snapshot.setMaLong(maLong);
        snapshot.setRecentYears(recentYears);
        snapshot.setTotalReturn(vo.getTotalReturn());
        snapshot.setTradeCount(vo.getTradeCount());
        snapshot.setWinRate(vo.getWinRate());
        snapshot.setTValue(normalizeFinite(vo.getTValue()));
        snapshot.setPValue(normalizeFinite(vo.getPValue()));
        snapshot.setReliability(vo.getReliability());
        snapshot.setLatestPrice(vo.getLatestPrice());
        snapshot.setPir(vo.getPir());
        return snapshot;
    }

    private StockTradeBacktestVO toVO(StockStrategyDualMaBacktestSnapshot snapshot) {
        return new StockTradeBacktestVO(
                snapshot.getCode(),
                snapshot.getName(),
                snapshot.getTotalReturn(),
                snapshot.getTradeCount(),
                snapshot.getWinRate(),
                snapshot.getTValue(),
                snapshot.getPValue(),
                snapshot.getReliability(),
                snapshot.getLatestPrice(),
                snapshot.getPir(),
                snapshot.getCreatedAt()
        );
    }

    private Specification<StockStrategyDualMaBacktestSnapshot> buildSnapshotSpec(
            Long batchNo,
            String market,
            DualMABacktestReqVO reqVO,
            Set<String> watchlistCodes
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("batchNo"), batchNo));
            predicates.add(cb.equal(root.get("market"), market));
            predicates.add(cb.equal(root.get("maShort"), reqVO.getMaShort()));
            predicates.add(cb.equal(root.get("maLong"), reqVO.getMaLong()));
            predicates.add(cb.equal(root.get("recentYears"), reqVO.getRecentYears()));

            if (StringUtils.isNotBlank(reqVO.getCode())) {
                predicates.add(cb.equal(root.get("code"), reqVO.getCode()));
            }

            if (StringUtils.isNotBlank(reqVO.getReliability())) {
                predicates.add(cb.equal(root.get("reliability"), reqVO.getReliability()));
            }

            if (watchlistCodes != null) {
                List<Predicate> orPredicates = new ArrayList<>();
                for (String watchlistCode : watchlistCodes) {
                    orPredicates.add(cb.like(root.get("code"), "%" + watchlistCode));
                }
                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Long getLatestBatchNo() {
        return getSyncTimestamp(StockSyncConstant.STOCK_STRATEGY_DUAL_MA_BACKTEST_SNAPSHOT_LATEST);
    }

    private boolean isPresetMarket(String market) {
        String normalizedMarket = normalizeMarket(market);
        if (StringUtils.isBlank(normalizedMarket)) {
            return false;
        }
        for (String presetMarket : PRESET_MARKETS) {
            if (presetMarket.equals(normalizedMarket)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeMarket(String market) {
        if (StringUtils.isBlank(market)) {
            return null;
        }
        return market.trim().toLowerCase();
    }

    private Double normalizeFinite(Double value) {
        return value != null && Double.isFinite(value) ? value : null;
    }

    private boolean isPresetMa(Integer ma) {
        if (ma == null) {
            return false;
        }
        for (int preset : PRESET_MA_OPTIONS) {
            if (preset == ma) {
                return true;
            }
        }
        return false;
    }

    private boolean isPresetRecentYears(Integer recentYears) {
        if (recentYears == null) {
            return false;
        }
        for (int preset : PRESET_RECENT_YEARS) {
            if (preset == recentYears) {
                return true;
            }
        }
        return false;
    }

    protected void activateLatestBatch(Long batchNo) {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_STRATEGY_DUAL_MA_BACKTEST_SNAPSHOT_LATEST);
        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName(StockSyncConstant.STOCK_STRATEGY_DUAL_MA_BACKTEST_SNAPSHOT_LATEST);
        }
        stockSync.setValue(String.valueOf(batchNo));
        stockSyncRepository.save(stockSync);
    }

    protected void cleanupOldBatches(Long batchNo) {
        snapshotRepository.deleteByBatchNoNot(batchNo);
    }

    private boolean shouldSkipRefreshDualMaBacktestSnapshots() {
        Long stockDailyLatest = getSyncTimestamp(StockSyncConstant.STOCK_DAILY_LATEST);
        if (stockDailyLatest == null) {
            return false;
        }

        LocalDate latestTradeDay = stockHelper.latestTradeDayFallback(LocalDate.now());
        long latestTradeDayCloseMillis = latestTradeDay.atTime(15, 0, 0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (stockDailyLatest < latestTradeDayCloseMillis) {
            return false;
        }

        Long latestBatchNo = getLatestBatchNo();
        if (latestBatchNo == null || latestBatchNo < stockDailyLatest) {
            return false;
        }

        log.info("双均线回测快照已覆盖最近交易日收盘数据，跳过生成。latestTradeDay={}, stockDailyLatest={}, latestBatchNo={}",
                latestTradeDay, stockDailyLatest, latestBatchNo);
        return true;
    }

    private Long getSyncTimestamp(String syncName) {
        StockSync stockSync = stockSyncRepository.findByName(syncName);
        if (stockSync == null || StringUtils.isBlank(stockSync.getValue())) {
            return null;
        }

        try {
            return Long.valueOf(stockSync.getValue());
        } catch (NumberFormatException e) {
            log.warn("同步标记值格式非法，name={}, value={}", syncName, stockSync.getValue());
            return null;
        }
    }

}
