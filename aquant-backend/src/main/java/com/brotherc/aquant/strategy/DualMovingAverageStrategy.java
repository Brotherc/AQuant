package com.brotherc.aquant.strategy;

import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.entity.StockQuoteHistory;
import com.brotherc.aquant.enums.TradeSignal;
import com.brotherc.aquant.exception.BusinessException;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.vo.strategy.StockTradeSignalVO;
import com.brotherc.aquant.model.vo.strategy.StockTradeBacktestVO;
import com.brotherc.aquant.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.repository.StockQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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

        LocalDateTime maxTime = stockQuoteRepository.findMaxCreatedAt();
        List<StockQuote> stocks = stockQuoteRepository.findByCreatedAt(maxTime);

        int needDays = maLong + 1;

        for (StockQuote stock : stocks) {

            String code = stock.getCode();
            String name = stock.getName();

            List<StockQuoteHistory> list = stockQuoteHistoryRepository.findLatestByCode(code, needDays);

            if (list.size() < needDays) {
                result.add(new StockTradeSignalVO(code, name, TradeSignal.HOLD.name(), stock.getLatestPrice(),
                        stock.getPir()));
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

            result.add(new StockTradeSignalVO(code, name, signal.name(), stock.getLatestPrice(), stock.getPir()));
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

        for (StockQuote stock : stocks) {
            String code = stock.getCode();
            String name = stock.getName();

            List<StockQuoteHistory> list = stockQuoteHistoryRepository.findLatestByCode(code, needDays);

            if (list.size() <= maLong) {
                result.add(
                        new StockTradeBacktestVO(code, name, BigDecimal.ZERO, 0, BigDecimal.ZERO, null, null, "样本不足",
                                stock.getLatestPrice(), stock.getPir()));
                continue;
            }

            // 倒序转正序以模拟时间推移
            Collections.reverse(list);

            BigDecimal netValue = BigDecimal.ONE;
            TradeSignal currentPosition = TradeSignal.HOLD;
            BigDecimal costPrice = BigDecimal.ZERO;

            // 收集每次闭环交易的收益率作为样本
            List<Double> tradeReturns = new ArrayList<>();

            for (int i = maLong; i < list.size(); i++) {
                BigDecimal todayShort = avg(list.subList(i - maShort + 1, i + 1));
                BigDecimal todayLong = avg(list.subList(i - maLong + 1, i + 1));

                BigDecimal yesterdayShort = avg(list.subList(i - maShort, i));
                BigDecimal yesterdayLong = avg(list.subList(i - maLong, i));

                // 金叉 -> 买入
                if (yesterdayShort.compareTo(yesterdayLong) <= 0 && todayShort.compareTo(todayLong) > 0) {
                    if (currentPosition == TradeSignal.HOLD) {
                        currentPosition = TradeSignal.BUY;
                        costPrice = list.get(i).getClosePrice();
                    }
                }
                // 死叉 -> 卖出
                else if (yesterdayShort.compareTo(yesterdayLong) >= 0 && todayShort.compareTo(todayLong) < 0) {
                    if (currentPosition == TradeSignal.BUY && costPrice.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal sellPrice = list.get(i).getClosePrice();
                        BigDecimal tradeReturn = sellPrice.subtract(costPrice).divide(costPrice, 4,
                                RoundingMode.HALF_UP);

                        tradeReturns.add(tradeReturn.doubleValue());
                        netValue = netValue.multiply(BigDecimal.ONE.add(tradeReturn));

                        currentPosition = TradeSignal.HOLD;
                        costPrice = BigDecimal.ZERO;
                    }
                }
            }

            // 最后一天如果仍持有，按收盘价计算浮动盈亏
            if (currentPosition == TradeSignal.BUY && costPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal lastPrice = list.get(list.size() - 1).getClosePrice();
                BigDecimal tradeReturn = lastPrice.subtract(costPrice).divide(costPrice, 4, RoundingMode.HALF_UP);
                tradeReturns.add(tradeReturn.doubleValue());
                netValue = netValue.multiply(BigDecimal.ONE.add(tradeReturn));
            }

            BigDecimal totalReturn = netValue.subtract(BigDecimal.ONE);

            // 计算统计学指标
            int tradeCount = tradeReturns.size();
            BigDecimal winRate = BigDecimal.ZERO;
            Double tValue = null;
            Double pValue = null;
            String reliability = "样本不足";

            if (tradeCount > 0) {
                long winCount = tradeReturns.stream().filter(r -> r > 0).count();
                winRate = BigDecimal.valueOf(winCount).divide(BigDecimal.valueOf(tradeCount), 4, RoundingMode.HALF_UP);
            }

            if (tradeCount >= 2) {
                double[] sampleArray = tradeReturns.stream().mapToDouble(Double::doubleValue).toArray();
                org.apache.commons.math3.stat.inference.TTest tTest = new org.apache.commons.math3.stat.inference.TTest();

                // 使用单样本的均值与0比较
                double sampleMean = tradeReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                try {
                    tValue = tTest.t(0.0, sampleArray);
                    // tTest.tTest 返回双尾 p 值，由于我们想证明均值 > 0 (右尾)，当 t > 0 时单尾为: 双尾/2
                    double twoSidedPValue = tTest.tTest(0.0, sampleArray);

                    if (tValue > 0) {
                        pValue = twoSidedPValue / 2.0;
                    } else {
                        // 如果 t <= 0，意味着样本均值可能小于或等于 0。
                        // 由于我们假设均值是否大于0，这种情况下 p值一定大于 0.5。
                        pValue = 1.0 - (twoSidedPValue / 2.0);
                    }

                    if (sampleMean > 0 && pValue != null && pValue < 0.05) {
                        reliability = "高"; // 显著大于0且p<0.05，高度可靠
                    } else if (sampleMean > 0 && pValue != null && pValue < 0.10) {
                        reliability = "中"; // 边缘显著
                    } else {
                        reliability = "低";
                    }

                } catch (Exception e) {
                    // 方差为0等特殊情况
                    reliability = sampleMean > 0 ? "低(方差0)" : "低";
                }
            }

            result.add(new StockTradeBacktestVO(code, name, totalReturn, tradeCount, winRate, tValue, pValue,
                    reliability, stock.getLatestPrice(), stock.getPir()));
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
