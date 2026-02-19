package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockDividend;
import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendDetailReqVO;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendDetailVO;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendStatPageReqVO;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendStatVO;
import com.brotherc.aquant.repository.StockDividendRepository;
import com.brotherc.aquant.repository.StockQuoteRepository;
import com.brotherc.aquant.repository.StockValuationMetricsRepository;
import com.brotherc.aquant.repository.StockDupontAnalysisRepository;
import com.brotherc.aquant.entity.StockValuationMetrics;
import com.brotherc.aquant.entity.StockDupontAnalysis;
import com.brotherc.aquant.repository.StockWatchlistStockRepository;
import com.brotherc.aquant.entity.StockWatchlistStock;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockDividendService {

    private final StockDividendRepository stockDividendRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockValuationMetricsRepository stockValuationMetricsRepository;
    private final StockDupontAnalysisRepository stockDupontAnalysisRepository;
    private final StockWatchlistStockRepository stockWatchlistStockRepository;

    public Page<StockDividendStatVO> pageDividendStats(StockDividendStatPageReqVO reqVO, Pageable pageable) {
        List<StockDividendStatVO> all = calcDividendStats(reqVO.getRecentYears(), reqVO.getMinAvgDividend(),
                reqVO.getStockCode(), reqVO.getStockName(), reqVO.getWatchlistGroupId());

        Map<String, StockQuote> stockQuoteMap = stockQuoteRepository.findAll()
                .stream().collect(Collectors.toMap(o -> o.getCode().substring(2), o -> o));

        Map<String, StockValuationMetrics> valuationMap = stockValuationMetricsRepository.findAll()
                .stream().collect(Collectors.toMap(o -> o.getStockCode().substring(2), o -> o));

        Map<String, StockDupontAnalysis> dupontMap = stockDupontAnalysisRepository.findAll()
                .stream().collect(Collectors.toMap(o -> o.getStockCode().substring(2), o -> o));

        for (StockDividendStatVO item : all) {
            StockQuote stockQuote = stockQuoteMap.get(item.getStockCode());
            if (stockQuote != null) {
                item.setLatestPrice(stockQuote.getLatestPrice());
            }

            StockValuationMetrics valuation = valuationMap.get(item.getStockCode());
            if (valuation != null) {
                item.setPeg(valuation.getPeg());
                item.setPe(valuation.getPeTtm());
                item.setPeIndustryAvg(valuation.getPeTtmIndustryAvg());
            }

            StockDupontAnalysis dupont = dupontMap.get(item.getStockCode());
            if (dupont != null) {
                item.setRoeActual(dupont.getRoeLastYA());
                item.setRoe3yAvg(dupont.getRoe3yAvg());
            }
        }

        // 排序（按最近一年分红倒序）
        all.sort(buildComparator(pageable.getSort()));

        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());

        List<StockDividendStatVO> content = start >= all.size() ? Collections.emptyList() : all.subList(start, end);

        return new PageImpl<>(content, pageable, all.size());
    }

    public List<StockDividendStatVO> calcDividendStats(
            Integer recentYears, BigDecimal minAvgDividend, String stockCodeQuery, String stockNameQuery,
            Long watchlistGroupId) {

        Set<String> watchlistCodes = null;
        if (watchlistGroupId != null) {
            watchlistCodes = stockWatchlistStockRepository.findByGroupIdOrderBySortNoDesc(watchlistGroupId)
                    .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
            if (watchlistCodes.isEmpty()) {
                return new ArrayList<>();
            }
        }

        List<StockDividend> list;
        if (recentYears != null) {
            LocalDate fromDate = LocalDate.now().minusYears(recentYears).withDayOfYear(1);
            list = stockDividendRepository.findByLatestAnnouncementDateAfter(fromDate);
        } else {
            list = stockDividendRepository.findAll();
        }

        // 按股票分组
        Map<String, List<StockDividend>> group = list.stream()
                .collect(Collectors.groupingBy(StockDividend::getStockCode));

        List<StockDividendStatVO> result = new ArrayList<>();

        for (Map.Entry<String, List<StockDividend>> entry : group.entrySet()) {

            String stockCode = entry.getKey();
            List<StockDividend> dividends = entry.getValue();
            String stockName = dividends.get(0).getStockName();

            // 过滤股票代码和名称
            if (StringUtils.isNotBlank(stockCodeQuery) && !stockCode.contains(stockCodeQuery)) {
                continue;
            }
            if (StringUtils.isNotBlank(stockNameQuery) && !stockName.contains(stockNameQuery)) {
                continue;
            }
            // 过滤自选分组
            if (watchlistCodes != null && !watchlistCodes.contains(stockCode)) {
                continue;
            }

            int currentYear = LocalDate.now().getYear();
            int minYear = dividends.stream()
                    .map(d -> d.getLatestAnnouncementDate().getYear())
                    .min(Integer::compareTo)
                    .orElse(currentYear);
            int years = currentYear - minYear + 1;

            // 最近 N 年平均分红
            BigDecimal avg = dividends.stream()
                    .map(StockDividend::getCashDividendRatio)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(
                            BigDecimal.valueOf(years),
                            4,
                            RoundingMode.HALF_UP);

            if (minAvgDividend != null && avg.compareTo(minAvgDividend) < 0) {
                continue;
            }

            // 最近一年分红（按公告日最大年）
            int latestYear = dividends.stream()
                    .map(d -> d.getLatestAnnouncementDate().getYear())
                    .max(Integer::compareTo)
                    .orElse(0);

            BigDecimal latestYearDividend = dividends.stream()
                    .filter(d -> d.getLatestAnnouncementDate().getYear() == latestYear)
                    .map(StockDividend::getCashDividendRatio)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal latestYearTransfer = dividends.stream()
                    .filter(d -> d.getLatestAnnouncementDate().getYear() == latestYear)
                    .map(d -> {
                        BigDecimal bonus = d.getBonusShareRatio() != null ? d.getBonusShareRatio() : BigDecimal.ZERO;
                        BigDecimal transfer = d.getTransferShareRatio() != null ? d.getTransferShareRatio()
                                : BigDecimal.ZERO;
                        return bonus.add(transfer);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            StockDividendStatVO stockDividendStatVO = new StockDividendStatVO();
            stockDividendStatVO.setStockCode(stockCode);
            stockDividendStatVO.setStockName(dividends.get(0).getStockName());
            stockDividendStatVO.setAvgDividend(avg);
            stockDividendStatVO.setLatestYearDividend(latestYearDividend);
            stockDividendStatVO.setLatestYearTransfer(latestYearTransfer);
            result.add(stockDividendStatVO);
        }

        return result;
    }

    private Comparator<StockDividendStatVO> buildComparator(Sort sort) {
        Comparator<StockDividendStatVO> result = null;

        for (Sort.Order order : sort) {
            Comparator<StockDividendStatVO> comparator;

            switch (order.getProperty()) {
                case "latestYearDividend":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getLatestYearDividend,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                case "latestYearTransfer":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getLatestYearTransfer,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                case "avgDividend":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getAvgDividend,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                case "latestPrice":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getLatestPrice,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                case "peg":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getPeg,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                case "pe":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getPe,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                case "roe":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getRoeActual,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                default:
                    // 不认识的排序字段，直接跳过
                    continue;
            }

            if (order.getDirection() == Sort.Direction.DESC) {
                comparator = comparator.reversed();
            }

            result = (result == null) ? comparator : result.thenComparing(comparator);
        }

        // 默认排序（兜底）
        return result != null ? result
                : Comparator.comparing(
                        StockDividendStatVO::getLatestYearDividend,
                        Comparator.nullsLast(BigDecimal::compareTo)).reversed();
    }

    public List<StockDividendDetailVO> getDetailByCode(StockDividendDetailReqVO reqVO) {
        List<StockDividend> list = stockDividendRepository
                .findByStockCodeOrderByLatestAnnouncementDateDesc(reqVO.getStockCode());
        return list.stream().map(o -> {
            StockDividendDetailVO stockDividendDetailVO = new StockDividendDetailVO();
            BeanUtils.copyProperties(o, stockDividendDetailVO);
            return stockDividendDetailVO;
        }).toList();
    }

}
