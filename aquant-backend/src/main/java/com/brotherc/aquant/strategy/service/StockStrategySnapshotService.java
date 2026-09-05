package com.brotherc.aquant.strategy.service;

import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.strategy.entity.StockStrategyDualMaBacktestSnapshot;
import com.brotherc.aquant.strategy.entity.StockStrategyMomentumBacktestSnapshot;
import com.brotherc.aquant.strategy.entity.StockStrategyMacdBacktestSnapshot;
import com.brotherc.aquant.strategy.entity.StockStrategyGridBacktestSnapshot;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.strategy.model.vo.DualMABacktestReqVO;
import com.brotherc.aquant.strategy.model.vo.MomentumBacktestReqVO;
import com.brotherc.aquant.strategy.model.vo.MacdBacktestReqVO;
import com.brotherc.aquant.strategy.model.vo.GridBacktestReqVO;
import com.brotherc.aquant.strategy.model.vo.StockTradeBacktestVO;
import com.brotherc.aquant.stock.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.strategy.repository.StockStrategyDualMaBacktestSnapshotRepository;
import com.brotherc.aquant.strategy.repository.StockStrategyMomentumBacktestSnapshotRepository;
import com.brotherc.aquant.strategy.repository.StockStrategyMacdBacktestSnapshotRepository;
import com.brotherc.aquant.strategy.repository.StockStrategyGridBacktestSnapshotRepository;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import com.brotherc.aquant.stock.model.dto.StockQuoteHistoryProjection;
import com.brotherc.aquant.common.utils.StockHelper;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private static final String P_VALUE = "pValue";
    private static final String RELIABILITY = "reliability";

    private static final String[] PRESET_MARKETS = {"sh", "sz", "bj"};
    private static final int[] PRESET_MA_OPTIONS = {5, 10, 20, 30, 60, 120};
    private static final int[] PRESET_MOMENTUM_LOOKBACK_DAY_OPTIONS = {10, 20, 60, 120};
    private static final int[] PRESET_RECENT_YEARS = {1, 2, 3, 5};
    private static final int PRESET_MACD_FAST_PERIOD = 12;
    private static final int PRESET_MACD_SLOW_PERIOD = 26;
    private static final int PRESET_MACD_SIGNAL_PERIOD = 9;
    private static final BigDecimal PRESET_GRID_RATE = new BigDecimal("0.03");
    private static final int PRESET_GRID_COUNT = 5;
    private static final int SNAPSHOT_BATCH_SIZE = 200;
    private static final int MAX_NEED_DAYS = 5 * 250 + 120;

    private final DualMovingAverageStrategy dualMovingAverageStrategy;
    private final MomentumStrategy momentumStrategy;
    private final MacdStrategy macdStrategy;
    private final GridTradingStrategy gridTradingStrategy;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;
    private final StockSyncRepository stockSyncRepository;
    private final StockStrategyDualMaBacktestSnapshotRepository dualMaSnapshotRepository;
    private final StockStrategyMomentumBacktestSnapshotRepository momentumSnapshotRepository;
    private final StockStrategyMacdBacktestSnapshotRepository macdSnapshotRepository;
    private final StockStrategyGridBacktestSnapshotRepository gridSnapshotRepository;
    private final StockHelper stockHelper;

    private final AtomicBoolean dualMaRefreshing = new AtomicBoolean(false);
    private final AtomicBoolean momentumRefreshing = new AtomicBoolean(false);
    private final AtomicBoolean macdRefreshing = new AtomicBoolean(false);
    private final AtomicBoolean gridRefreshing = new AtomicBoolean(false);

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

        if (!dualMaSnapshotRepository.existsByBatchNoAndMarketAndMaShortAndMaLongAndRecentYears(
                batchNo, market, reqVO.getMaShort(), reqVO.getMaLong(), reqVO.getRecentYears()
        )) {
            return null;
        }

        Sort sort = pageable != null ? pageable.getSort() : Sort.unsorted();
        Pageable queryPageable = buildSnapshotQueryPageable(pageable, sort);

        return dualMaSnapshotRepository.findAll(
                        buildDualMaSnapshotSpec(batchNo, market, reqVO, watchlistCodes, sort),
                        queryPageable
                )
                .map(this::toVO);
    }

    public Page<StockTradeBacktestVO> queryMomentumBacktestSnapshot(
            MomentumBacktestReqVO reqVO,
            Pageable pageable,
            Set<String> watchlistCodes
    ) {
        String market = normalizeMarket(reqVO.getMarket());
        if (!isMomentumPresetRequest(reqVO)) {
            return null;
        }

        Long batchNo = getMomentumLatestBatchNo();
        if (batchNo == null) {
            return null;
        }

        if (!momentumSnapshotRepository.existsByBatchNoAndMarketAndLookbackDaysAndRecentYears(
                batchNo, market, reqVO.getLookbackDays(), reqVO.getRecentYears()
        )) {
            return null;
        }

        Sort sort = pageable != null ? pageable.getSort() : Sort.unsorted();
        Pageable queryPageable = buildSnapshotQueryPageable(pageable, sort);

        return momentumSnapshotRepository.findAll(
                buildMomentumSnapshotSpec(batchNo, market, reqVO, watchlistCodes, sort), queryPageable
        ).map(this::toVO);
    }

    public Page<StockTradeBacktestVO> queryMacdBacktestSnapshot(
            MacdBacktestReqVO reqVO,
            Pageable pageable,
            Set<String> watchlistCodes
    ) {
        String market = normalizeMarket(reqVO.getMarket());
        if (!isMacdPresetRequest(reqVO)) {
            return null;
        }
        Long batchNo = getMacdLatestBatchNo();
        if (batchNo == null || !macdSnapshotRepository
                .existsByBatchNoAndMarketAndFastPeriodAndSlowPeriodAndSignalPeriodAndRecentYears(
                        batchNo, market, reqVO.getFastPeriod(), reqVO.getSlowPeriod(),
                        reqVO.getSignalPeriod(), reqVO.getRecentYears()
                )) {
            return null;
        }
        Sort sort = pageable != null ? pageable.getSort() : Sort.unsorted();
        Pageable queryPageable = buildSnapshotQueryPageable(pageable, sort);
        return macdSnapshotRepository.findAll(
                buildMacdSnapshotSpec(batchNo, market, reqVO, watchlistCodes, sort), queryPageable
        ).map(this::toVO);
    }

    public Page<StockTradeBacktestVO> queryGridBacktestSnapshot(
            GridBacktestReqVO reqVO,
            Pageable pageable,
            Set<String> watchlistCodes
    ) {
        String market = normalizeMarket(reqVO.getMarket());
        if (!isGridPresetRequest(reqVO)) {
            return null;
        }
        Long batchNo = getGridLatestBatchNo();
        if (batchNo == null || !gridSnapshotRepository
                .existsByBatchNoAndMarketAndGridRateAndGridCountAndRecentYears(
                        batchNo, market, reqVO.getGridRate(), reqVO.getGridCount(), reqVO.getRecentYears()
                )) {
            return null;
        }
        Sort sort = pageable != null ? pageable.getSort() : Sort.unsorted();
        Pageable queryPageable = buildSnapshotQueryPageable(pageable, sort);
        return gridSnapshotRepository.findAll(
                buildGridSnapshotSpec(batchNo, market, reqVO, watchlistCodes, sort), queryPageable
        ).map(this::toVO);
    }

    public boolean isPresetRequest(DualMABacktestReqVO reqVO) {
        return isPresetMarket(reqVO.getMarket())
                && isPresetMa(reqVO.getMaShort())
                && isPresetMa(reqVO.getMaLong())
                && reqVO.getMaShort() < reqVO.getMaLong()
                && isPresetRecentYears(reqVO.getRecentYears());
    }

    public boolean isMomentumPresetRequest(MomentumBacktestReqVO reqVO) {
        return isPresetMarket(reqVO.getMarket())
                && isPresetMomentumLookbackDays(reqVO.getLookbackDays())
                && isPresetRecentYears(reqVO.getRecentYears());
    }

    public boolean isMacdPresetRequest(MacdBacktestReqVO reqVO) {
        return isPresetMarket(reqVO.getMarket())
                && Integer.valueOf(PRESET_MACD_FAST_PERIOD).equals(reqVO.getFastPeriod())
                && Integer.valueOf(PRESET_MACD_SLOW_PERIOD).equals(reqVO.getSlowPeriod())
                && Integer.valueOf(PRESET_MACD_SIGNAL_PERIOD).equals(reqVO.getSignalPeriod())
                && isPresetRecentYears(reqVO.getRecentYears());
    }

    public boolean isGridPresetRequest(GridBacktestReqVO reqVO) {
        return isPresetMarket(reqVO.getMarket())
                && PRESET_GRID_RATE.compareTo(reqVO.getGridRate()) == 0
                && Integer.valueOf(PRESET_GRID_COUNT).equals(reqVO.getGridCount())
                && isPresetRecentYears(reqVO.getRecentYears());
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshDualMaBacktestSnapshots() {
        if (!dualMaRefreshing.compareAndSet(false, true)) {
            log.info("双均线回测快照任务已在执行中，本次跳过");
            return;
        }

        long batchNo = System.currentTimeMillis();
        try {
            if (shouldSkipRefreshSnapshots(
                    StockSyncConstant.STOCK_STRATEGY_DUAL_MA_BACKTEST_SNAPSHOT_LATEST,
                    "双均线回测快照"
            )) {
                return;
            }

            List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(MAX_NEED_DAYS);
            if (CollectionUtils.isEmpty(recentDates)) {
                log.warn("双均线回测快照生成跳过，历史行情为空");
                return;
            }

            for (String market : PRESET_MARKETS) {
                refreshDualMaMarketSnapshots(batchNo, market, recentDates);
            }

            activateLatestBatch(
                    batchNo,
                    StockSyncConstant.STOCK_STRATEGY_DUAL_MA_BACKTEST_SNAPSHOT_LATEST
            );
            int limit = 5000;

            while (true) {
                int deleted = dualMaSnapshotRepository.deleteOldBatchLimit(batchNo, limit);

                if (deleted < limit) {
                    break;
                }
            }
            log.info("双均线回测快照生成完成，batchNo={}", batchNo);
        } catch (Exception e) {
            log.error("双均线回测快照生成失败，batchNo={}", batchNo, e);
        } finally {
            dualMaRefreshing.set(false);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void  refreshMomentumBacktestSnapshots() {
        if (!momentumRefreshing.compareAndSet(false, true)) {
            log.info("动量回测快照任务已在执行中，本次跳过");
            return;
        }

        long batchNo = System.currentTimeMillis();
        try {
            if (shouldSkipRefreshSnapshots(
                    StockSyncConstant.STOCK_STRATEGY_MOMENTUM_BACKTEST_SNAPSHOT_LATEST,
                    "动量回测快照"
            )) {
                return;
            }

            List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(MAX_NEED_DAYS);
            if (CollectionUtils.isEmpty(recentDates)) {
                log.warn("动量回测快照生成跳过，历史行情为空");
                return;
            }

            for (String market : PRESET_MARKETS) {
                refreshMomentumMarketSnapshots(batchNo, market, recentDates);
            }

            activateLatestBatch(
                    batchNo,
                    StockSyncConstant.STOCK_STRATEGY_MOMENTUM_BACKTEST_SNAPSHOT_LATEST
            );

            int limit = 5000;

            while (true) {
                int deleted = momentumSnapshotRepository.deleteOldBatchLimit(batchNo, limit);

                if (deleted < limit) {
                    break;
                }
            }

            log.info("动量回测快照生成完成，batchNo={}", batchNo);
        } catch (Exception e) {
            log.error("动量回测快照生成失败，batchNo={}", batchNo, e);
        } finally {
            momentumRefreshing.set(false);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshMacdBacktestSnapshots() {
        if (!macdRefreshing.compareAndSet(false, true)) {
            log.info("MACD回测快照任务已在执行中，本次跳过");
            return;
        }

        long batchNo = System.currentTimeMillis();
        try {
            if (shouldSkipRefreshSnapshots(
                    StockSyncConstant.STOCK_STRATEGY_MACD_BACKTEST_SNAPSHOT_LATEST,
                    "MACD回测快照"
            )) {
                return;
            }
            List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(MAX_NEED_DAYS);
            if (CollectionUtils.isEmpty(recentDates)) {
                log.warn("MACD回测快照生成跳过，历史行情为空");
                return;
            }
            for (String market : PRESET_MARKETS) {
                refreshMacdMarketSnapshots(batchNo, market, recentDates);
            }
            activateLatestBatch(batchNo, StockSyncConstant.STOCK_STRATEGY_MACD_BACKTEST_SNAPSHOT_LATEST);

            int limit = 5000;
            while (true) {
                int deleted = macdSnapshotRepository.deleteOldBatchLimit(batchNo, limit);
                if (deleted < limit) {
                    break;
                }
            }
            log.info("MACD回测快照生成完成，batchNo={}", batchNo);
        } catch (Exception e) {
            log.error("MACD回测快照生成失败，batchNo={}", batchNo, e);
        } finally {
            macdRefreshing.set(false);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshGridBacktestSnapshots() {
        if (!gridRefreshing.compareAndSet(false, true)) {
            log.info("网格交易回测快照任务已在执行中，本次跳过");
            return;
        }

        long batchNo = System.currentTimeMillis();
        try {
            if (shouldSkipRefreshSnapshots(
                    StockSyncConstant.STOCK_STRATEGY_GRID_BACKTEST_SNAPSHOT_LATEST,
                    "网格交易回测快照"
            )) {
                return;
            }
            List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(MAX_NEED_DAYS);
            if (CollectionUtils.isEmpty(recentDates)) {
                log.warn("网格交易回测快照生成跳过，历史行情为空");
                return;
            }
            for (String market : PRESET_MARKETS) {
                refreshGridMarketSnapshots(batchNo, market, recentDates);
            }
            activateLatestBatch(batchNo, StockSyncConstant.STOCK_STRATEGY_GRID_BACKTEST_SNAPSHOT_LATEST);

            int limit = 5000;
            while (true) {
                int deleted = gridSnapshotRepository.deleteOldBatchLimit(batchNo, limit);
                if (deleted < limit) {
                    break;
                }
            }
            log.info("网格交易回测快照生成完成，batchNo={}", batchNo);
        } catch (Exception e) {
            log.error("网格交易回测快照生成失败，batchNo={}", batchNo, e);
        } finally {
            gridRefreshing.set(false);
        }
    }

    private void refreshDualMaMarketSnapshots(Long batchNo, String market, List<String> recentDates) {
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

            dualMaSnapshotRepository.saveAll(snapshots);
            log.info("双均线回测快照已生成，market={}, batchNo={}, progress={}/{}", market, batchNo,
                    Math.min(b + SNAPSHOT_BATCH_SIZE, stocks.size()), stocks.size());
        }
    }

    private void refreshMomentumMarketSnapshots(Long batchNo, String market, List<String> recentDates) {
        List<StockQuote> stocks = stockQuoteRepository.findByCodeStartingWithIgnoreCase(market);
        if (CollectionUtils.isEmpty(stocks)) {
            log.info("市场 {} 无股票数据，跳过动量回测快照", market);
            return;
        }

        for (int b = 0; b < stocks.size(); b += SNAPSHOT_BATCH_SIZE) {
            List<StockQuote> batch = stocks.subList(b, Math.min(stocks.size(), b + SNAPSHOT_BATCH_SIZE));
            List<String> codes = batch.stream().map(StockQuote::getCode).toList();
            List<StockQuoteHistoryProjection> histories = stockQuoteHistoryRepository
                    .findByTradeDateInAndCodeInOrderByTradeDateAsc(recentDates, codes);

            var historyMap = momentumStrategy.groupHistoriesByCode(histories);
            List<StockStrategyMomentumBacktestSnapshot> snapshots = new ArrayList<>();
            TTest tTest = new TTest();

            for (StockQuote stock : batch) {
                List<StockQuoteHistoryProjection> stockHistories = historyMap.getOrDefault(stock.getCode(), Collections.emptyList());
                BigDecimal[] closePrices = momentumStrategy.extractClosePrices(stockHistories);

                for (int recentYears : PRESET_RECENT_YEARS) {
                    for (int lookbackDays : PRESET_MOMENTUM_LOOKBACK_DAY_OPTIONS) {
                        StockTradeBacktestVO vo = momentumStrategy.backtestSingle(
                                stock, closePrices, lookbackDays, recentYears, tTest
                        );
                        snapshots.add(toSnapshot(batchNo, market, lookbackDays, recentYears, vo));
                    }
                }
            }

            for (StockStrategyMomentumBacktestSnapshot snapshot : snapshots) {
                if ((snapshot.getTValue() != null && !Double.isFinite(snapshot.getTValue()))
                        || (snapshot.getPValue() != null && !Double.isFinite(snapshot.getPValue()))) {
                    log.warn("invalid momentum snapshot: market={}, code={}, lookbackDays={}, recentYears={}, tValue={}, pValue={}",
                            snapshot.getMarket(), snapshot.getCode(), snapshot.getLookbackDays(),
                            snapshot.getRecentYears(), snapshot.getTValue(), snapshot.getPValue());
                }
            }

            momentumSnapshotRepository.saveAll(snapshots);
            log.info("动量回测快照已生成，market={}, batchNo={}, progress={}/{}", market, batchNo,
                    Math.min(b + SNAPSHOT_BATCH_SIZE, stocks.size()), stocks.size());
        }
    }

    private void refreshMacdMarketSnapshots(Long batchNo, String market, List<String> recentDates) {
        List<StockQuote> stocks = stockQuoteRepository.findByCodeStartingWithIgnoreCase(market);
        if (CollectionUtils.isEmpty(stocks)) {
            log.info("市场 {} 无股票数据，跳过MACD回测快照", market);
            return;
        }

        for (int batchStart = 0; batchStart < stocks.size(); batchStart += SNAPSHOT_BATCH_SIZE) {
            List<StockQuote> batch = stocks.subList(
                    batchStart, Math.min(stocks.size(), batchStart + SNAPSHOT_BATCH_SIZE)
            );
            List<String> codes = batch.stream().map(StockQuote::getCode).toList();
            List<StockQuoteHistoryProjection> histories = stockQuoteHistoryRepository
                    .findByTradeDateInAndCodeInOrderByTradeDateAsc(recentDates, codes);
            var historyMap = macdStrategy.groupHistoriesByCode(histories);
            List<StockStrategyMacdBacktestSnapshot> snapshots = new ArrayList<>();
            TTest tTest = new TTest();

            for (StockQuote stock : batch) {
                BigDecimal[] closePrices = macdStrategy.extractClosePrices(
                        historyMap.getOrDefault(stock.getCode(), Collections.emptyList())
                );
                for (int recentYears : PRESET_RECENT_YEARS) {
                    StockTradeBacktestVO vo = macdStrategy.backtestSingle(
                            stock, closePrices, PRESET_MACD_FAST_PERIOD, PRESET_MACD_SLOW_PERIOD,
                            PRESET_MACD_SIGNAL_PERIOD, recentYears, tTest
                    );
                    snapshots.add(toSnapshot(batchNo, market, recentYears, vo));
                }
            }
            macdSnapshotRepository.saveAll(snapshots);
            log.info("MACD回测快照已生成，market={}, batchNo={}, progress={}/{}", market, batchNo,
                    Math.min(batchStart + SNAPSHOT_BATCH_SIZE, stocks.size()), stocks.size());
        }
    }

    private void refreshGridMarketSnapshots(Long batchNo, String market, List<String> recentDates) {
        List<StockQuote> stocks = stockQuoteRepository.findByCodeStartingWithIgnoreCase(market);
        if (CollectionUtils.isEmpty(stocks)) {
            log.info("市场 {} 无股票数据，跳过网格交易回测快照", market);
            return;
        }

        for (int batchStart = 0; batchStart < stocks.size(); batchStart += SNAPSHOT_BATCH_SIZE) {
            List<StockQuote> batch = stocks.subList(
                    batchStart, Math.min(stocks.size(), batchStart + SNAPSHOT_BATCH_SIZE)
            );
            List<String> codes = batch.stream().map(StockQuote::getCode).toList();
            List<StockQuoteHistoryProjection> histories = stockQuoteHistoryRepository
                    .findByTradeDateInAndCodeInOrderByTradeDateAsc(recentDates, codes);
            var historyMap = gridTradingStrategy.groupHistoriesByCode(histories);
            List<StockStrategyGridBacktestSnapshot> snapshots = new ArrayList<>();
            TTest tTest = new TTest();

            for (StockQuote stock : batch) {
                BigDecimal[] closePrices = gridTradingStrategy.extractClosePrices(
                        historyMap.getOrDefault(stock.getCode(), Collections.emptyList())
                );
                for (int recentYears : PRESET_RECENT_YEARS) {
                    StockTradeBacktestVO vo = gridTradingStrategy.backtestSingle(
                            stock, closePrices, PRESET_GRID_RATE, PRESET_GRID_COUNT, recentYears, tTest
                    );
                    snapshots.add(toGridSnapshot(batchNo, market, recentYears, vo));
                }
            }
            gridSnapshotRepository.saveAll(snapshots);
            log.info("网格交易回测快照已生成，market={}, batchNo={}, progress={}/{}", market, batchNo,
                    Math.min(batchStart + SNAPSHOT_BATCH_SIZE, stocks.size()), stocks.size());
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

    private StockStrategyMomentumBacktestSnapshot toSnapshot(
            Long batchNo,
            String market,
            int lookbackDays,
            int recentYears,
            StockTradeBacktestVO vo
    ) {
        StockStrategyMomentumBacktestSnapshot snapshot = new StockStrategyMomentumBacktestSnapshot();
        snapshot.setBatchNo(batchNo);
        snapshot.setMarket(market);
        snapshot.setCode(vo.getCode());
        snapshot.setName(vo.getName());
        snapshot.setLookbackDays(lookbackDays);
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

    private StockTradeBacktestVO toVO(StockStrategyMomentumBacktestSnapshot snapshot) {
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

    private StockStrategyMacdBacktestSnapshot toSnapshot(
            Long batchNo,
            String market,
            int recentYears,
            StockTradeBacktestVO vo
    ) {
        StockStrategyMacdBacktestSnapshot snapshot = new StockStrategyMacdBacktestSnapshot();
        snapshot.setBatchNo(batchNo);
        snapshot.setMarket(market);
        snapshot.setCode(vo.getCode());
        snapshot.setName(vo.getName());
        snapshot.setFastPeriod(PRESET_MACD_FAST_PERIOD);
        snapshot.setSlowPeriod(PRESET_MACD_SLOW_PERIOD);
        snapshot.setSignalPeriod(PRESET_MACD_SIGNAL_PERIOD);
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

    private StockTradeBacktestVO toVO(StockStrategyMacdBacktestSnapshot snapshot) {
        return new StockTradeBacktestVO(
                snapshot.getCode(), snapshot.getName(), snapshot.getTotalReturn(), snapshot.getTradeCount(),
                snapshot.getWinRate(), snapshot.getTValue(), snapshot.getPValue(), snapshot.getReliability(),
                snapshot.getLatestPrice(), snapshot.getPir(), snapshot.getCreatedAt()
        );
    }

    private StockStrategyGridBacktestSnapshot toGridSnapshot(
            Long batchNo,
            String market,
            int recentYears,
            StockTradeBacktestVO vo
    ) {
        StockStrategyGridBacktestSnapshot snapshot = new StockStrategyGridBacktestSnapshot();
        snapshot.setBatchNo(batchNo);
        snapshot.setMarket(market);
        snapshot.setCode(vo.getCode());
        snapshot.setName(vo.getName());
        snapshot.setGridRate(PRESET_GRID_RATE);
        snapshot.setGridCount(PRESET_GRID_COUNT);
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

    private StockTradeBacktestVO toVO(StockStrategyGridBacktestSnapshot snapshot) {
        return new StockTradeBacktestVO(
                snapshot.getCode(), snapshot.getName(), snapshot.getTotalReturn(), snapshot.getTradeCount(),
                snapshot.getWinRate(), snapshot.getTValue(), snapshot.getPValue(), snapshot.getReliability(),
                snapshot.getLatestPrice(), snapshot.getPir(), snapshot.getCreatedAt()
        );
    }

    private Specification<StockStrategyDualMaBacktestSnapshot> buildDualMaSnapshotSpec(
            Long batchNo,
            String market,
            DualMABacktestReqVO reqVO,
            Set<String> watchlistCodes,
            Sort sort
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
                predicates.add(cb.equal(root.get(RELIABILITY), reqVO.getReliability()));
            }

            if (watchlistCodes != null) {
                List<Predicate> orPredicates = new ArrayList<>();
                for (String watchlistCode : watchlistCodes) {
                    orPredicates.add(cb.like(root.get("code"), "%" + watchlistCode));
                }
                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }

            applyCustomSnapshotOrdering(root, query, cb, sort);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<StockStrategyMomentumBacktestSnapshot> buildMomentumSnapshotSpec(
            Long batchNo,
            String market,
            MomentumBacktestReqVO reqVO,
            Set<String> watchlistCodes,
            Sort sort
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("batchNo"), batchNo));
            predicates.add(cb.equal(root.get("market"), market));
            predicates.add(cb.equal(root.get("lookbackDays"), reqVO.getLookbackDays()));
            predicates.add(cb.equal(root.get("recentYears"), reqVO.getRecentYears()));

            if (StringUtils.isNotBlank(reqVO.getCode())) {
                predicates.add(cb.equal(root.get("code"), reqVO.getCode()));
            }

            if (StringUtils.isNotBlank(reqVO.getReliability())) {
                predicates.add(cb.equal(root.get(RELIABILITY), reqVO.getReliability()));
            }

            if (watchlistCodes != null) {
                List<Predicate> orPredicates = new ArrayList<>();
                for (String watchlistCode : watchlistCodes) {
                    orPredicates.add(cb.like(root.get("code"), "%" + watchlistCode));
                }
                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }

            applyCustomSnapshotOrdering(root, query, cb, sort);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<StockStrategyMacdBacktestSnapshot> buildMacdSnapshotSpec(
            Long batchNo,
            String market,
            MacdBacktestReqVO reqVO,
            Set<String> watchlistCodes,
            Sort sort
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("batchNo"), batchNo));
            predicates.add(cb.equal(root.get("market"), market));
            predicates.add(cb.equal(root.get("fastPeriod"), reqVO.getFastPeriod()));
            predicates.add(cb.equal(root.get("slowPeriod"), reqVO.getSlowPeriod()));
            predicates.add(cb.equal(root.get("signalPeriod"), reqVO.getSignalPeriod()));
            predicates.add(cb.equal(root.get("recentYears"), reqVO.getRecentYears()));
            if (StringUtils.isNotBlank(reqVO.getCode())) {
                predicates.add(cb.equal(root.get("code"), reqVO.getCode()));
            }
            if (StringUtils.isNotBlank(reqVO.getReliability())) {
                predicates.add(cb.equal(root.get(RELIABILITY), reqVO.getReliability()));
            }
            if (watchlistCodes != null) {
                List<Predicate> orPredicates = new ArrayList<>();
                for (String watchlistCode : watchlistCodes) {
                    orPredicates.add(cb.like(root.get("code"), "%" + watchlistCode));
                }
                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }
            applyCustomSnapshotOrdering(root, query, cb, sort);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<StockStrategyGridBacktestSnapshot> buildGridSnapshotSpec(
            Long batchNo,
            String market,
            GridBacktestReqVO reqVO,
            Set<String> watchlistCodes,
            Sort sort
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("batchNo"), batchNo));
            predicates.add(cb.equal(root.get("market"), market));
            predicates.add(cb.equal(root.get("gridRate"), reqVO.getGridRate()));
            predicates.add(cb.equal(root.get("gridCount"), reqVO.getGridCount()));
            predicates.add(cb.equal(root.get("recentYears"), reqVO.getRecentYears()));
            if (StringUtils.isNotBlank(reqVO.getCode())) {
                predicates.add(cb.equal(root.get("code"), reqVO.getCode()));
            }
            if (StringUtils.isNotBlank(reqVO.getReliability())) {
                predicates.add(cb.equal(root.get(RELIABILITY), reqVO.getReliability()));
            }
            if (watchlistCodes != null) {
                List<Predicate> orPredicates = new ArrayList<>();
                for (String watchlistCode : watchlistCodes) {
                    orPredicates.add(cb.like(root.get("code"), "%" + watchlistCode));
                }
                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }
            applyCustomSnapshotOrdering(root, query, cb, sort);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private <T> void applyCustomSnapshotOrdering(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Sort sort
    ) {
        if (!requiresCustomPValueOrdering(sort)) {
            return;
        }

        List<Order> orders = new ArrayList<>();
        for (Sort.Order sortOrder : sort) {
            String property = mapSnapshotSortProperty(sortOrder.getProperty());
            if (property == null) {
                continue;
            }

            if (P_VALUE.equals(property)) {
                Expression<Integer> nullRank = cb.<Integer>selectCase()
                        .when(cb.isNull(root.get(P_VALUE)), 1)
                        .otherwise(0);
                orders.add(cb.asc(nullRank));
                orders.add(sortOrder.getDirection() == Sort.Direction.DESC
                        ? cb.desc(root.get(P_VALUE))
                        : cb.asc(root.get(P_VALUE)));
            } else {
                orders.add(sortOrder.getDirection() == Sort.Direction.DESC
                        ? cb.desc(root.get(property))
                        : cb.asc(root.get(property)));
            }
        }

        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }
    }

    private boolean requiresCustomPValueOrdering(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return false;
        }
        for (Sort.Order order : sort) {
            if (P_VALUE.equals(order.getProperty())) {
                return true;
            }
        }
        return false;
    }

    private Pageable buildSnapshotQueryPageable(Pageable pageable, Sort sort) {
        if (!requiresCustomPValueOrdering(sort)) {
            return pageable;
        }
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private String mapSnapshotSortProperty(String property) {
        if (StringUtils.isBlank(property)) {
            return null;
        }
        return switch (property) {
            case "code", "name", "totalReturn", "tradeCount", "winRate",
                 P_VALUE, "latestPrice", "pir", RELIABILITY -> property;
            case "lastTime" -> "createdAt";
            default -> null;
        };
    }

    private Long getDualMaLatestBatchNo() {
        return getSyncTimestamp(StockSyncConstant.STOCK_STRATEGY_DUAL_MA_BACKTEST_SNAPSHOT_LATEST);
    }

    private Long getMomentumLatestBatchNo() {
        return getSyncTimestamp(StockSyncConstant.STOCK_STRATEGY_MOMENTUM_BACKTEST_SNAPSHOT_LATEST);
    }

    private Long getMacdLatestBatchNo() {
        return getSyncTimestamp(StockSyncConstant.STOCK_STRATEGY_MACD_BACKTEST_SNAPSHOT_LATEST);
    }

    private Long getGridLatestBatchNo() {
        return getSyncTimestamp(StockSyncConstant.STOCK_STRATEGY_GRID_BACKTEST_SNAPSHOT_LATEST);
    }

    private Long getLatestBatchNo() {
        return getDualMaLatestBatchNo();
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

    private boolean isPresetMomentumLookbackDays(Integer lookbackDays) {
        if (lookbackDays == null) {
            return false;
        }
        for (int preset : PRESET_MOMENTUM_LOOKBACK_DAY_OPTIONS) {
            if (preset == lookbackDays) {
                return true;
            }
        }
        return false;
    }

    private void activateLatestBatch(Long batchNo, String syncName) {
        StockSync stockSync = stockSyncRepository.findByName(syncName);
        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName(syncName);
        }
        stockSync.setValue(String.valueOf(batchNo));
        stockSyncRepository.save(stockSync);
    }

    private boolean shouldSkipRefreshSnapshots(String latestSnapshotSyncName, String snapshotLabel) {
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

        Long latestBatchNo = getSyncTimestamp(latestSnapshotSyncName);
        if (latestBatchNo == null || latestBatchNo < stockDailyLatest) {
            return false;
        }

        log.info("{}已覆盖最近交易日收盘数据，跳过生成。latestTradeDay={}, stockDailyLatest={}, latestBatchNo={}",
                snapshotLabel, latestTradeDay, stockDailyLatest, latestBatchNo);
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
