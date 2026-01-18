package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockDividend;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendStatPageReqVO;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendStatVO;
import com.brotherc.aquant.repository.StockDividendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    public Page<StockDividendStatVO> pageDividendStats(StockDividendStatPageReqVO reqVO, Pageable pageable) {
        List<StockDividendStatVO> all = calcDividendStats(reqVO.getRecentYears(), reqVO.getMinAvgDividend());

        // 排序（按最近一年分红倒序）
        all.sort(
                Comparator.comparing(
                        StockDividendStatVO::getLatestYearDividend,
                        Comparator.nullsLast(BigDecimal::compareTo)
                ).reversed()
        );

        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());

        List<StockDividendStatVO> content =
                start >= all.size() ? Collections.emptyList() : all.subList(start, end);

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


}
