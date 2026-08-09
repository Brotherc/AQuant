package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.model.vo.sentiment.MarketSentimentVO;
import com.brotherc.aquant.repository.StockQuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketSentimentService {

    private final StockQuoteRepository stockQuoteRepository;

    /**
     * 基于本地 stock_quote 股票实时行情表，统计大盘分析与 15 个高密度精细化涨跌分布区间柱状图数据
     */
    @Transactional(readOnly = true)
    public MarketSentimentVO getMarketSentiment() {
        List<StockQuote> quotes = stockQuoteRepository.findAll();
        MarketSentimentVO vo = new MarketSentimentVO();

        if (quotes.isEmpty()) {
            vo.setTotalCount(0);
            vo.setRiseCount(0);
            vo.setFallCount(0);
            vo.setFlatCount(0);
            vo.setStrongRiseCount(0);
            vo.setStrongFallCount(0);
            vo.setLimitUpCount(0);
            vo.setUp8ToMaxCount(0);
            vo.setUp6To8Count(0);
            vo.setUp4To6Count(0);
            vo.setUp2To4Count(0);
            vo.setUp1To2Count(0);
            vo.setUp0To1Count(0);
            vo.setDown0To1Count(0);
            vo.setDown1To2Count(0);
            vo.setDown2To4Count(0);
            vo.setDown4To6Count(0);
            vo.setDown6To8Count(0);
            vo.setDown8ToMinCount(0);
            vo.setLimitDownCount(0);
            vo.setTotalTurnover(BigDecimal.ZERO);
            vo.setTurnoverChangeAmount(BigDecimal.ZERO);
            vo.setTemperature(50);
            vo.setTemperatureLabel("温和震荡");
            return vo;
        }

        int riseCount = 0;
        int fallCount = 0;
        int flatCount = 0;

        int strongRiseCount = 0;
        int strongFallCount = 0;

        int limitUpCount = 0;
        int up8ToMaxCount = 0;
        int up6To8Count = 0;
        int up4To6Count = 0;
        int up2To4Count = 0;
        int up1To2Count = 0;
        int up0To1Count = 0;

        int down0To1Count = 0;
        int down1To2Count = 0;
        int down2To4Count = 0;
        int down4To6Count = 0;
        int down6To8Count = 0;
        int down8ToMinCount = 0;
        int limitDownCount = 0;

        BigDecimal totalTurnover = BigDecimal.ZERO;

        for (StockQuote quote : quotes) {
            BigDecimal changePercent = quote.getChangePercent();
            if (changePercent == null) {
                continue;
            }

            if (quote.getTurnover() != null) {
                totalTurnover = totalTurnover.add(quote.getTurnover());
            }

            double pct = changePercent.doubleValue();

            if (pct >= 9.8) {
                limitUpCount++;
                riseCount++;
                strongRiseCount++;
            } else if (pct >= 8.0) {
                up8ToMaxCount++;
                riseCount++;
                strongRiseCount++;
            } else if (pct >= 6.0) {
                up6To8Count++;
                riseCount++;
                strongRiseCount++;
            } else if (pct >= 4.0) {
                up4To6Count++;
                riseCount++;
            } else if (pct >= 2.0) {
                up2To4Count++;
                riseCount++;
            } else if (pct >= 1.0) {
                up1To2Count++;
                riseCount++;
            } else if (pct > 0.0) {
                up0To1Count++;
                riseCount++;
            } else if (pct == 0.0) {
                flatCount++;
            } else if (pct > -1.0) {
                down0To1Count++;
                fallCount++;
            } else if (pct > -2.0) {
                down1To2Count++;
                fallCount++;
            } else if (pct > -4.0) {
                down2To4Count++;
                fallCount++;
            } else if (pct > -6.0) {
                down4To6Count++;
                fallCount++;
                strongFallCount++;
            } else if (pct > -8.0) {
                down6To8Count++;
                fallCount++;
                strongFallCount++;
            } else if (pct > -9.8) {
                down8ToMinCount++;
                fallCount++;
                strongFallCount++;
            } else {
                limitDownCount++;
                fallCount++;
                strongFallCount++;
            }
        }

        int totalValid = riseCount + fallCount + flatCount;
        vo.setTotalCount(totalValid);
        vo.setRiseCount(riseCount);
        vo.setFallCount(fallCount);
        vo.setFlatCount(flatCount);
        vo.setStrongRiseCount(strongRiseCount);
        vo.setStrongFallCount(strongFallCount);

        vo.setLimitUpCount(limitUpCount);
        vo.setUp8ToMaxCount(up8ToMaxCount);
        vo.setUp6To8Count(up6To8Count);
        vo.setUp4To6Count(up4To6Count);
        vo.setUp2To4Count(up2To4Count);
        vo.setUp1To2Count(up1To2Count);
        vo.setUp0To1Count(up0To1Count);

        vo.setDown0To1Count(down0To1Count);
        vo.setDown1To2Count(down1To2Count);
        vo.setDown2To4Count(down2To4Count);
        vo.setDown4To6Count(down4To6Count);
        vo.setDown6To8Count(down6To8Count);
        vo.setDown8ToMinCount(down8ToMinCount);
        vo.setLimitDownCount(limitDownCount);

        vo.setTotalTurnover(totalTurnover);
        vo.setTurnoverChangeAmount(totalTurnover.multiply(new BigDecimal("0.05")));

        // 计算赚钱效应温度得分 (0 ~ 100)
        double riseRatio = totalValid > 0 ? (riseCount * 100.0 / totalValid) : 50.0;
        double limitFactor = (limitUpCount + limitDownCount > 0) ? (limitUpCount * 100.0 / (limitUpCount + limitDownCount)) : 50.0;

        int temperature = (int) Math.round(riseRatio * 0.7 + limitFactor * 0.3);
        temperature = Math.max(0, Math.min(100, temperature));

        String label;
        if (temperature >= 80) {
            label = "极度狂热";
        } else if (temperature >= 60) {
            label = "交投活跃";
        } else if (temperature >= 40) {
            label = "温和震荡";
        } else if (temperature >= 20) {
            label = "情绪低迷";
        } else {
            label = "冰点避险";
        }

        vo.setTemperature(temperature);
        vo.setTemperatureLabel(label);

        return vo;
    }
}
