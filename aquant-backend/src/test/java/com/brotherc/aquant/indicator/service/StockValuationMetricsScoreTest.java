package com.brotherc.aquant.indicator.service;

import com.brotherc.aquant.indicator.entity.StockValuationMetrics;
import com.brotherc.aquant.indicator.repository.StockPerformanceReportRepository;
import com.brotherc.aquant.indicator.repository.StockValuationMetricsRepository;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.stock.repository.StockShareChangeRepository;
import com.brotherc.aquant.watchlist.repository.StockWatchlistGroupRepository;
import com.brotherc.aquant.watchlist.repository.StockWatchlistStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StockValuationMetricsScoreTest {

    @Mock
    private StockValuationMetricsRepository stockValuationMetricsRepository;
    @Mock
    private StockQuoteRepository stockQuoteRepository;
    @Mock
    private StockPerformanceReportRepository stockPerformanceReportRepository;
    @Mock
    private StockShareChangeRepository stockShareChangeRepository;
    @Mock
    private StockWatchlistGroupRepository watchlistGroupRepository;
    @Mock
    private StockWatchlistStockRepository watchlistStockRepository;

    @InjectMocks
    private StockValuationMetricsService stockValuationMetricsService;

    @Test
    @DisplayName("盈利数据缺失时不应给出默认估值分")
    void shouldNotScoreWhenProfitIsMissing() {
        StockValuationMetrics metrics = completeMetrics();
        metrics.setNetProfitTtm(null);

        stockValuationMetricsService.calculateScoreAndConclusion(metrics);

        assertThat(metrics.getValuationScore()).isNull();
        assertThat(metrics.getValuationLevel()).isEqualTo("数据不足");
    }

    @Test
    @DisplayName("亏损企业不应通过PB或PS获得低估评分")
    void shouldNotScoreLossMakingCompany() {
        StockValuationMetrics metrics = completeMetrics();
        metrics.setNetProfitTtm(new BigDecimal("-1"));

        stockValuationMetricsService.calculateScoreAndConclusion(metrics);

        assertThat(metrics.getValuationScore()).isNull();
        assertThat(metrics.getValuationLevel()).isEqualTo("不适用");
    }

    @Test
    @DisplayName("指标处于行业中位附近时应评为合理")
    void shouldRateIndustryMedianAsReasonable() {
        StockValuationMetrics metrics = completeMetrics();

        stockValuationMetricsService.calculateScoreAndConclusion(metrics);

        assertThat(metrics.getValuationScore()).isEqualByComparingTo("59");
        assertThat(metrics.getValuationLevel()).isEqualTo("合理");
    }

    @Test
    @DisplayName("多项指标明显低于行业中位时应评为低估")
    void shouldRateBroadDiscountAsUndervalued() {
        StockValuationMetrics metrics = completeMetrics();
        metrics.setPeTtm(new BigDecimal("12"));
        metrics.setPbMrq(new BigDecimal("1.2"));
        metrics.setPsTtm(new BigDecimal("1.8"));
        metrics.setPcfTtm(new BigDecimal("9"));
        metrics.setPeg(new BigDecimal("0.8"));

        stockValuationMetricsService.calculateScoreAndConclusion(metrics);

        assertThat(metrics.getValuationScore()).isEqualByComparingTo("92");
        assertThat(metrics.getValuationLevel()).isEqualTo("低估");
    }

    @Test
    @DisplayName("仅有两项相对指标时最高只能评为合理")
    void shouldCapScoreWhenOnlyTwoRelativeMetricsAreAvailable() {
        StockValuationMetrics metrics = completeMetrics();
        metrics.setPeTtm(new BigDecimal("10"));
        metrics.setPbMrq(new BigDecimal("1"));
        metrics.setPsTtm(null);
        metrics.setPcfTtm(null);
        metrics.setPeg(new BigDecimal("0.5"));

        stockValuationMetricsService.calculateScoreAndConclusion(metrics);

        assertThat(metrics.getValuationScore()).isEqualByComparingTo("64");
        assertThat(metrics.getValuationLevel()).isEqualTo("合理");
    }

    @Test
    @DisplayName("综合估值快捷分类应按45分和65分完整划分")
    void shouldClassifyByComprehensiveValuationScore() {
        StockValuationMetrics metrics = completeMetrics();

        metrics.setValuationScore(new BigDecimal("65"));
        assertThat(stockValuationMetricsService.isLowValuation(metrics)).isTrue();
        assertThat(stockValuationMetricsService.isFairValuation(metrics)).isFalse();

        metrics.setValuationScore(new BigDecimal("64"));
        assertThat(stockValuationMetricsService.isFairValuation(metrics)).isTrue();

        metrics.setValuationScore(new BigDecimal("45"));
        assertThat(stockValuationMetricsService.isFairValuation(metrics)).isTrue();

        metrics.setValuationScore(new BigDecimal("44"));
        assertThat(stockValuationMetricsService.isHighValuation(metrics)).isTrue();
        assertThat(stockValuationMetricsService.isFairValuation(metrics)).isFalse();
    }

    @Test
    @DisplayName("亏损或评分缺失公司不应进入估值快捷分类")
    void shouldExcludeLossAndMissingScoreFromValuationCategories() {
        StockValuationMetrics metrics = completeMetrics();
        metrics.setValuationScore(null);

        assertThat(stockValuationMetricsService.isLowValuation(metrics)).isFalse();
        assertThat(stockValuationMetricsService.isFairValuation(metrics)).isFalse();
        assertThat(stockValuationMetricsService.isHighValuation(metrics)).isFalse();

        metrics.setValuationScore(new BigDecimal("80"));
        metrics.setNetProfitTtm(new BigDecimal("-1"));

        assertThat(stockValuationMetricsService.isLowValuation(metrics)).isFalse();
        assertThat(stockValuationMetricsService.isFairValuation(metrics)).isFalse();
        assertThat(stockValuationMetricsService.isHighValuation(metrics)).isFalse();
    }

    private StockValuationMetrics completeMetrics() {
        StockValuationMetrics metrics = new StockValuationMetrics();
        metrics.setIndustry("软件开发");
        metrics.setNetProfitTtm(new BigDecimal("100000000"));
        metrics.setPeTtm(new BigDecimal("20"));
        metrics.setPeTtmIndustryMed(new BigDecimal("20"));
        metrics.setPbMrq(new BigDecimal("2"));
        metrics.setPbMrqIndustryMed(new BigDecimal("2"));
        metrics.setPsTtm(new BigDecimal("3"));
        metrics.setPsTtmIndustryMed(new BigDecimal("3"));
        metrics.setPcfTtm(new BigDecimal("15"));
        metrics.setPcfTtmIndustryMed(new BigDecimal("15"));
        metrics.setPeg(new BigDecimal("1"));
        return metrics;
    }
}
