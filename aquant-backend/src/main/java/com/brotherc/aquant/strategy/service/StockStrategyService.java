package com.brotherc.aquant.strategy.service;

import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.strategy.model.vo.DualMAReqVO;
import com.brotherc.aquant.strategy.model.vo.DualMABacktestReqVO;
import com.brotherc.aquant.strategy.model.vo.MomentumReqVO;
import com.brotherc.aquant.strategy.model.vo.MomentumBacktestReqVO;
import com.brotherc.aquant.strategy.model.vo.MacdReqVO;
import com.brotherc.aquant.strategy.model.vo.MacdBacktestReqVO;
import com.brotherc.aquant.strategy.model.vo.StockTradeSignalVO;
import com.brotherc.aquant.strategy.model.vo.StockTradeBacktestVO;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.watchlist.repository.StockWatchlistGroupRepository;
import com.brotherc.aquant.watchlist.repository.StockWatchlistStockRepository;
import com.brotherc.aquant.watchlist.entity.StockWatchlistStock;
import com.brotherc.aquant.common.utils.UserContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockStrategyService {

    private static final String SIGNAL = "signal";
    private static final String LATEST_PRICE = "latestPrice";

    private final DualMovingAverageStrategy dualMovingAverageStrategy;
    private final MomentumStrategy momentumStrategy;
    private final MacdStrategy macdStrategy;
    private final StockWatchlistStockRepository stockWatchlistStockRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockWatchlistGroupRepository stockWatchlistGroupRepository;
    private final StockStrategySnapshotService stockStrategySnapshotService;

    public Page<StockTradeSignalVO> dualMA(DualMAReqVO reqVO, Pageable pageable) {
        Set<String> watchlistCodes = null;
        if (reqVO.getWatchlistGroupId() != null) {
            Long userId = UserContext.requireCurrentUserId();
            stockWatchlistGroupRepository.findByIdAndUserId(reqVO.getWatchlistGroupId(), userId)
                    .orElseThrow(() -> new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND));
            watchlistCodes = stockWatchlistStockRepository
                    .findByGroupIdOrderBySortNoDesc(reqVO.getWatchlistGroupId())
                    .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(watchlistCodes)) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
        }

        if (StringUtils.isBlank(reqVO.getSignal())) {
            Specification<StockQuote> stockQuoteSpecification = buildStockQuoteSpec(reqVO.getCode(), watchlistCodes, reqVO.getMarket());
            Page<StockQuote> pagedStocks = stockQuoteRepository.findAll(stockQuoteSpecification, pageable);
            if (pagedStocks.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            List<StockTradeSignalVO> pagedList = dualMovingAverageStrategy.calculate(
                    reqVO.getMaShort(), reqVO.getMaLong(), pagedStocks.getContent()
            );
            return new PageImpl<>(pagedList, pageable, pagedStocks.getTotalElements());
        }

        List<StockQuote> allStocks = stockQuoteRepository.findAll();
        Stream<StockQuote> quoteStream = allStocks.stream();

        if (StringUtils.isNotBlank(reqVO.getMarket())) {
            final String market = reqVO.getMarket().toLowerCase();
            quoteStream = quoteStream.filter(vo -> vo.getCode() != null && vo.getCode().toLowerCase().startsWith(market));
        }

        if (StringUtils.isNotBlank(reqVO.getCode())) {
            quoteStream = quoteStream.filter(vo -> reqVO.getCode().equalsIgnoreCase(vo.getCode()));
        }

        if (watchlistCodes != null) {
            final Set<String> wc = watchlistCodes;
            quoteStream = quoteStream.filter(vo -> {
                String c = vo.getCode();
                String c6 = c.length() > 6 ? c.substring(c.length() - 6) : c;
                return wc.contains(c6);
            });
        }

        List<StockQuote> targetStocks = quoteStream.toList();
        if (targetStocks.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<StockTradeSignalVO> list = dualMovingAverageStrategy.calculate(reqVO.getMaShort(), reqVO.getMaLong(), targetStocks);

        if (StringUtils.isNotBlank(reqVO.getSignal())) {
            list = list.stream().filter(vo -> reqVO.getSignal().equalsIgnoreCase(vo.getSignal())).collect(Collectors.toList());
        }

        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            list = new ArrayList<>(list);
            list.sort(buildComparator(sort));
        }

        int total = list.size();
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int fromIndex = currentPage * pageSize;

        if (fromIndex >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }

        int toIndex = Math.min(fromIndex + pageSize, total);
        return new PageImpl<>(list.subList(fromIndex, toIndex), pageable, total);
    }

    private Comparator<StockTradeSignalVO> buildComparator(Sort sort) {
        Comparator<StockTradeSignalVO> result = null;

        for (Sort.Order order : sort) {
            Comparator<StockTradeSignalVO> comparator = null;
            if ("code".equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeSignalVO::getCode);
            } else if ("name".equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeSignalVO::getName);
            } else if (SIGNAL.equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeSignalVO::getSignal);
            } else if ("pir".equals(order.getProperty())) {
                comparator = Comparator.comparing(
                        StockTradeSignalVO::getPir,
                        Comparator.nullsLast(BigDecimal::compareTo));
            } else if (LATEST_PRICE.equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeSignalVO::getLatestPrice);
            }

            if (comparator == null) {
                continue;
            }

            if (order.getDirection() == Sort.Direction.DESC) {
                comparator = comparator.reversed();
            }

            result = (result == null) ? comparator : result.thenComparing(comparator);
        }

        return result != null ? result : Comparator.comparing(StockTradeSignalVO::getCode);
    }

    public Page<StockTradeBacktestVO> dualMABacktest(DualMABacktestReqVO reqVO, Pageable pageable) {
        java.util.Set<String> watchlistCodes = null;
        if (reqVO.getWatchlistGroupId() != null) {
            Long userId = UserContext.requireCurrentUserId();
            stockWatchlistGroupRepository.findByIdAndUserId(reqVO.getWatchlistGroupId(), userId)
                    .orElseThrow(() -> new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND));
            watchlistCodes = stockWatchlistStockRepository
                    .findByGroupIdOrderBySortNoDesc(reqVO.getWatchlistGroupId())
                    .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(watchlistCodes)) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
        }

        Page<StockTradeBacktestVO> snapshotPage = stockStrategySnapshotService
                .queryDualMABacktestSnapshot(reqVO, pageable, watchlistCodes);
        if (snapshotPage != null) {
            return snapshotPage;
        }

        return dualMABacktestOnline(reqVO, pageable, watchlistCodes);
    }

    private Page<StockTradeBacktestVO> dualMABacktestOnline(
            DualMABacktestReqVO reqVO,
            Pageable pageable,
            Set<String> watchlistCodes
    ) {
        boolean earlyPaginate = StringUtils.isBlank(reqVO.getReliability()) && !hasStrategySortFields(pageable.getSort());
        if (earlyPaginate) {
            Page<StockQuote> pagedStocks = stockQuoteRepository.findAll(buildStockQuoteSpec(reqVO.getCode(), watchlistCodes, reqVO.getMarket()), pageable);
            if (pagedStocks.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            List<StockTradeBacktestVO> pagedList = dualMovingAverageStrategy.backtest(
                    reqVO.getMaShort(), reqVO.getMaLong(), reqVO.getRecentYears(), pagedStocks.getContent());
            return new PageImpl<>(pagedList, pageable, pagedStocks.getTotalElements());
        }

        List<StockQuote> targetStocks = stockQuoteRepository.findAll(
                buildStockQuoteSpec(reqVO.getCode(), watchlistCodes, reqVO.getMarket())
        );
        if (targetStocks.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<StockTradeBacktestVO> result = dualMovingAverageStrategy.backtest(
                reqVO.getMaShort(), reqVO.getMaLong(), reqVO.getRecentYears(), targetStocks);

        if (StringUtils.isNotBlank(reqVO.getReliability())) {
            result = result.stream()
                    .filter(vo -> reqVO.getReliability().equals(vo.getReliability()))
                    .collect(Collectors.toList());
        }

        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            result = new ArrayList<>(result);
            result.sort(buildBacktestComparator(sort));
        }

        int total = result.size();
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int fromIndex = currentPage * pageSize;

        if (fromIndex >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }

        int toIndex = Math.min(fromIndex + pageSize, total);
        return new PageImpl<>(result.subList(fromIndex, toIndex), pageable, total);
    }

    private Comparator<StockTradeBacktestVO> buildBacktestComparator(Sort sort) {
        Comparator<StockTradeBacktestVO> result = null;

        for (Sort.Order order : sort) {
            Comparator<StockTradeBacktestVO> comparator = null;
            boolean reverse = order.getDirection() == Sort.Direction.DESC;
            if ("code".equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeBacktestVO::getCode);
            } else if ("name".equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeBacktestVO::getName);
            } else if ("totalReturn".equals(order.getProperty())) {
                comparator = Comparator.comparing(
                        StockTradeBacktestVO::getTotalReturn,
                        Comparator.nullsLast(BigDecimal::compareTo));
            } else if ("tradeCount".equals(order.getProperty())) {
                comparator = Comparator.comparing(
                        StockTradeBacktestVO::getTradeCount,
                        Comparator.nullsLast(Integer::compareTo));
            } else if ("winRate".equals(order.getProperty())) {
                comparator = Comparator.comparing(
                        StockTradeBacktestVO::getWinRate,
                        Comparator.nullsLast(BigDecimal::compareTo));
            } else if ("pValue".equals(order.getProperty())) {
                comparator = Comparator.comparing(
                        StockTradeBacktestVO::getPValue,
                        reverse ? Comparator.nullsLast(Comparator.reverseOrder()) : Comparator.nullsLast(Double::compareTo));
                reverse = false;
            } else if (LATEST_PRICE.equals(order.getProperty())) {
                comparator = Comparator.comparing(
                        StockTradeBacktestVO::getLatestPrice,
                        Comparator.nullsLast(BigDecimal::compareTo));
            }

            if (comparator == null) {
                continue;
            }

            if (reverse) {
                comparator = comparator.reversed();
            }

            result = (result == null) ? comparator : result.thenComparing(comparator);
        }

        return result != null ? result : Comparator.comparing(StockTradeBacktestVO::getCode);
    }

    // ==================== 动量策略 ====================

    public Page<StockTradeSignalVO> momentum(MomentumReqVO reqVO, Pageable pageable) {
        java.util.Set<String> watchlistCodes = null;
        if (reqVO.getWatchlistGroupId() != null) {
            Long userId = UserContext.requireCurrentUserId();
            stockWatchlistGroupRepository.findByIdAndUserId(reqVO.getWatchlistGroupId(), userId)
                    .orElseThrow(() -> new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND));
            watchlistCodes = stockWatchlistStockRepository
                    .findByGroupIdOrderBySortNoDesc(reqVO.getWatchlistGroupId())
                    .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(watchlistCodes)) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
        }

        boolean earlyPaginate = StringUtils.isBlank(reqVO.getSignal()) && !hasStrategySortFields(pageable.getSort());
        if (earlyPaginate) {
            Page<StockQuote> pagedStocks = stockQuoteRepository.findAll(buildStockQuoteSpec(reqVO.getCode(), watchlistCodes, reqVO.getMarket()), pageable);
            if (pagedStocks.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            List<StockTradeSignalVO> pagedList = momentumStrategy.calculate(reqVO.getLookbackDays(), reqVO.getThreshold(), pagedStocks.getContent());
            return new PageImpl<>(pagedList, pageable, pagedStocks.getTotalElements());
        }

        List<StockQuote> allStocks = stockQuoteRepository.findAll();
        Stream<StockQuote> quoteStream = allStocks.stream();

        if (StringUtils.isNotBlank(reqVO.getMarket())) {
            final String market = reqVO.getMarket().toLowerCase();
            quoteStream = quoteStream.filter(vo -> vo.getCode() != null && vo.getCode().toLowerCase().startsWith(market));
        }

        if (StringUtils.isNotBlank(reqVO.getCode())) {
            quoteStream = quoteStream.filter(vo -> reqVO.getCode().equalsIgnoreCase(vo.getCode()));
        }

        if (watchlistCodes != null) {
            final java.util.Set<String> wc = watchlistCodes;
            quoteStream = quoteStream.filter(vo -> {
                String c = vo.getCode();
                String c6 = c.length() > 6 ? c.substring(c.length() - 6) : c;
                return wc.contains(c6);
            });
        }

        List<StockQuote> targetStocks = quoteStream.toList();
        if (targetStocks.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<StockTradeSignalVO> list = momentumStrategy.calculate(reqVO.getLookbackDays(), reqVO.getThreshold(), targetStocks);

        if (StringUtils.isNotBlank(reqVO.getSignal())) {
            list = list.stream().filter(vo -> reqVO.getSignal().equalsIgnoreCase(vo.getSignal())).toList();
        }

        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            list = new java.util.ArrayList<>(list);
            list.sort(buildMomentumSignalComparator(sort));
        }

        int total = list.size();
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int fromIndex = currentPage * pageSize;

        if (fromIndex >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }

        int toIndex = Math.min(fromIndex + pageSize, total);
        return new PageImpl<>(list.subList(fromIndex, toIndex), pageable, total);
    }

    private Comparator<StockTradeSignalVO> buildMomentumSignalComparator(Sort sort) {
        Comparator<StockTradeSignalVO> result = null;

        for (Sort.Order order : sort) {
            Comparator<StockTradeSignalVO> comparator = null;
            if ("code".equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeSignalVO::getCode);
            } else if ("name".equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeSignalVO::getName);
            } else if (SIGNAL.equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeSignalVO::getSignal);
            } else if ("pir".equals(order.getProperty())) {
                comparator = Comparator.comparing(
                        StockTradeSignalVO::getPir,
                        Comparator.nullsLast(BigDecimal::compareTo));
            } else if (LATEST_PRICE.equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeSignalVO::getLatestPrice);
            } else if ("momentumValue".equals(order.getProperty())) {
                comparator = Comparator.comparing(
                        StockTradeSignalVO::getMomentumValue,
                        Comparator.nullsLast(BigDecimal::compareTo));
            }

            if (comparator == null) continue;
            if (order.getDirection() == Sort.Direction.DESC) comparator = comparator.reversed();
            result = (result == null) ? comparator : result.thenComparing(comparator);
        }

        return result != null ? result : Comparator.comparing(StockTradeSignalVO::getCode);
    }

    public Page<StockTradeBacktestVO> momentumBacktest(MomentumBacktestReqVO reqVO, Pageable pageable) {
        Set<String> watchlistCodes = null;
        if (reqVO.getWatchlistGroupId() != null) {
            Long userId = UserContext.requireCurrentUserId();
            stockWatchlistGroupRepository.findByIdAndUserId(reqVO.getWatchlistGroupId(), userId)
                    .orElseThrow(() -> new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND));
            watchlistCodes = stockWatchlistStockRepository
                    .findByGroupIdOrderBySortNoDesc(reqVO.getWatchlistGroupId())
                    .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(watchlistCodes)) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
        }

        Page<StockTradeBacktestVO> snapshotPage = stockStrategySnapshotService
                .queryMomentumBacktestSnapshot(reqVO, pageable, watchlistCodes);
        if (snapshotPage != null) {
            return snapshotPage;
        }

        boolean earlyPaginate = StringUtils.isBlank(reqVO.getReliability()) && !hasStrategySortFields(pageable.getSort());
        if (earlyPaginate) {
            Page<StockQuote> pagedStocks = stockQuoteRepository.findAll(buildStockQuoteSpec(reqVO.getCode(), watchlistCodes, reqVO.getMarket()), pageable);
            if (pagedStocks.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            List<StockTradeBacktestVO> pagedList = momentumStrategy.backtest(
                    reqVO.getLookbackDays(), reqVO.getRecentYears(), pagedStocks.getContent());
            return new PageImpl<>(pagedList, pageable, pagedStocks.getTotalElements());
        }

        List<StockQuote> stocks = stockQuoteRepository.findAll();
        Stream<StockQuote> stream = stocks.stream();

        if (StringUtils.isNotBlank(reqVO.getMarket())) {
            final String market = reqVO.getMarket().toLowerCase();
            stream = stream.filter(vo -> vo.getCode() != null && vo.getCode().toLowerCase().startsWith(market));
        }

        if (StringUtils.isNotBlank(reqVO.getCode())) {
            stream = stream.filter(vo -> reqVO.getCode().equalsIgnoreCase(vo.getCode()));
        }

        if (watchlistCodes != null) {
            final java.util.Set<String> wc = watchlistCodes;
            stream = stream.filter(vo -> {
                String c = vo.getCode();
                String c6 = c.length() > 6 ? c.substring(c.length() - 6) : c;
                return wc.contains(c6);
            });
        }

        List<StockQuote> targetStocks = stream.toList();
        if (targetStocks.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<StockTradeBacktestVO> result = momentumStrategy.backtest(
                reqVO.getLookbackDays(), reqVO.getRecentYears(), targetStocks);

        if (StringUtils.isNotBlank(reqVO.getReliability())) {
            result = result.stream()
                    .filter(vo -> reqVO.getReliability().equals(vo.getReliability()))
                    .collect(Collectors.toList());
        }

        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            result = new ArrayList<>(result);
            result.sort(buildBacktestComparator(sort));
        }

        int total = result.size();
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int fromIndex = currentPage * pageSize;

        if (fromIndex >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }

        int toIndex = Math.min(fromIndex + pageSize, total);
        return new PageImpl<>(result.subList(fromIndex, toIndex), pageable, total);
    }

    // ==================== MACD策略 ====================

    public Page<StockTradeSignalVO> macd(MacdReqVO reqVO, Pageable pageable) {
        Set<String> watchlistCodes = loadWatchlistCodes(reqVO.getWatchlistGroupId());
        if (watchlistCodes != null && watchlistCodes.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        boolean earlyPaginate = StringUtils.isBlank(reqVO.getSignal()) && !hasStrategySortFields(pageable.getSort());
        if (earlyPaginate) {
            Page<StockQuote> pagedStocks = stockQuoteRepository.findAll(
                    buildStockQuoteSpec(reqVO.getCode(), watchlistCodes, reqVO.getMarket()), pageable
            );
            if (pagedStocks.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            List<StockTradeSignalVO> pagedList = macdStrategy.calculate(
                    reqVO.getFastPeriod(), reqVO.getSlowPeriod(), reqVO.getSignalPeriod(), pagedStocks.getContent()
            );
            return new PageImpl<>(pagedList, pageable, pagedStocks.getTotalElements());
        }

        List<StockQuote> targetStocks = stockQuoteRepository.findAll(
                buildStockQuoteSpec(reqVO.getCode(), watchlistCodes, reqVO.getMarket())
        );
        if (targetStocks.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<StockTradeSignalVO> result = macdStrategy.calculate(
                reqVO.getFastPeriod(), reqVO.getSlowPeriod(), reqVO.getSignalPeriod(), targetStocks
        );
        if (StringUtils.isNotBlank(reqVO.getSignal())) {
            result = result.stream()
                    .filter(item -> reqVO.getSignal().equalsIgnoreCase(item.getSignal()))
                    .toList();
        }
        if (pageable.getSort().isSorted()) {
            result = new ArrayList<>(result);
            result.sort(buildMacdSignalComparator(pageable.getSort()));
        }
        return toPage(result, pageable);
    }

    public Page<StockTradeBacktestVO> macdBacktest(MacdBacktestReqVO reqVO, Pageable pageable) {
        Set<String> watchlistCodes = loadWatchlistCodes(reqVO.getWatchlistGroupId());
        if (watchlistCodes != null && watchlistCodes.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Page<StockTradeBacktestVO> snapshotPage = stockStrategySnapshotService
                .queryMacdBacktestSnapshot(reqVO, pageable, watchlistCodes);
        if (snapshotPage != null) {
            return snapshotPage;
        }

        boolean earlyPaginate = StringUtils.isBlank(reqVO.getReliability()) && !hasStrategySortFields(pageable.getSort());
        if (earlyPaginate) {
            Page<StockQuote> pagedStocks = stockQuoteRepository.findAll(
                    buildStockQuoteSpec(reqVO.getCode(), watchlistCodes, reqVO.getMarket()), pageable
            );
            if (pagedStocks.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            List<StockTradeBacktestVO> pagedList = macdStrategy.backtest(
                    reqVO.getFastPeriod(), reqVO.getSlowPeriod(), reqVO.getSignalPeriod(),
                    reqVO.getRecentYears(), pagedStocks.getContent()
            );
            return new PageImpl<>(pagedList, pageable, pagedStocks.getTotalElements());
        }

        List<StockQuote> targetStocks = stockQuoteRepository.findAll(
                buildStockQuoteSpec(reqVO.getCode(), watchlistCodes, reqVO.getMarket())
        );
        if (targetStocks.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        List<StockTradeBacktestVO> result = macdStrategy.backtest(
                reqVO.getFastPeriod(), reqVO.getSlowPeriod(), reqVO.getSignalPeriod(),
                reqVO.getRecentYears(), targetStocks
        );
        if (StringUtils.isNotBlank(reqVO.getReliability())) {
            result = result.stream()
                    .filter(item -> reqVO.getReliability().equals(item.getReliability()))
                    .toList();
        }
        if (pageable.getSort().isSorted()) {
            result = new ArrayList<>(result);
            result.sort(buildBacktestComparator(pageable.getSort()));
        }
        return toPage(result, pageable);
    }

    private Set<String> loadWatchlistCodes(Long watchlistGroupId) {
        if (watchlistGroupId == null) {
            return null;
        }
        Long userId = UserContext.requireCurrentUserId();
        stockWatchlistGroupRepository.findByIdAndUserId(watchlistGroupId, userId)
                .orElseThrow(() -> new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND));
        return stockWatchlistStockRepository.findByGroupIdOrderBySortNoDesc(watchlistGroupId)
                .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
    }

    private Comparator<StockTradeSignalVO> buildMacdSignalComparator(Sort sort) {
        Comparator<StockTradeSignalVO> result = null;
        for (Sort.Order order : sort) {
            Comparator<StockTradeSignalVO> comparator = switch (order.getProperty()) {
                case "code" -> Comparator.comparing(StockTradeSignalVO::getCode);
                case "name" -> Comparator.comparing(StockTradeSignalVO::getName);
                case SIGNAL -> Comparator.comparing(StockTradeSignalVO::getSignal);
                case "latestPrice" -> Comparator.comparing(
                        StockTradeSignalVO::getLatestPrice, Comparator.nullsLast(BigDecimal::compareTo));
                case "pir" -> Comparator.comparing(
                        StockTradeSignalVO::getPir, Comparator.nullsLast(BigDecimal::compareTo));
                case "dif" -> Comparator.comparing(
                        StockTradeSignalVO::getDif, Comparator.nullsLast(BigDecimal::compareTo));
                case "dea" -> Comparator.comparing(
                        StockTradeSignalVO::getDea, Comparator.nullsLast(BigDecimal::compareTo));
                case "macdHistogram" -> Comparator.comparing(
                        StockTradeSignalVO::getMacdHistogram, Comparator.nullsLast(BigDecimal::compareTo));
                default -> null;
            };
            if (comparator != null) {
                if (order.getDirection() == Sort.Direction.DESC) {
                    comparator = comparator.reversed();
                }
                result = result == null ? comparator : result.thenComparing(comparator);
            }
        }
        return result != null ? result : Comparator.comparing(StockTradeSignalVO::getCode);
    }

    private <T> Page<T> toPage(List<T> result, Pageable pageable) {
        int total = result.size();
        int fromIndex = pageable.getPageNumber() * pageable.getPageSize();
        if (fromIndex >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        return new PageImpl<>(result.subList(fromIndex, toIndex), pageable, total);
    }

    private boolean hasStrategySortFields(Sort sort) {
        if (!sort.isSorted()) return false;
        for (Sort.Order order : sort) {
            String prop = order.getProperty();
            if (SIGNAL.equals(prop) || "momentumValue".equals(prop) || "dif".equals(prop)
                    || "dea".equals(prop) || "macdHistogram".equals(prop) || "totalReturn".equals(prop) ||
                "tradeCount".equals(prop) || "winRate".equals(prop) || "pValue".equals(prop)) {
                return true;
            }
        }
        return false;
    }

    private Specification<StockQuote> buildStockQuoteSpec(String code, Set<String> watchlistCodes, String market) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.isNotBlank(market)) {
                predicates.add(cb.like(cb.lower(root.get("code")), market.toLowerCase() + "%"));
            }
            if (StringUtils.isNotBlank(code)) {
                predicates.add(cb.equal(root.get("code"), code));
            }
            if (watchlistCodes != null) {
                List<Predicate> orPreds = new ArrayList<>();
                for (String wc : watchlistCodes) {
                    orPreds.add(cb.like(root.get("code"), "%" + wc));
                }
                predicates.add(cb.or(orPreds.toArray(new Predicate[0])));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
