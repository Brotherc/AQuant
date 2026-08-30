package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.industry.entity.StockIndustryBoardHistory;
import com.brotherc.aquant.industry.model.vo.IndustryRiseAnalysisVO;
import com.brotherc.aquant.industry.repository.StockIndustryBoardHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockIndustryBoardAnalysisService {

    private static final long MAX_NATURAL_DAYS = 180;
    private static final int MAX_RANK_LIMIT = 100;

    private final StockIndustryBoardHistoryRepository repository;

    @Transactional(readOnly = true)
    public List<IndustryRiseAnalysisVO> analysis(LocalDate startDate, LocalDate endDate, Integer rankLimit) {
        validateDateRange(startDate, endDate);
        validateRankLimit(rankLimit);

        List<StockIndustryBoardHistory> range = repository
                .findByTradeDateBetweenOrderByTradeDateAscSectorNameAsc(startDate.toString(), endDate.toString());
        populateMissingChangeMetrics(range, startDate);

        Map<String, List<StockIndustryBoardHistory>> byDate = new LinkedHashMap<>();
        for (StockIndustryBoardHistory history : range) {
            if (history.getChangePercent() != null) {
                byDate.computeIfAbsent(history.getTradeDate(), key -> new ArrayList<>()).add(history);
            }
        }

        Comparator<StockIndustryBoardHistory> rankingComparator = Comparator
                .comparing(StockIndustryBoardHistory::getChangePercent, Comparator.reverseOrder())
                .thenComparing(StockIndustryBoardHistory::getSectorName,
                        Comparator.nullsLast(String::compareTo));

        List<IndustryRiseAnalysisVO> result = new ArrayList<>();
        for (List<StockIndustryBoardHistory> dailyRows : byDate.values()) {
            dailyRows.sort(rankingComparator);
            int dailyResultSize = Math.min(rankLimit, dailyRows.size());
            for (int index = 0; index < dailyResultSize; index++) {
                StockIndustryBoardHistory row = dailyRows.get(index);
                result.add(new IndustryRiseAnalysisVO(
                        row.getTradeDate(),
                        row.getSectorName(),
                        index + 1,
                        row.getChangePercent(),
                        row.getChangeAmount()
                ));
            }
        }
        return result;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw validationError("开始日期和结束日期不能为空");
        }
        if (startDate.isAfter(endDate)) {
            throw validationError("开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_NATURAL_DAYS) {
            throw validationError("查询范围不能超过180个自然日");
        }
    }

    private BusinessException validationError(String message) {
        return new BusinessException(ExceptionEnum.SYS_CHECK_ERROR, message);
    }

    private void validateRankLimit(Integer rankLimit) {
        if (rankLimit == null || rankLimit < 1 || rankLimit > MAX_RANK_LIMIT) {
            throw validationError("排名数量必须在1到100之间");
        }
    }

    private void populateMissingChangeMetrics(List<StockIndustryBoardHistory> range, LocalDate startDate) {
        boolean requiresFallback = range.stream()
                .anyMatch(row -> row.getChangePercent() == null && row.getClosePrice() != null);
        if (!requiresFallback) {
            return;
        }

        Map<String, BigDecimal> previousCloseBySector = new LinkedHashMap<>();
        for (StockIndustryBoardHistory predecessor : repository
                .findLatestBeforeTradeDateForEachSector(startDate.toString())) {
            if (predecessor.getClosePrice() != null) {
                previousCloseBySector.put(predecessor.getSectorName(), predecessor.getClosePrice());
            }
        }

        Map<String, List<StockIndustryBoardHistory>> rowsBySector = new LinkedHashMap<>();
        for (StockIndustryBoardHistory history : range) {
            rowsBySector.computeIfAbsent(history.getSectorName(), key -> new ArrayList<>()).add(history);
        }
        for (Map.Entry<String, List<StockIndustryBoardHistory>> entry : rowsBySector.entrySet()) {
            List<StockIndustryBoardHistory> sectorRows = entry.getValue();
            sectorRows.sort(Comparator.comparing(StockIndustryBoardHistory::getTradeDate));
            BigDecimal previousClose = previousCloseBySector.get(entry.getKey());
            for (StockIndustryBoardHistory history : sectorRows) {
                if (history.getChangePercent() == null && history.getClosePrice() != null && previousClose != null) {
                    BigDecimal changeAmount = history.getClosePrice().subtract(previousClose);
                    history.setChangeAmount(changeAmount);
                    history.setChangePercent(toChangePercent(changeAmount, previousClose));
                }
                if (history.getClosePrice() != null) {
                    previousClose = history.getClosePrice();
                }
            }
        }
    }

    private BigDecimal toChangePercent(BigDecimal changeAmount, BigDecimal previousClose) {
        if (previousClose.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return changeAmount.divide(previousClose, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }
}
