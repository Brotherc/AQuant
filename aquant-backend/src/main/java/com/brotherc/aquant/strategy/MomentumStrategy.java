package com.brotherc.aquant.strategy;

import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.repository.projection.StockQuoteHistoryProjection;
import com.brotherc.aquant.enums.TradeSignal;
import com.brotherc.aquant.model.vo.strategy.StockTradeBacktestVO;
import com.brotherc.aquant.model.vo.strategy.StockTradeSignalVO;
import com.brotherc.aquant.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.repository.StockQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MomentumStrategy {

    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;

    /**
     * 计算所有股票的动量值并生成交易信号
     *
     * @param lookbackDays 回望天数
     * @param threshold    信号阈值(%)
     * @return 信号列表
     */
    public List<StockTradeSignalVO> calculate(int lookbackDays, BigDecimal threshold, List<StockQuote> stocks) {
        List<StockTradeSignalVO> result = new ArrayList<>();

        int needDays = lookbackDays + 1;
        List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(needDays);

        int batchSize = 500;
        for (int b = 0; b < stocks.size(); b += batchSize) {
            List<StockQuote> batch = stocks.subList(b, Math.min(stocks.size(), b + batchSize));
            List<String> codes = batch.stream().map(StockQuote::getCode).toList();
            
            List<StockQuoteHistoryProjection> histories = stockQuoteHistoryRepository
                    .findByTradeDateInAndCodeInOrderByTradeDateAsc(recentDates, codes);
            Map<String, List<StockQuoteHistoryProjection>> historyMap = histories.stream()
                    .collect(Collectors.groupingBy(StockQuoteHistoryProjection::getCode));

            for (StockQuote stock : batch) {
                String code = stock.getCode();
                String name = stock.getName();

                List<StockQuoteHistoryProjection> list = historyMap.getOrDefault(code, new ArrayList<>());

                if (list.size() < needDays) {
                    result.add(new StockTradeSignalVO(code, name, TradeSignal.HOLD.name(),
                            stock.getLatestPrice(), stock.getPir(), null));
                    continue;
                }

                // 批次数据自带 ASC 排序，已无需逆序

                BigDecimal todayClose = list.get(list.size() - 1).getClosePrice();
            BigDecimal pastClose = list.get(0).getClosePrice();

            if (pastClose == null || pastClose.compareTo(BigDecimal.ZERO) == 0 || todayClose == null) {
                result.add(new StockTradeSignalVO(code, name, TradeSignal.HOLD.name(),
                        stock.getLatestPrice(), stock.getPir(), null));
                continue;
            }

            BigDecimal momentumValue = todayClose.subtract(pastClose)
                    .divide(pastClose, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            TradeSignal signal;
            if (momentumValue.compareTo(threshold) > 0) {
                signal = TradeSignal.BUY;
            } else if (momentumValue.compareTo(threshold.negate()) < 0) {
                signal = TradeSignal.SELL;
            } else {
                signal = TradeSignal.HOLD;
            }

            result.add(new StockTradeSignalVO(code, name, signal.name(),
                    stock.getLatestPrice(), stock.getPir(), momentumValue));
            }
        }

        return result;
    }

    /**
     * 动量策略历史回测
     */
    public List<StockTradeBacktestVO> backtest(int lookbackDays, int recentYears, List<StockQuote> stocks) {
        List<StockTradeBacktestVO> result = new ArrayList<>();
        int needDays = recentYears * 250 + lookbackDays;
        List<String> recentDates = stockQuoteHistoryRepository.findRecentTradeDates(needDays);

        int batchSize = 500;
        for (int b = 0; b < stocks.size(); b += batchSize) {
            List<StockQuote> batch = stocks.subList(b, Math.min(stocks.size(), b + batchSize));
            List<String> codes = batch.stream().map(StockQuote::getCode).toList();
            
            List<StockQuoteHistoryProjection> histories = stockQuoteHistoryRepository
                    .findByTradeDateInAndCodeInOrderByTradeDateAsc(recentDates, codes);
            Map<String, List<StockQuoteHistoryProjection>> historyMap = histories.stream()
                    .collect(Collectors.groupingBy(StockQuoteHistoryProjection::getCode));

            for (StockQuote stock : batch) {
                String code = stock.getCode();
                String name = stock.getName();

                List<StockQuoteHistoryProjection> list = historyMap.getOrDefault(code, new ArrayList<>());

                if (list.size() <= lookbackDays) {
                    result.add(new StockTradeBacktestVO(code, name, BigDecimal.ZERO, 0,
                            BigDecimal.ZERO, null, null, "样本不足",
                            stock.getLatestPrice(), stock.getPir(), stock.getCreatedAt()));
                    continue;
                }

                // 批次数据自带 ASC 排序，已无需逆序

            BigDecimal netValue = BigDecimal.ONE;
            boolean inPosition = false;
            BigDecimal costPrice = BigDecimal.ZERO;
            List<Double> tradeReturns = new ArrayList<>();
            BigDecimal prevMomentum = null;

            for (int i = lookbackDays; i < list.size(); i++) {
                BigDecimal todayClose = list.get(i).getClosePrice();
                BigDecimal pastClose = list.get(i - lookbackDays).getClosePrice();

                if (todayClose == null || pastClose == null || pastClose.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                BigDecimal momentum = todayClose.subtract(pastClose)
                        .divide(pastClose, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                if (prevMomentum != null) {
                    if (prevMomentum.compareTo(BigDecimal.ZERO) <= 0
                            && momentum.compareTo(BigDecimal.ZERO) > 0) {
                        if (!inPosition) {
                            inPosition = true;
                            costPrice = todayClose;
                        }
                    } else if (prevMomentum.compareTo(BigDecimal.ZERO) >= 0
                            && momentum.compareTo(BigDecimal.ZERO) < 0) {
                        if (inPosition && costPrice.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal tradeReturn = todayClose.subtract(costPrice)
                                    .divide(costPrice, 4, RoundingMode.HALF_UP);
                            tradeReturns.add(tradeReturn.doubleValue());
                            netValue = netValue.multiply(BigDecimal.ONE.add(tradeReturn));
                            inPosition = false;
                            costPrice = BigDecimal.ZERO;
                        }
                    }
                }

                prevMomentum = momentum;
            }

            if (inPosition && costPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal lastPrice = list.get(list.size() - 1).getClosePrice();
                if (lastPrice != null) {
                    BigDecimal tradeReturn = lastPrice.subtract(costPrice)
                            .divide(costPrice, 4, RoundingMode.HALF_UP);
                    tradeReturns.add(tradeReturn.doubleValue());
                    netValue = netValue.multiply(BigDecimal.ONE.add(tradeReturn));
                }
            }

            BigDecimal totalReturn = netValue.subtract(BigDecimal.ONE);

            int tradeCount = tradeReturns.size();
            BigDecimal winRate = BigDecimal.ZERO;
            Double tValue = null;
            Double pValue = null;
            String reliability = "样本不足";

            if (tradeCount > 0) {
                long winCount = tradeReturns.stream().filter(r -> r > 0).count();
                winRate = BigDecimal.valueOf(winCount)
                        .divide(BigDecimal.valueOf(tradeCount), 4, RoundingMode.HALF_UP);
            }

            if (tradeCount >= 2) {
                double[] sampleArray = tradeReturns.stream().mapToDouble(Double::doubleValue).toArray();
                org.apache.commons.math3.stat.inference.TTest tTest =
                        new org.apache.commons.math3.stat.inference.TTest();
                double sampleMean = tradeReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                try {
                    tValue = tTest.t(0.0, sampleArray);
                    double twoSidedPValue = tTest.tTest(0.0, sampleArray);
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
                } catch (Exception e) {
                    reliability = sampleMean > 0 ? "低(方差0)" : "低";
                }
            }

            result.add(new StockTradeBacktestVO(code, name, totalReturn, tradeCount, winRate,
                    tValue, pValue, reliability, stock.getLatestPrice(), stock.getPir(), stock.getCreatedAt()));
            }
        }

        return result;
    }
}
