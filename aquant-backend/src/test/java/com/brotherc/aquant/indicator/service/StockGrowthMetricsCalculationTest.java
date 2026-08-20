package com.brotherc.aquant.indicator.service;

import com.brotherc.aquant.indicator.entity.StockGrowthMetrics;
import com.brotherc.aquant.indicator.entity.StockPerformanceReport;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.indicator.repository.StockGrowthMetricsRepository;
import com.brotherc.aquant.indicator.repository.StockPerformanceReportRepository;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockGrowthMetricsCalculationTest {

    @Mock
    private StockGrowthMetricsRepository stockGrowthMetricsRepository;

    @Mock
    private StockQuoteRepository stockQuoteRepository;

    @Mock
    private StockPerformanceReportRepository stockPerformanceReportRepository;

    @InjectMocks
    private StockGrowthMetricsService stockGrowthMetricsService;

    @Test
    @DisplayName("Should correctly calculate growth metrics: LastYA, TTM, 3y CAGR, industry stats, and rank")
    void shouldCalculateGrowthMetricsCorrectly() {
        // Stock 1: 600519 茅台 (白酒)
        StockQuote quote1 = createQuote("sh600519", "贵州茅台");
        // Stock 2: 000858 五粮液 (白酒)
        StockQuote quote2 = createQuote("sz000858", "五粮液");

        // 茅台 年报数据:
        // 2025: 营收=1400, 净利=700, EPS=5.0
        // 2024: 营收=1200, 净利=600, EPS=4.0 -> LastYA: 营收增 (1400-1200)/1200 = 16.6667%, 净利增 (700-600)/600 = 16.6667%, EPS增 (5-4)/4 = 25%
        // 2023: 营收=1000, 净利=500, EPS=3.5
        // 2022: 营收=700, 净利=350, EPS=2.5 -> 3y CAGR: 营收 (1400/700)^(1/3)-1 = 25.9921%, 净利 (700/350)^(1/3)-1 = 25.9921%, EPS (5.0/2.5)^(1/3)-1 = 25.9921%
        StockPerformanceReport pr1_2025 = createPR("600519", "贵州茅台", "2025-12-31", "白酒", "700", "1400", "5.0");
        StockPerformanceReport pr1_2024 = createPR("600519", "贵州茅台", "2024-12-31", "白酒", "600", "1200", "4.0");
        StockPerformanceReport pr1_2023 = createPR("600519", "贵州茅台", "2023-12-31", "白酒", "500", "1000", "3.5");
        StockPerformanceReport pr1_2022 = createPR("600519", "贵州茅台", "2022-12-31", "白酒", "350", "700", "2.5");

        // 茅台 季报数据用于 TTM 计算 (2026-03-31 最新季报):
        // 2026-03-31: 营收=400, 净利=200, EPS=1.5
        // 2025-03-31: 营收=350, 净利=175, EPS=1.2
        // 2024-03-31: 营收=300, 净利=150, EPS=1.0
        // TTM (2026-03-31) = 2026Q1(400) + 2025年报(1400) - 2025Q1(350) = 1450 (净利: 200+700-175 = 725, EPS: 1.5+5.0-1.2 = 5.3)
        // Prev TTM (2025-03-31) = 2025Q1(350) + 2024年报(1200) - 2024Q1(300) = 1250 (净利: 175+600-150 = 625, EPS: 1.2+4.0-1.0 = 4.2)
        // TTM 增长率: 营收 (1450-1250)/1250 = 16.0%, 净利 (725-625)/625 = 16.0%, EPS (5.3-4.2)/4.2 = 26.1905%
        StockPerformanceReport pr1_2026Q1 = createPR("600519", "贵州茅台", "2026-03-31", "白酒", "200", "400", "1.5");
        StockPerformanceReport pr1_2025Q1 = createPR("600519", "贵州茅台", "2025-03-31", "白酒", "175", "350", "1.2");
        StockPerformanceReport pr1_2024Q1 = createPR("600519", "贵州茅台", "2024-03-31", "白酒", "150", "300", "1.0");

        // 五粮液 年报数据:
        // 2025: 营收=900, 净利=300, EPS=3.0
        // 2024: 营收=800, 净利=240, EPS=2.4
        // 2023: 营收=600, 净利=180, EPS=1.8
        // 2022: 营收=450, 净利=150, EPS=1.5 -> 3y CAGR: EPS (3.0/1.5)^(1/3)-1 = 25.9921%
        StockPerformanceReport pr2_2025 = createPR("000858", "五粮液", "2025-12-31", "白酒", "300", "900", "3.0");
        StockPerformanceReport pr2_2024 = createPR("000858", "五粮液", "2024-12-31", "白酒", "240", "800", "2.4");
        StockPerformanceReport pr2_2023 = createPR("000858", "五粮液", "2023-12-31", "白酒", "180", "600", "1.8");
        StockPerformanceReport pr2_2022 = createPR("000858", "五粮液", "2022-12-31", "白酒", "150", "450", "1.5");

        when(stockQuoteRepository.findAll()).thenReturn(List.of(quote1, quote2));
        when(stockPerformanceReportRepository.findAll()).thenReturn(List.of(
                pr1_2026Q1, pr1_2025, pr1_2025Q1, pr1_2024, pr1_2024Q1, pr1_2023, pr1_2022,
                pr2_2025, pr2_2024, pr2_2023, pr2_2022
        ));
        when(stockGrowthMetricsRepository.findAll()).thenReturn(List.of());

        stockGrowthMetricsService.refreshGrowthMetrics();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockGrowthMetrics>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockGrowthMetricsRepository).saveAll(captor.capture());
        List<StockGrowthMetrics> saved = captor.getValue();

        assertThat(saved).hasSize(2);

        StockGrowthMetrics moutai = saved.stream()
                .filter(s -> "sh600519".equals(s.getStockCode()))
                .findFirst().orElseThrow();

        // 茅台 去年实际增长率
        assertThat(moutai.getRevenueGrowthLastYA()).isEqualByComparingTo("16.6667");
        assertThat(moutai.getNetProfitGrowthLastYA()).isEqualByComparingTo("16.6667");
        assertThat(moutai.getEpsGrowthLastYA()).isEqualByComparingTo("25.0000");

        // 茅台 TTM 增长率
        assertThat(moutai.getRevenueGrowthTtm()).isEqualByComparingTo("16.0000");
        assertThat(moutai.getNetProfitGrowthTtm()).isEqualByComparingTo("16.0000");
        assertThat(moutai.getEpsGrowthTtm()).isEqualByComparingTo("26.1905");

        // 茅台 3年复合增长率 (CAGR)
        assertThat(moutai.getEpsGrowth3yCagr()).isEqualByComparingTo("25.9921");
        assertThat(moutai.getRevenueGrowth3yCagr()).isEqualByComparingTo("25.9921");
        assertThat(moutai.getNetProfitGrowth3yCagr()).isEqualByComparingTo("25.9921");

        // 行业统计
        assertThat(moutai.getEpsGrowthLastYAIndustryAvg()).isNotNull();
        assertThat(moutai.getEpsGrowthLastYAIndustryMed()).isNotNull();
        assertThat(moutai.getEpsGrowth3yCagrRank()).isNotNull();
    }

    @Test
    @DisplayName("Should handle loss (negative values) and zero divisors gracefully without errors")
    void shouldHandleEdgeCasesGracefully() {
        StockQuote quote = createQuote("sh600000", "浦发银行");

        // 基期 2022 年净亏损 -100，2025 年净利 100 -> 3年 CAGR 无法开方，应返回 null
        StockPerformanceReport pr_2025 = createPR("600000", "浦发银行", "2025-12-31", "银行", "100", "500", "0.5");
        StockPerformanceReport pr_2024 = createPR("600000", "浦发银行", "2024-12-31", "银行", "0", "0", "0.0");
        StockPerformanceReport pr_2022 = createPR("600000", "浦发银行", "2022-12-31", "银行", "-100", "400", "-0.5");

        when(stockQuoteRepository.findAll()).thenReturn(List.of(quote));
        when(stockPerformanceReportRepository.findAll()).thenReturn(List.of(pr_2025, pr_2024, pr_2022));
        when(stockGrowthMetricsRepository.findAll()).thenReturn(List.of());

        stockGrowthMetricsService.refreshGrowthMetrics();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockGrowthMetrics>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockGrowthMetricsRepository).saveAll(captor.capture());
        List<StockGrowthMetrics> saved = captor.getValue();

        assertThat(saved).hasSize(1);
        StockGrowthMetrics bank = saved.get(0);

        // 2024 营收为 0，去年实际营收增长率分母为 0，安全返回 null
        assertThat(bank.getRevenueGrowthLastYA()).isNull();
        // 基期为负数，3年 CAGR 应安全返回 null
        assertThat(bank.getNetProfitGrowth3yCagr()).isNull();
        assertThat(bank.getEpsGrowth3yCagr()).isNull();
    }

    private StockQuote createQuote(String code, String name) {
        StockQuote quote = new StockQuote();
        quote.setCode(code);
        quote.setName(name);
        return quote;
    }

    private StockPerformanceReport createPR(String code, String name, String date, String industry, String netProfit, String revenue, String eps) {
        StockPerformanceReport pr = new StockPerformanceReport();
        pr.setStockCode(code);
        pr.setStockName(name);
        pr.setReportDate(LocalDate.parse(date));
        pr.setIndustry(industry);
        pr.setNetProfit(netProfit != null ? new BigDecimal(netProfit) : null);
        pr.setTotalRevenue(revenue != null ? new BigDecimal(revenue) : null);
        pr.setEarningsPerShare(eps != null ? new BigDecimal(eps) : null);
        return pr;
    }

}
