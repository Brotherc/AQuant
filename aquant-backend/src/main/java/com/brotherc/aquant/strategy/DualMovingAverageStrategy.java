package com.brotherc.aquant.strategy;

import com.brotherc.aquant.entity.StockQuoteHistory;
import com.brotherc.aquant.enums.TradeSignal;
import com.brotherc.aquant.exception.BusinessException;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.dto.stockquote.StockCodeName;
import com.brotherc.aquant.model.vo.strategy.StockTradeSignalVO;
import com.brotherc.aquant.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.repository.StockQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DualMovingAverageStrategy {

    private final StockQuoteRepository stockQuoteRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;

    public List<StockTradeSignalVO> calculate(int maShort, int maLong) {
        if (maShort >= maLong) {
            throw new BusinessException(ExceptionEnum.STOCK_STRATEGY_DUAL_MA_ILLEGAL);
        }

        List<StockTradeSignalVO> result = new ArrayList<>();

        List<StockCodeName> stocks = stockQuoteRepository.findAllStockCodes();

        int needDays = maLong + 1;

        for (StockCodeName stock : stocks) {

            String code = stock.getCode();
            String name = stock.getName();

            List<StockQuoteHistory> list = stockQuoteHistoryRepository.findLatestByCode(code, needDays);

            if (list.size() < needDays) {
                result.add(new StockTradeSignalVO(code, name, TradeSignal.HOLD.name()));
                continue;
            }

            // 倒序转正序
            Collections.reverse(list);

            // 今天
            BigDecimal todayShort = avg(list.subList(list.size() - maShort, list.size()));

            BigDecimal todayLong = avg(list.subList(list.size() - maLong, list.size()));

            // 昨天
            BigDecimal yesterdayShort = avg(list.subList(list.size() - maShort - 1, list.size() - 1));

            BigDecimal yesterdayLong = avg(list.subList(list.size() - maLong - 1, list.size() - 1));

            TradeSignal signal = TradeSignal.HOLD;

            // 金叉
            if (yesterdayShort.compareTo(yesterdayLong) <= 0
                    && todayShort.compareTo(todayLong) > 0) {
                signal = TradeSignal.BUY;
            }
            // 死叉
            else if (yesterdayShort.compareTo(yesterdayLong) >= 0
                    && todayShort.compareTo(todayLong) < 0) {
                signal = TradeSignal.SELL;
            }

            result.add(new StockTradeSignalVO(code, name, signal.name()));
        }

        return result;
    }

    private BigDecimal avg(List<StockQuoteHistory> list) {
        return list.stream()
                .map(StockQuoteHistory::getClosePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(list.size()), 4, RoundingMode.HALF_UP);
    }

}
