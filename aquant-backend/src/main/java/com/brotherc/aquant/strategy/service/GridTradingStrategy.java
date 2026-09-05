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
public class GridTradingStrategy {

    private static final int SIGNAL_LOOKBACK_DAYS = 120;
    private static final int BATCH_SIZE = 500;
    private static final int CALCULATION_SCALE = 10;

    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;

    public List<StockTradeSignalVO> calculate(BigDecimal gridRate, int gridCount, List<StockQuote> stocks) {
        validateParameters(gridRate, gridCount);
        List<StockTradeSignalVO> result = new ArrayList<>();
        List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(SIGNAL_LOOKBACK_DAYS);

        for (int batchStart = 0; batchStart < stocks.size(); batchStart += BATCH_SIZE) {
            List<StockQuote> batch = stocks.subList(batchStart, Math.min(stocks.size(), batchStart + BATCH_SIZE));
            List<String> codes = batch.stream().map(StockQuote::getCode).toList();
            Map<String, List<StockQuoteHistoryProjection>> historyMap = groupHistoriesByCode(
                    stockQuoteHistoryRepository.findByTradeDateInAndCodeInOrderByTradeDateAsc(recentDates, codes)
            );
            for (StockQuote stock : batch) {
                result.add(calculateSignal(
                        stock,
                        extractClosePrices(historyMap.getOrDefault(stock.getCode(), Collections.emptyList())),
                        gridRate,
                        gridCount
                ));
            }
        }
        return result;
    }

