package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.industry.entity.StockIndustryBoardEm;
import com.brotherc.aquant.industry.entity.StockIndustryBoardHistoryEm;
import com.brotherc.aquant.industry.model.vo.IndustryRiseAnalysisVO;
import com.brotherc.aquant.industry.model.vo.StockIndustryBoardKVO;
import com.brotherc.aquant.industry.model.vo.StockIndustryBoardVO;
import com.brotherc.aquant.industry.repository.StockIndustryBoardEmRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardHistoryEmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StockIndustryBoardEmQueryService {
    private static final int MAX_RANK_LIMIT = 100;
    private static final long MAX_NATURAL_DAYS = 180;
    private final StockIndustryBoardEmRepository boardRepository;
    private final StockIndustryBoardHistoryEmRepository historyRepository;

    @Transactional(readOnly = true)
    public List<IndustryRiseAnalysisVO> analysis(LocalDate startDate, LocalDate endDate, Integer rankLimit) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)
                || ChronoUnit.DAYS.between(startDate, endDate) > MAX_NATURAL_DAYS) {
            throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR, "开始日期和结束日期无效");
        }
        if (rankLimit == null || rankLimit < 1 || rankLimit > MAX_RANK_LIMIT) {
            throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR, "排名数量必须在1到100之间");
        }
        Map<String, List<StockIndustryBoardHistoryEm>> byDate = new LinkedHashMap<>();
        for (StockIndustryBoardHistoryEm item : historyRepository
                .findByTradeDateBetweenOrderByTradeDateAscSectorNameAsc(startDate.toString(), endDate.toString())) {
            if (item.getChangePercent() != null) byDate.computeIfAbsent(item.getTradeDate(), ignored -> new ArrayList<>()).add(item);
        }
        Comparator<StockIndustryBoardHistoryEm> comparator = Comparator
                .comparing(StockIndustryBoardHistoryEm::getChangePercent, Comparator.reverseOrder())
                .thenComparing(StockIndustryBoardHistoryEm::getSectorName, Comparator.nullsLast(String::compareTo));
        List<IndustryRiseAnalysisVO> result = new ArrayList<>();
        for (List<StockIndustryBoardHistoryEm> rows : byDate.values()) {
            rows.sort(comparator);
            for (int index = 0; index < Math.min(rankLimit, rows.size()); index++) {
                StockIndustryBoardHistoryEm item = rows.get(index);
                result.add(new IndustryRiseAnalysisVO(item.getTradeDate(), item.getSectorName(), index + 1,
                        item.getChangePercent(), item.getChangeAmount()));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public StockIndustryBoardVO overview(String industry) {
        return overview(industry, null);
    }

    @Transactional(readOnly = true)
    public StockIndustryBoardVO overview(String industry, LocalDate tradeDate) {
        StockIndustryBoardEm board = boardRepository.findBySectorName(industry);
        if (board == null) return null;
        StockIndustryBoardVO view = new StockIndustryBoardVO();
        BeanUtils.copyProperties(board, view);
        if (tradeDate != null) {
            StockIndustryBoardHistoryEm history = historyRepository.findBySectorNameAndTradeDate(industry, tradeDate.toString());
            if (history == null) return null;
            view.setTradeDate(tradeDate);
            view.setChangePercent(history.getChangePercent());
            view.setChangeAmount(history.getChangeAmount());
            view.setAveragePrice(history.getClosePrice());
            view.setTotalVolume(history.getVolume());
            view.setTotalAmount(history.getAmount());
            view.setNetInflow(null);
            view.setRiseCount(null);
            view.setFallCount(null);
            view.setLeadingStock(null);
            view.setLeadingStockPrice(null);
            view.setLeadingStockChangePercent(null);
        }
        return view;
    }

    @Transactional(readOnly = true)
    public List<StockIndustryBoardKVO> history(String industry, String frequency) {
        List<StockIndustryBoardKVO> daily = historyRepository.findBySectorNameOrderByTradeDateAsc(industry).stream()
                .map(item -> { StockIndustryBoardKVO view = new StockIndustryBoardKVO(); BeanUtils.copyProperties(item, view); return view; })
                .toList();
        if (daily.isEmpty()) return List.of();
        return "1d".equals(frequency) ? calculateMetrics(daily) : aggregate(daily, frequency);
    }

    private List<StockIndustryBoardKVO> calculateMetrics(List<StockIndustryBoardKVO> source) {
        BigDecimal previousClose = null;
        for (StockIndustryBoardKVO item : source) {
            applyMetrics(item, previousClose);
            if (item.getClosePrice() != null) previousClose = item.getClosePrice();
        }
        return source;
    }

    private List<StockIndustryBoardKVO> aggregate(List<StockIndustryBoardKVO> daily, String frequency) {
        Map<String, List<StockIndustryBoardKVO>> groups = new LinkedHashMap<>();
        for (StockIndustryBoardKVO item : daily) groups.computeIfAbsent(periodKey(item.getTradeDate(), frequency), ignored -> new ArrayList<>()).add(item);
        List<StockIndustryBoardKVO> result = new ArrayList<>();
        BigDecimal previousClose = null;
        for (List<StockIndustryBoardKVO> group : groups.values()) {
            StockIndustryBoardKVO first = group.get(0), last = group.get(group.size() - 1);
            StockIndustryBoardKVO item = new StockIndustryBoardKVO();
            item.setSectorName(first.getSectorName()); item.setTradeDate(last.getTradeDate());
            item.setOpenPrice(first.getOpenPrice()); item.setClosePrice(last.getClosePrice());
            item.setHighPrice(group.stream().map(StockIndustryBoardKVO::getHighPrice).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(null));
            item.setLowPrice(group.stream().map(StockIndustryBoardKVO::getLowPrice).filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(null));
            item.setVolume(group.stream().map(StockIndustryBoardKVO::getVolume).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            item.setAmount(group.stream().map(StockIndustryBoardKVO::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            applyMetrics(item, previousClose);
            if (item.getClosePrice() != null) previousClose = item.getClosePrice();
            result.add(item);
        }
        return result;
    }

    private String periodKey(String text, String frequency) {
        LocalDate date = LocalDate.parse(text);
        return switch (frequency) {
            case "1w" -> date.getYear() + "-W" + date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            case "1M" -> date.getYear() + "-" + date.getMonthValue();
            case "1Q" -> date.getYear() + "-Q" + date.get(IsoFields.QUARTER_OF_YEAR);
            case "1Y" -> String.valueOf(date.getYear());
            default -> text;
        };
    }

    private void applyMetrics(StockIndustryBoardKVO item, BigDecimal previousClose) {
        BigDecimal close = item.getClosePrice();
        if (close == null) return;
        if (previousClose == null) { item.setChangeAmount(BigDecimal.ZERO); item.setChangePercent(BigDecimal.ZERO); return; }
        BigDecimal amount = close.subtract(previousClose);
        item.setChangeAmount(amount);
        item.setChangePercent(previousClose.signum() == 0 ? BigDecimal.ZERO
                : amount.multiply(BigDecimal.valueOf(100)).divide(previousClose, 4, RoundingMode.HALF_UP));
    }
}
