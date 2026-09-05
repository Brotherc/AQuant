package com.brotherc.aquant.strategy.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.stock.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.strategy.model.vo.StockTradeBacktestVO;
import org.apache.commons.math3.stat.inference.TTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GridTradingStrategyTest {

    private final GridTradingStrategy strategy = new GridTradingStrategy(
            mock(StockQuoteHistoryRepository.class)
    );

    @Test
    void shouldRejectInvalidGridParameters() {
        assertThrows(BusinessException.class, () -> strategy.backtestSingle(
                stock(), prices(300), BigDecimal.ZERO, 5, 1, new TTest()
        ));
        assertThrows(BusinessException.class, () -> strategy.backtestSingle(
                stock(), prices(300), new BigDecimal("0.03"), 0, 1, new TTest()
        ));
    }

    @Test
    void shouldGenerateTradesForOscillatingPrices() {
        StockTradeBacktestVO result = strategy.backtestSingle(
                stock(), prices(700), new BigDecimal("0.03"), 5, 2, new TTest()
        );

        assertTrue(result.getTradeCount() > 2);
        assertNotNull(result.getTotalReturn());
        assertTrue(result.getWinRate().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.getWinRate().compareTo(BigDecimal.ONE) <= 0);
    }

    @Test
    void shouldReturnInsufficientSampleWhenHistoryIsTooShort() {
        StockTradeBacktestVO result = strategy.backtestSingle(
                stock(), new BigDecimal[]{new BigDecimal("100")},
                new BigDecimal("0.03"), 5, 1, new TTest()
        );

        assertEquals("样本不足", result.getReliability());
        assertEquals(0, result.getTradeCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalReturn()));
    }

    private StockQuote stock() {
        StockQuote stock = new StockQuote();
        stock.setCode("sh600000");
        stock.setName("测试股票");
        stock.setLatestPrice(new BigDecimal("100"));
        stock.setPir(new BigDecimal("0.5"));
        stock.setCreatedAt(LocalDateTime.of(2026, 9, 1, 15, 0));
        return stock;
    }

    private BigDecimal[] prices(int size) {
        BigDecimal[] prices = new BigDecimal[size];
        for (int i = 0; i < size; i++) {
            prices[i] = BigDecimal.valueOf(100D + Math.sin(i / 8D) * 10D);
        }
        return prices;
    }
}
