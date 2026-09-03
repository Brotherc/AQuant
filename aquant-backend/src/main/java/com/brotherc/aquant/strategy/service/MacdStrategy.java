package com.brotherc.aquant.strategy.service;

import com.brotherc.aquant.common.enums.TradeSignal;
import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.stock.model.dto.StockQuoteHistoryProjection;
import com.brotherc.aquant.stock.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.strategy.model.vo.StockTradeBacktestVO;
import com.brotherc.aquant.strategy.model.vo.StockTradeSignalVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.stat.inference.TTest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MacdStrategy {

    private static final int RETURN_SCALE = 8;
    private static final int INDICATOR_SCALE = 4;
    private static final int BATCH_SIZE = 500;

    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;

    public List<StockTradeSignalVO> calculate(
            int fastPeriod,
            int slowPeriod,
            int signalPeriod,
            List<StockQuote> stocks
    ) {
        validateParameters(fastPeriod, slowPeriod, signalPeriod);
        List<StockTradeSignalVO> result = new ArrayList<>();
        int needDays = warmupDays(slowPeriod, signalPeriod) + 2;
        List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(needDays);

        for (int batchStart = 0; batchStart < stocks.size(); batchStart += BATCH_SIZE) {
            List<StockQuote> batch = stocks.subList(batchStart, Math.min(stocks.size(), batchStart + BATCH_SIZE));
            List<String> codes = batch.stream().map(StockQuote::getCode).toList();
            Map<String, List<StockQuoteHistoryProjection>> historyMap = groupHistoriesByCode(
                    stockQuoteHistoryRepository.findByTradeDateInAndCodeInOrderByTradeDateAsc(recentDates, codes)
            );

            for (StockQuote stock : batch) {
                BigDecimal[] closePrices = extractClosePrices(
                        historyMap.getOrDefault(stock.getCode(), Collections.emptyList())
                );
                result.add(calculateSignal(stock, closePrices, fastPeriod, slowPeriod, signalPeriod, needDays));
            }
        }
        return result;
    }

    public List<StockTradeBacktestVO> backtest(
            int fastPeriod,
            int slowPeriod,
            int signalPeriod,
            int recentYears,
            List<StockQuote> stocks
    ) {
        validateParameters(fastPeriod, slowPeriod, signalPeriod);
        List<StockTradeBacktestVO> result = new ArrayList<>();
        int needDays = recentYears * 250 + warmupDays(slowPeriod, signalPeriod);
        List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(needDays);
        TTest tTest = new TTest();

        for (int batchStart = 0; batchStart < stocks.size(); batchStart += BATCH_SIZE) {
            List<StockQuote> batch = stocks.subList(batchStart, Math.min(stocks.size(), batchStart + BATCH_SIZE));
            List<String> codes = batch.stream().map(StockQuote::getCode).toList();
            Map<String, List<StockQuoteHistoryProjection>> historyMap = groupHistoriesByCode(
                    stockQuoteHistoryRepository.findByTradeDateInAndCodeInOrderByTradeDateAsc(recentDates, codes)
            );

            for (StockQuote stock : batch) {
                result.add(backtestSingle(
                        stock,
                        extractClosePrices(historyMap.getOrDefault(stock.getCode(), Collections.emptyList())),
                        fastPeriod,
                        slowPeriod,
                        signalPeriod,
                        recentYears,
                        tTest
                ));
            }
        }
        return result;
    }

    public StockTradeBacktestVO backtestSingle(
            StockQuote stock,
            BigDecimal[] closePrices,
            int fastPeriod,
            int slowPeriod,
            int signalPeriod,
            int recentYears,
            TTest tTest
    ) {
        validateParameters(fastPeriod, slowPeriod, signalPeriod);
        int warmupDays = warmupDays(slowPeriod, signalPeriod);
        int needDays = recentYears * 250 + warmupDays;
        int startIndex = Math.max(0, closePrices.length - needDays);
        int tradeStartIndex = startIndex + warmupDays;

        if (closePrices.length <= tradeStartIndex || containsInvalidPrice(closePrices, startIndex)) {
            return insufficientSample(stock);
        }

        MacdSeries series = calculateSeries(closePrices, startIndex, fastPeriod, slowPeriod, signalPeriod);
        BigDecimal netValue = BigDecimal.ONE;
        BigDecimal costPrice = null;
        List<Double> tradeReturns = new ArrayList<>();
        int winCount = 0;
        double tradeReturnSum = 0D;

        for (int i = tradeStartIndex; i < closePrices.length; i++) {
            int seriesIndex = i - startIndex;
            double previousGap = series.dif[seriesIndex - 1] - series.dea[seriesIndex - 1];
            double currentGap = series.dif[seriesIndex] - series.dea[seriesIndex];

            if (previousGap <= 0D && currentGap > 0D && costPrice == null) {
                costPrice = closePrices[i];
            } else if (previousGap >= 0D && currentGap < 0D && costPrice != null) {
                BigDecimal tradeReturn = closePrices[i].subtract(costPrice)
                        .divide(costPrice, RETURN_SCALE, RoundingMode.HALF_UP);
                double tradeReturnValue = tradeReturn.doubleValue();
                tradeReturns.add(tradeReturnValue);
                tradeReturnSum += tradeReturnValue;
                if (tradeReturn.signum() > 0) {
                    winCount++;
                }
                netValue = netValue.multiply(BigDecimal.ONE.add(tradeReturn));
                costPrice = null;
            }
        }

        if (costPrice != null) {
            BigDecimal tradeReturn = closePrices[closePrices.length - 1].subtract(costPrice)
                    .divide(costPrice, RETURN_SCALE, RoundingMode.HALF_UP);
            double tradeReturnValue = tradeReturn.doubleValue();
            tradeReturns.add(tradeReturnValue);
            tradeReturnSum += tradeReturnValue;
            if (tradeReturn.signum() > 0) {
                winCount++;
            }
            netValue = netValue.multiply(BigDecimal.ONE.add(tradeReturn));
        }

        int tradeCount = tradeReturns.size();
        BigDecimal winRate = tradeCount == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(winCount)
                .divide(BigDecimal.valueOf(tradeCount), 4, RoundingMode.HALF_UP);
        Double tValue = null;
        Double pValue = null;
        String reliability = "样本不足";

        if (tradeCount >= 2) {
            double[] samples = tradeReturns.stream().mapToDouble(Double::doubleValue).toArray();
            double sampleMean = tradeReturnSum / tradeCount;
            try {
                tValue = tTest.t(0D, samples);
                double twoSidedPValue = tTest.tTest(0D, samples);
                if (!Double.isFinite(tValue) || !Double.isFinite(twoSidedPValue)) {
                    tValue = null;
                    reliability = sampleMean > 0 ? "低(方差0)" : "低";
                } else {
                    pValue = tValue > 0 ? twoSidedPValue / 2D : 1D - twoSidedPValue / 2D;
                    reliability = sampleMean > 0 && pValue < 0.05 ? "高"
                            : sampleMean > 0 && pValue < 0.10 ? "中" : "低";
                }
            } catch (Exception ignored) {
                reliability = sampleMean > 0 ? "低(方差0)" : "低";
            }
        }

        return new StockTradeBacktestVO(
                stock.getCode(), stock.getName(), netValue.subtract(BigDecimal.ONE), tradeCount,
                winRate, tValue, pValue, reliability, stock.getLatestPrice(), stock.getPir(), stock.getCreatedAt()
        );
    }

    public Map<String, List<StockQuoteHistoryProjection>> groupHistoriesByCode(
            List<StockQuoteHistoryProjection> histories
    ) {
        Map<String, List<StockQuoteHistoryProjection>> result = new HashMap<>();
        for (StockQuoteHistoryProjection history : histories) {
            result.computeIfAbsent(history.getCode(), key -> new ArrayList<>()).add(history);
        }
        return result;
    }

    public BigDecimal[] extractClosePrices(List<StockQuoteHistoryProjection> histories) {
        BigDecimal[] result = new BigDecimal[histories.size()];
        for (int i = 0; i < histories.size(); i++) {
            result[i] = histories.get(i).getClosePrice();
        }
        return result;
    }

    private StockTradeSignalVO calculateSignal(
            StockQuote stock,
            BigDecimal[] closePrices,
            int fastPeriod,
            int slowPeriod,
            int signalPeriod,
            int needDays
    ) {
        if (closePrices.length < needDays || containsInvalidPrice(closePrices, 0)) {
            return new StockTradeSignalVO(
                    stock.getCode(), stock.getName(), TradeSignal.HOLD.name(), stock.getLatestPrice(), stock.getPir()
            );
        }

        MacdSeries series = calculateSeries(closePrices, 0, fastPeriod, slowPeriod, signalPeriod);
        int current = series.dif.length - 1;
        int previous = current - 1;
        double previousGap = series.dif[previous] - series.dea[previous];
        double currentGap = series.dif[current] - series.dea[current];
        TradeSignal signal = TradeSignal.HOLD;
        if (previousGap <= 0D && currentGap > 0D) {
            signal = TradeSignal.BUY;
        } else if (previousGap >= 0D && currentGap < 0D) {
            signal = TradeSignal.SELL;
        }

        return new StockTradeSignalVO(
                stock.getCode(), stock.getName(), signal.name(), stock.getLatestPrice(), stock.getPir(),
                decimal(series.dif[current]), decimal(series.dea[current]), decimal(currentGap * 2D)
        );
    }

    private MacdSeries calculateSeries(
            BigDecimal[] closePrices,
            int startIndex,
            int fastPeriod,
            int slowPeriod,
            int signalPeriod
    ) {
        int length = closePrices.length - startIndex;
        double[] dif = new double[length];
        double[] dea = new double[length];
        double fastEma = closePrices[startIndex].doubleValue();
        double slowEma = fastEma;
        double fastAlpha = 2D / (fastPeriod + 1D);
        double slowAlpha = 2D / (slowPeriod + 1D);
        double signalAlpha = 2D / (signalPeriod + 1D);

        for (int i = 1; i < length; i++) {
            double close = closePrices[startIndex + i].doubleValue();
            fastEma += fastAlpha * (close - fastEma);
            slowEma += slowAlpha * (close - slowEma);
            dif[i] = fastEma - slowEma;
            dea[i] = dea[i - 1] + signalAlpha * (dif[i] - dea[i - 1]);
        }
        return new MacdSeries(dif, dea);
    }

    private void validateParameters(int fastPeriod, int slowPeriod, int signalPeriod) {
        if (fastPeriod <= 0 || slowPeriod <= 0 || signalPeriod <= 0 || fastPeriod >= slowPeriod) {
            throw new BusinessException(ExceptionEnum.STOCK_STRATEGY_MACD_PARAMS_ILLEGAL);
        }
    }

    private int warmupDays(int slowPeriod, int signalPeriod) {
        return slowPeriod * 3 + signalPeriod;
    }

    private boolean containsInvalidPrice(BigDecimal[] closePrices, int startIndex) {
        for (int i = startIndex; i < closePrices.length; i++) {
            if (closePrices[i] == null || closePrices[i].signum() <= 0) {
                return true;
            }
        }
        return false;
    }

    private StockTradeBacktestVO insufficientSample(StockQuote stock) {
        return new StockTradeBacktestVO(
                stock.getCode(), stock.getName(), BigDecimal.ZERO, 0, BigDecimal.ZERO,
                null, null, "样本不足", stock.getLatestPrice(), stock.getPir(), stock.getCreatedAt()
        );
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(INDICATOR_SCALE, RoundingMode.HALF_UP);
    }

    private static class MacdSeries {
        private final double[] dif;
        private final double[] dea;

        private MacdSeries(double[] dif, double[] dea) {
            this.dif = dif;
            this.dea = dea;
        }
    }
}