    public List<StockTradeBacktestVO> backtest(
            BigDecimal gridRate,
            int gridCount,
            int recentYears,
            List<StockQuote> stocks
    ) {
        validateParameters(gridRate, gridCount);
        List<StockTradeBacktestVO> result = new ArrayList<>();
        List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(recentYears * 250 + 1);
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
                        gridRate,
                        gridCount,
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
            BigDecimal gridRate,
            int gridCount,
            int recentYears,
            TTest tTest
    ) {
        validateParameters(gridRate, gridCount);
        int needDays = recentYears * 250 + 1;
        int startIndex = Math.max(0, closePrices.length - needDays);
        if (closePrices.length - startIndex < 2 || containsInvalidPrice(closePrices, startIndex)) {
            return insufficientSample(stock);
        }

        BigDecimal initialPrice = closePrices[startIndex];
        BigDecimal cash = new BigDecimal("0.5");
        BigDecimal shares = new BigDecimal("0.5").divide(initialPrice, CALCULATION_SCALE, RoundingMode.HALF_UP);
        BigDecimal averageCost = initialPrice;
        BigDecimal referencePrice = initialPrice;
        BigDecimal unitCapital = BigDecimal.ONE.divide(
                BigDecimal.valueOf(gridCount * 2L), CALCULATION_SCALE, RoundingMode.HALF_UP
        );
        int positionLevel = 0;
        int winCount = 0;
        double tradeReturnSum = 0D;
        List<Double> tradeReturns = new ArrayList<>();

        for (int i = startIndex + 1; i < closePrices.length; i++) {
            BigDecimal price = closePrices[i];
            BigDecimal buyTrigger = referencePrice.multiply(BigDecimal.ONE.subtract(gridRate));
            BigDecimal sellTrigger = referencePrice.multiply(BigDecimal.ONE.add(gridRate));

            if (price.compareTo(buyTrigger) <= 0 && positionLevel < gridCount
                    && cash.compareTo(unitCapital) >= 0) {
                BigDecimal boughtShares = unitCapital.divide(price, CALCULATION_SCALE, RoundingMode.HALF_UP);
                BigDecimal previousCost = averageCost.multiply(shares);
                shares = shares.add(boughtShares);
                averageCost = previousCost.add(unitCapital).divide(shares, CALCULATION_SCALE, RoundingMode.HALF_UP);
                cash = cash.subtract(unitCapital);
                referencePrice = buyTrigger;
                positionLevel++;
            } else if (price.compareTo(sellTrigger) >= 0 && positionLevel > -gridCount && shares.signum() > 0) {
                BigDecimal sellShares = unitCapital.divide(price, CALCULATION_SCALE, RoundingMode.HALF_UP)
                        .min(shares);
                BigDecimal proceeds = sellShares.multiply(price);
                BigDecimal tradeReturn = price.subtract(averageCost)
                        .divide(averageCost, CALCULATION_SCALE, RoundingMode.HALF_UP);
                double tradeReturnValue = tradeReturn.doubleValue();
                tradeReturns.add(tradeReturnValue);
                tradeReturnSum += tradeReturnValue;
                if (tradeReturn.signum() > 0) {
                    winCount++;
                }
                shares = shares.subtract(sellShares);
                cash = cash.add(proceeds);
                if (shares.signum() == 0) {
                    averageCost = BigDecimal.ZERO;
                }
                referencePrice = sellTrigger;
                positionLevel--;
            }
        }

        BigDecimal finalValue = cash.add(shares.multiply(closePrices[closePrices.length - 1]));
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
                stock.getCode(), stock.getName(), finalValue.subtract(BigDecimal.ONE), tradeCount,
                winRate, tValue, pValue, reliability, stock.getLatestPrice(), stock.getPir(), stock.getCreatedAt()
        );
    }

    private StockTradeSignalVO calculateSignal(
            StockQuote stock,
            BigDecimal[] closePrices,
            BigDecimal gridRate,
            int gridCount
    ) {
        if (closePrices.length < 2 || containsInvalidPrice(closePrices, 0)) {
            return new StockTradeSignalVO(
                    stock.getCode(), stock.getName(), TradeSignal.HOLD.name(), stock.getLatestPrice(), stock.getPir()
            );
        }

        BigDecimal referencePrice = closePrices[0];
        int positionLevel = 0;
        TradeSignal latestSignal = TradeSignal.HOLD;
        for (int i = 1; i < closePrices.length; i++) {
            latestSignal = TradeSignal.HOLD;
            BigDecimal buyTrigger = referencePrice.multiply(BigDecimal.ONE.subtract(gridRate));
            BigDecimal sellTrigger = referencePrice.multiply(BigDecimal.ONE.add(gridRate));
            if (closePrices[i].compareTo(buyTrigger) <= 0 && positionLevel < gridCount) {
                referencePrice = buyTrigger;
                positionLevel++;
                latestSignal = TradeSignal.BUY;
            } else if (closePrices[i].compareTo(sellTrigger) >= 0 && positionLevel > -gridCount) {
                referencePrice = sellTrigger;
                positionLevel--;
                latestSignal = TradeSignal.SELL;
            }
        }

        BigDecimal lowerGridPrice = referencePrice.multiply(BigDecimal.ONE.subtract(gridRate));
        BigDecimal upperGridPrice = referencePrice.multiply(BigDecimal.ONE.add(gridRate));
        return new StockTradeSignalVO(
                stock.getCode(), stock.getName(), latestSignal.name(), stock.getLatestPrice(), stock.getPir(),
                referencePrice.setScale(4, RoundingMode.HALF_UP),
                lowerGridPrice.setScale(4, RoundingMode.HALF_UP),
                upperGridPrice.setScale(4, RoundingMode.HALF_UP),
                positionLevel
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

    private boolean containsInvalidPrice(BigDecimal[] prices, int startIndex) {
        for (int i = startIndex; i < prices.length; i++) {
            if (prices[i] == null || prices[i].signum() <= 0) {
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

    private void validateParameters(BigDecimal gridRate, int gridCount) {
        if (gridRate == null || gridRate.compareTo(BigDecimal.ZERO) <= 0
                || gridRate.compareTo(new BigDecimal("0.5")) >= 0
                || gridCount < 1 || gridCount > 50) {
            throw new BusinessException(ExceptionEnum.STOCK_STRATEGY_GRID_PARAMS_ILLEGAL);
        }
    }
}
