package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.exception.BusinessException;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.vo.strategy.DualMAReqVO;
import com.brotherc.aquant.model.vo.strategy.DualMABacktestReqVO;
import com.brotherc.aquant.model.vo.strategy.MomentumReqVO;
import com.brotherc.aquant.model.vo.strategy.MomentumBacktestReqVO;
import com.brotherc.aquant.model.vo.strategy.StockTradeSignalVO;
import com.brotherc.aquant.model.vo.strategy.StockTradeBacktestVO;
import com.brotherc.aquant.repository.StockQuoteRepository;
import com.brotherc.aquant.repository.StockWatchlistGroupRepository;
import com.brotherc.aquant.strategy.DualMovingAverageStrategy;
import com.brotherc.aquant.strategy.MomentumStrategy;
import com.brotherc.aquant.repository.StockWatchlistStockRepository;
import com.brotherc.aquant.entity.StockWatchlistStock;
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

    private final DualMovingAverageStrategy dualMovingAverageStrategy;
    private final MomentumStrategy momentumStrategy;
    private final StockWatchlistStockRepository stockWatchlistStockRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockWatchlistGroupRepository stockWatchlistGroupRepository;

    public Page<StockTradeSignalVO> dualMA(DualMAReqVO reqVO, Pageable pageable, Long userId) {
        Set<String> watchlistCodes = null;
        if (reqVO.getWatchlistGroupId() != null) {
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
            Specification<StockQuote> stockQuoteSpecification = buildStockQuoteSpec(reqVO.getCode(), watchlistCodes);
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
            } else if ("signal".equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeSignalVO::getSignal);
            } else if ("pir".equals(order.getProperty())) {
                comparator = Comparator.comparing(
                        StockTradeSignalVO::getPir,
                        Comparator.nullsLast(BigDecimal::compareTo));
            } else if ("latestPrice".equals(order.getProperty())) {
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

    public Page<StockTradeBacktestVO> dualMABacktest(DualMABacktestReqVO reqVO, Pageable pageable, Long userId) {
        // Find latest stocks up front to filter targets
        List<com.brotherc.aquant.entity.StockQuote> stocks = stockQuoteRepository.findAll();

        Stream<com.brotherc.aquant.entity.StockQuote> stream = stocks.stream();

        if (StringUtils.isNotBlank(reqVO.getCode())) {
            stream = stream.filter(vo -> reqVO.getCode().equalsIgnoreCase(vo.getCode()));
        }

        if (reqVO.getWatchlistGroupId() != null) {
            stockWatchlistGroupRepository.findByIdAndUserId(reqVO.getWatchlistGroupId(), userId)
                    .orElseThrow(() -> new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND));
            java.util.Set<String> watchlistCodes = stockWatchlistStockRepository
                    .findByGroupIdOrderBySortNoDesc(reqVO.getWatchlistGroupId())
                    .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
            if (watchlistCodes.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            stream = stream.filter(vo -> {
                String c = vo.getCode();
                String c6 = c.length() > 6 ? c.substring(c.length() - 6) : c;
                return watchlistCodes.contains(c6);
            });
        }

        List<com.brotherc.aquant.entity.StockQuote> targetStocks = stream.collect(Collectors.toList());
        if (targetStocks.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<StockTradeBacktestVO> result = dualMovingAverageStrategy.backtest(
                reqVO.getMaShort(), reqVO.getMaLong(), reqVO.getRecentYears(), targetStocks);

        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            Comparator<StockTradeBacktestVO> comparator = buildBacktestComparator(sort);
            result.sort(comparator);
        }

        int total = result.size();
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int fromIndex = currentPage * pageSize;

        if (fromIndex >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }

        int toIndex = Math.min(fromIndex + pageSize, total);

        List<StockTradeBacktestVO> pageContent = result.subList(fromIndex, toIndex);

        return new PageImpl<>(pageContent, pageable, total);
    }

    private Comparator<StockTradeBacktestVO> buildBacktestComparator(Sort sort) {
        Comparator<StockTradeBacktestVO> result = null;

        for (Sort.Order order : sort) {
            Comparator<StockTradeBacktestVO> comparator = null;
            if ("code".equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeBacktestVO::getCode);
            } else if ("name".equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeBacktestVO::getName);
            } else if ("totalReturn".equals(order.getProperty())) {
                comparator = Comparator.comparing(
                        StockTradeBacktestVO::getTotalReturn,
                        Comparator.nullsLast(BigDecimal::compareTo));
            } else if ("latestPrice".equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeBacktestVO::getLatestPrice);
            }

            if (comparator == null) {
                continue;
            }

            if (order.getDirection() == Sort.Direction.DESC) {
                comparator = comparator.reversed();
            }

            result = (result == null) ? comparator : result.thenComparing(comparator);
        }

        return result != null ? result : Comparator.comparing(StockTradeBacktestVO::getCode);
    }

    // ==================== 动量策略 ====================

    public Page<StockTradeSignalVO> momentum(MomentumReqVO reqVO, Pageable pageable, Long userId) {
        java.util.Set<String> watchlistCodes = null;
        if (reqVO.getWatchlistGroupId() != null) {
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
            Page<StockQuote> pagedStocks = stockQuoteRepository.findAll(buildStockQuoteSpec(reqVO.getCode(), watchlistCodes), pageable);
            if (pagedStocks.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            List<StockTradeSignalVO> pagedList = momentumStrategy.calculate(reqVO.getLookbackDays(), reqVO.getThreshold(), pagedStocks.getContent());
            return new PageImpl<>(pagedList, pageable, pagedStocks.getTotalElements());
        }

        List<StockQuote> allStocks = stockQuoteRepository.findAll();
        Stream<StockQuote> quoteStream = allStocks.stream();

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

        List<StockQuote> targetStocks = quoteStream.collect(Collectors.toList());
        if (targetStocks.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<StockTradeSignalVO> list = momentumStrategy.calculate(reqVO.getLookbackDays(), reqVO.getThreshold(), targetStocks);

        if (StringUtils.isNotBlank(reqVO.getSignal())) {
            list = list.stream().filter(vo -> reqVO.getSignal().equalsIgnoreCase(vo.getSignal())).collect(Collectors.toList());
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
            } else if ("signal".equals(order.getProperty())) {
                comparator = Comparator.comparing(StockTradeSignalVO::getSignal);
            } else if ("pir".equals(order.getProperty())) {
                comparator = Comparator.comparing(
                        StockTradeSignalVO::getPir,
                        Comparator.nullsLast(BigDecimal::compareTo));
            } else if ("latestPrice".equals(order.getProperty())) {
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

    public Page<StockTradeBacktestVO> momentumBacktest(MomentumBacktestReqVO reqVO, Pageable pageable, Long userId) {
        List<StockQuote> stocks = stockQuoteRepository.findAll();

        Stream<StockQuote> stream = stocks.stream();

        if (StringUtils.isNotBlank(reqVO.getCode())) {
            stream = stream.filter(vo -> reqVO.getCode().equalsIgnoreCase(vo.getCode()));
        }

        if (reqVO.getWatchlistGroupId() != null) {
            stockWatchlistGroupRepository.findByIdAndUserId(reqVO.getWatchlistGroupId(), userId)
                    .orElseThrow(() -> new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND));
            Set<String> watchlistCodes = stockWatchlistStockRepository
                    .findByGroupIdOrderBySortNoDesc(reqVO.getWatchlistGroupId())
                    .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
            if (watchlistCodes.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            stream = stream.filter(vo -> {
                String c = vo.getCode();
                String c6 = c.length() > 6 ? c.substring(c.length() - 6) : c;
                return watchlistCodes.contains(c6);
            });
        }

        List<StockQuote> targetStocks = stream.collect(Collectors.toList());
        if (targetStocks.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<StockTradeBacktestVO> result = momentumStrategy.backtest(
                reqVO.getLookbackDays(), reqVO.getRecentYears(), targetStocks);

        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            Comparator<StockTradeBacktestVO> comparator = buildBacktestComparator(sort);
            result.sort(comparator);
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

    private boolean hasStrategySortFields(Sort sort) {
        if (!sort.isSorted()) return false;
        for (Sort.Order order : sort) {
            String prop = order.getProperty();
            if ("signal".equals(prop) || "momentumValue".equals(prop) || "totalReturn".equals(prop)) {
                return true;
            }
        }
        return false;
    }

    private Specification<StockQuote> buildStockQuoteSpec(String code, Set<String> watchlistCodes) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
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
