package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockDividend;
import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendStatPageReqVO;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendStatVO;
import com.brotherc.aquant.repository.StockDividendRepository;
import com.brotherc.aquant.repository.StockQuoteRepository;
import lombok.RequiredArgsConstructor;
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

    public Page<StockDividendStatVO> pageDividendStats(StockDividendStatPageReqVO reqVO, Pageable pageable) {
        List<StockDividendStatVO> all = calcDividendStats(reqVO.getRecentYears(), reqVO.getMinAvgDividend());

        // 排序（按最近一年分红倒序）
        all.sort(buildComparator(pageable.getSort()));

        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());

        List<StockDividendStatVO> content =
                start >= all.size() ? Collections.emptyList() : all.subList(start, end);

        List<String> codeList = content.stream().map(o -> {
            String code = o.getStockCode();
            if (code.startsWith("6")) {
                return "sh" + code;
            } else if (code.startsWith("0") || code.startsWith("3")) {
                return "sz" + code;
            } else if (code.startsWith("8") || code.startsWith("9")) {
                return "bj" + code;
            }
            return code;
        }).toList();

        Map<String, StockQuote> stockQuoteMap = stockQuoteRepository.findByCodeIn(codeList)
                .stream().collect(Collectors.toMap(o -> o.getCode().substring(2), o -> o));

        for (StockDividendStatVO item : content) {
            StockQuote stockQuote = stockQuoteMap.get(item.getStockCode());
            if (stockQuote != null) {
                item.setLatestPrice(stockQuote.getLatestPrice());
            }
        }

        return new PageImpl<>(content, pageable, all.size());
    }


    public List<StockDividendStatVO> calcDividendStats(
            Integer recentYears,
            BigDecimal minAvgDividend
    ) {

        List<StockDividend> list;
        if (recentYears != null) {
            LocalDate fromDate = LocalDate.now().minusYears(recentYears).withDayOfYear(1);
            list = stockDividendRepository.findByLatestAnnouncementDateAfter(fromDate);
        } else {
            list = stockDividendRepository.findAll();
        }

        // 按股票分组
        Map<String, List<StockDividend>> group = list.stream().collect(Collectors.groupingBy(StockDividend::getStockCode));

        List<StockDividendStatVO> result = new ArrayList<>();

        for (Map.Entry<String, List<StockDividend>> entry : group.entrySet()) {

            String stockCode = entry.getKey();
            List<StockDividend> dividends = entry.getValue();

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
                            RoundingMode.HALF_UP
                    );

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

            StockDividendStatVO stockDividendStatVO = new StockDividendStatVO();
            stockDividendStatVO.setStockCode(stockCode);
            stockDividendStatVO.setStockName(dividends.get(0).getStockName());
            stockDividendStatVO.setAvgDividend(avg);
            stockDividendStatVO.setLatestYearDividend(latestYearDividend);
            result.add(stockDividendStatVO);
        }

        return result;
    }

    private Comparator<StockDividendStatVO> buildComparator(Sort sort) {
        Comparator<StockDividendStatVO> result = null;

        for (Sort.Order order : sort) {
            Comparator<StockDividendStatVO> comparator = null;

            switch (order.getProperty()) {
                case "latestYearDividend":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getLatestYearDividend,
                            Comparator.nullsLast(BigDecimal::compareTo)
                    );
                    break;

                case "avgDividend":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getAvgDividend,
                            Comparator.nullsLast(BigDecimal::compareTo)
                    );
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
        return result != null
                ? result
                : Comparator.comparing(
                StockDividendStatVO::getLatestYearDividend,
                Comparator.nullsLast(BigDecimal::compareTo)
        ).reversed();
    }

}
