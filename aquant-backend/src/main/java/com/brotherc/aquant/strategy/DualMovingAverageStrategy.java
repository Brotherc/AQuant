package com.brotherc.aquant.strategy;

import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.repository.projection.StockQuoteHistoryProjection;
import com.brotherc.aquant.enums.TradeSignal;
import com.brotherc.aquant.exception.BusinessException;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.vo.strategy.StockTradeSignalVO;
import com.brotherc.aquant.model.vo.strategy.StockTradeBacktestVO;
import com.brotherc.aquant.repository.StockQuoteHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.stat.inference.TTest;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DualMovingAverageStrategy {

    private static final int RETURN_SCALE = 8;

    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;

    public List<StockTradeSignalVO> calculate(int maShort, int maLong, List<StockQuote> stocks) {
        if (maShort >= maLong) {
            throw new BusinessException(ExceptionEnum.STOCK_STRATEGY_DUAL_MA_ILLEGAL);
        }

        List<StockTradeSignalVO> result = new ArrayList<>();

        int needDays = maLong + 1;
        List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(needDays);

        int batchSize = 500;
        for (int b = 0; b < stocks.size(); b += batchSize) {
            List<StockQuote> batch = stocks.subList(b, Math.min(stocks.size(), b + batchSize));
            List<String> codes = batch.stream().map(StockQuote::getCode).toList();

            List<StockQuoteHistoryProjection> histories = stockQuoteHistoryRepository.findByTradeDateInAndCodeInOrderByTradeDateAsc(recentDates, codes);
            Map<String, List<StockQuoteHistoryProjection>> historyMap = histories.stream().collect(Collectors.groupingBy(StockQuoteHistoryProjection::getCode));

            for (StockQuote stock : batch) {
                String code = stock.getCode();
                String name = stock.getName();

                List<StockQuoteHistoryProjection> list = historyMap.getOrDefault(code, new ArrayList<>());

                if (list.size() < needDays) {
                    StockTradeSignalVO stockTradeSignalVO = new StockTradeSignalVO(
                            code, name, TradeSignal.HOLD.name(), stock.getLatestPrice(), stock.getPir()
                    );
                    result.add(stockTradeSignalVO);
                    continue;
                }

                // 批次数据自带 ASC 排序，已无需逆序

                // 今天
                BigDecimal todayShort = avg(list.subList(list.size() - maShort, list.size()));

                BigDecimal todayLong = avg(list.subList(list.size() - maLong, list.size()));

                // 昨天
                BigDecimal yesterdayShort = avg(list.subList(list.size() - maShort - 1, list.size() - 1));

                BigDecimal yesterdayLong = avg(list.subList(list.size() - maLong - 1, list.size() - 1));

                TradeSignal signal = TradeSignal.HOLD;

                // 金叉
                if (yesterdayShort.compareTo(yesterdayLong) <= 0 && todayShort.compareTo(todayLong) > 0) {
                    signal = TradeSignal.BUY;
                }
                // 死叉
                else if (yesterdayShort.compareTo(yesterdayLong) >= 0 && todayShort.compareTo(todayLong) < 0) {
                    signal = TradeSignal.SELL;
                }

                result.add(new StockTradeSignalVO(code, name, signal.name(), stock.getLatestPrice(), stock.getPir()));
            }
        }

        return result;
    }

    public List<StockTradeBacktestVO> backtest(int maShort, int maLong, int recentYears, List<StockQuote> stocks) {
        if (maShort >= maLong) {
            throw new BusinessException(ExceptionEnum.STOCK_STRATEGY_DUAL_MA_ILLEGAL);
        }

        List<StockTradeBacktestVO> result = new ArrayList<>();
        // 1年约250个交易日
        int needDays = recentYears * 250 + maLong;
        List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(needDays);
        BigDecimal maShortDecimal = BigDecimal.valueOf(maShort);
        BigDecimal maLongDecimal = BigDecimal.valueOf(maLong);
        TTest tTest = new TTest();

        int batchSize = 500;
        for (int b = 0; b < stocks.size(); b += batchSize) {
            List<StockQuote> batch = stocks.subList(b, Math.min(stocks.size(), b + batchSize));
            List<String> codes = batch.stream().map(StockQuote::getCode).toList();

            List<StockQuoteHistoryProjection> histories = stockQuoteHistoryRepository
                    .findByTradeDateInAndCodeInOrderByTradeDateAsc(recentDates, codes);
            Map<String, List<StockQuoteHistoryProjection>> historyMap = groupHistoriesByCode(histories);

            for (StockQuote stock : batch) {
                String code = stock.getCode();
                List<StockQuoteHistoryProjection> list = historyMap.getOrDefault(code, Collections.emptyList());
                result.add(backtestSingle(stock, list, maShort, maLong, recentYears, tTest, maShortDecimal, maLongDecimal));
            }
        }

        return result;
    }

    private StockTradeBacktestVO backtestSingle(
            StockQuote stock,
            List<StockQuoteHistoryProjection> histories,
            int maShort,
            int maLong,
            int recentYears,
            TTest tTest,
            BigDecimal maShortDecimal,
            BigDecimal maLongDecimal
    ) {
        return backtestSingleInternal(
                stock,
                extractClosePrices(histories),
                maShort,
                maLong,
                recentYears,
                tTest,
                maShortDecimal,
                maLongDecimal
        );
    }

    public StockTradeBacktestVO backtestSingle(
            StockQuote stock,
            BigDecimal[] closePrices,
            int maShort,
            int maLong,
            int recentYears,
            TTest tTest,
            BigDecimal maShortDecimal,
            BigDecimal maLongDecimal
    ) {
        return backtestSingleInternal(
                stock, closePrices, maShort, maLong, recentYears, tTest, maShortDecimal, maLongDecimal
        );
    }

    private StockTradeBacktestVO backtestSingleInternal(
            StockQuote stock,
            BigDecimal[] closePrices,
            int maShort,
            int maLong,
            int recentYears,
            TTest tTest,
            BigDecimal maShortDecimal,
            BigDecimal maLongDecimal
    ) {
        String code = stock.getCode();
        String name = stock.getName();
        int needDays = recentYears * 250 + maLong;
        int startIndex = Math.max(0, closePrices.length - needDays);
        int usableLength = closePrices.length - startIndex;

        if (usableLength <= maLong) {
            return new StockTradeBacktestVO(
                    code, name, BigDecimal.ZERO, 0, BigDecimal.ZERO, null, null, "样本不足",
                    stock.getLatestPrice(), stock.getPir(), stock.getCreatedAt()
            );
        }

        BigDecimal netValue = BigDecimal.ONE;
        TradeSignal currentPosition = TradeSignal.HOLD;
        BigDecimal costPrice = BigDecimal.ZERO;
        List<Double> tradeReturns = new ArrayList<>();
        int winCount = 0;
        double tradeReturnSum = 0D;

        BigDecimal yesterdayShortSum = sum(closePrices, startIndex + maLong - maShort, startIndex + maLong);
        BigDecimal yesterdayLongSum = sum(closePrices, startIndex, startIndex + maLong);

        for (int i = startIndex + maLong; i < closePrices.length; i++) {
            BigDecimal todayPrice = closePrices[i];
            BigDecimal todayShortSum = yesterdayShortSum
                    .subtract(closePrices[i - maShort])
                    .add(todayPrice);
            BigDecimal todayLongSum = yesterdayLongSum
                    .subtract(closePrices[i - maLong])
                    .add(todayPrice);

            int yesterdayCompare = compareMovingAverage(
                    yesterdayShortSum, yesterdayLongSum, maShortDecimal, maLongDecimal
            );
            int todayCompare = compareMovingAverage(
                    todayShortSum, todayLongSum, maShortDecimal, maLongDecimal
            );

            if (yesterdayCompare <= 0 && todayCompare > 0) {
                if (currentPosition == TradeSignal.HOLD) {
                    currentPosition = TradeSignal.BUY;
                    costPrice = todayPrice;
                }
            } else if (yesterdayCompare >= 0 && todayCompare < 0) {
                if (currentPosition == TradeSignal.BUY && costPrice.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal tradeReturn = todayPrice.subtract(costPrice)
                            .divide(costPrice, RETURN_SCALE, RoundingMode.HALF_UP);
                    double tradeReturnValue = tradeReturn.doubleValue();
                    tradeReturns.add(tradeReturnValue);
                    tradeReturnSum += tradeReturnValue;
                    if (tradeReturn.signum() > 0) {
                        winCount++;
                    }
                    netValue = netValue.multiply(BigDecimal.ONE.add(tradeReturn));

                    currentPosition = TradeSignal.HOLD;
                    costPrice = BigDecimal.ZERO;
                }
            }

            yesterdayShortSum = todayShortSum;
            yesterdayLongSum = todayLongSum;
        }

        if (currentPosition == TradeSignal.BUY && costPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal lastPrice = closePrices[closePrices.length - 1];
            BigDecimal tradeReturn = lastPrice.subtract(costPrice).divide(costPrice, RETURN_SCALE, RoundingMode.HALF_UP);
            double tradeReturnValue = tradeReturn.doubleValue();
            tradeReturns.add(tradeReturnValue);
            tradeReturnSum += tradeReturnValue;
            if (tradeReturn.signum() > 0) {
                winCount++;
            }
            netValue = netValue.multiply(BigDecimal.ONE.add(tradeReturn));
        }

        BigDecimal totalReturn = netValue.subtract(BigDecimal.ONE);
        int tradeCount = tradeReturns.size();
        BigDecimal winRate = BigDecimal.ZERO;
        Double tValue = null;
        Double pValue = null;
        String reliability = "样本不足";

        if (tradeCount > 0) {
            winRate = BigDecimal.valueOf(winCount).divide(BigDecimal.valueOf(tradeCount), 4, RoundingMode.HALF_UP);
        }

        if (tradeCount >= 2) {
            double[] sampleArray = tradeReturns.stream().mapToDouble(Double::doubleValue).toArray();
            double sampleMean = tradeReturnSum / tradeCount;

            try {
                tValue = tTest.t(0.0, sampleArray);
                double twoSidedPValue = tTest.tTest(0.0, sampleArray);

                if (!Double.isFinite(tValue) || !Double.isFinite(twoSidedPValue)) {
                    tValue = null;
                    pValue = null;
                    reliability = sampleMean > 0 ? "低(方差0)" : "低";
                } else {
                    if (tValue > 0) {
                        pValue = twoSidedPValue / 2.0;
                    } else {
                        pValue = 1.0 - (twoSidedPValue / 2.0);
                    }

                    if (sampleMean > 0 && pValue != null && pValue < 0.05) {
                        reliability = "高";
                    } else if (sampleMean > 0 && pValue != null && pValue < 0.10) {
                        reliability = "中";
                    } else {
                        reliability = "低";
                    }
                }

            } catch (Exception e) {
                reliability = sampleMean > 0 ? "低(方差0)" : "低";
            }
        }

        return new StockTradeBacktestVO(
                code, name, totalReturn, tradeCount, winRate, tValue, pValue,
                reliability, stock.getLatestPrice(), stock.getPir(), stock.getCreatedAt()
        );
    }

    public Map<String, List<StockQuoteHistoryProjection>> groupHistoriesByCode(List<StockQuoteHistoryProjection> histories) {
        Map<String, List<StockQuoteHistoryProjection>> historyMap = new HashMap<>();
        for (StockQuoteHistoryProjection history : histories) {
            historyMap.computeIfAbsent(history.getCode(), key -> new ArrayList<>()).add(history);
        }
        return historyMap;
    }

    public BigDecimal[] extractClosePrices(List<StockQuoteHistoryProjection> histories) {
        BigDecimal[] closePrices = new BigDecimal[histories.size()];
        for (int i = 0; i < histories.size(); i++) {
            closePrices[i] = histories.get(i).getClosePrice();
        }
        return closePrices;
    }

    private BigDecimal sum(BigDecimal[] closePrices, int startInclusive, int endExclusive) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = startInclusive; i < endExclusive; i++) {
            sum = sum.add(closePrices[i]);
        }
        return sum;
    }

    private int compareMovingAverage(
            BigDecimal shortSum,
            BigDecimal longSum,
            BigDecimal maShortDecimal,
            BigDecimal maLongDecimal
    ) {
        return shortSum.multiply(maLongDecimal).compareTo(longSum.multiply(maShortDecimal));
    }

    private BigDecimal avg(List<StockQuoteHistoryProjection> list) {
        return list.stream()
                .map(StockQuoteHistoryProjection::getClosePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(list.size()), 4, RoundingMode.HALF_UP);
    }

}
