package com.brotherc.aquant.indicator.service;

import com.brotherc.aquant.indicator.entity.StockBalanceSheet;
import com.brotherc.aquant.indicator.entity.StockDupontAnalysis;
import com.brotherc.aquant.indicator.entity.StockPerformanceReport;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.indicator.repository.StockBalanceSheetRepository;
import com.brotherc.aquant.indicator.repository.StockDupontAnalysisRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDupontAnalysisCalculationTest {

    @Mock
    private StockDupontAnalysisRepository stockDupontAnalysisRepository;

    @Mock
    private StockQuoteRepository stockQuoteRepository;

    @Mock
    private StockPerformanceReportRepository stockPerformanceReportRepository;

    @Mock
    private StockBalanceSheetRepository stockBalanceSheetRepository;

    @InjectMocks
    private StockDupontAnalysisService stockDupontAnalysisService;

    @Test
    @DisplayName("Should correctly compute Dupont analysis decomposition, 3-year averages, industry stats and rank")
    void shouldCalculateDupontMetricsCorrectly() {
        // Stock 1: 600519 茅台 (白酒)
        StockQuote quote1 = createQuote("sh600519", "贵州茅台");
        // Stock 2: 000858 五粮液 (白酒)
        StockQuote quote2 = createQuote("sz000858", "五粮液");

        // 2025 年报 (LastYA)
        // 茅台: 净利润=700, 营收=1400 -> 净利率=50%; 2025总资产=2800, 2024期初总资产=2800 -> 平均总资产=2800 -> 周转率=0.5; 净资产=2000 -> 权益乘数=1.4; ROE=35%
        StockPerformanceReport pr1_2025 = createPR("600519", "贵州茅台", "2025-12-31", "白酒", "700", "1400", "35.0");
        StockBalanceSheet bs1_2025 = createBS("600519", "贵州茅台", "2025-12-31", "2800", "2000");

        // 2024 年报 (Last2yA)
        StockPerformanceReport pr1_2024 = createPR("600519", "贵州茅台", "2024-12-31", "白酒", "600", "1200", "30.0");
        StockBalanceSheet bs1_2024 = createBS("600519", "贵州茅台", "2024-12-31", "2800", "2000");

        // 2023 年报 (Last3yA)
        StockPerformanceReport pr1_2023 = createPR("600519", "贵州茅台", "2023-12-31", "白酒", "500", "1000", "25.0");
        StockBalanceSheet bs1_2023 = createBS("600519", "贵州茅台", "2023-12-31", "2000", "2000");
        StockBalanceSheet bs1_2022 = createBS("600519", "贵州茅台", "2022-12-31", "2000", "2000");

        // 五粮液 2025 年报: 净利润=300, 营收=900 -> 净利率=33.3333%; 2025总资产=1800, 2024期初总资产=1800 -> 平均总资产=1800 -> 周转率=0.5; 净资产=1200 -> 权益乘数=1.5; ROE=25%
        StockPerformanceReport pr2_2025 = createPR("000858", "五粮液", "2025-12-31", "白酒", "300", "900", "25.0");
        StockBalanceSheet bs2_2025 = createBS("000858", "五粮液", "2025-12-31", "1800", "1200");

        StockPerformanceReport pr2_2024 = createPR("000858", "五粮液", "2024-12-31", "白酒", "240", "800", "20.0");
        StockBalanceSheet bs2_2024 = createBS("000858", "五粮液", "2024-12-31", "1800", "1200");

        StockPerformanceReport pr2_2023 = createPR("000858", "五粮液", "2023-12-31", "白酒", "180", "600", "15.0");
        StockBalanceSheet bs2_2023 = createBS("000858", "五粮液", "2023-12-31", "1200", "1200");
        StockBalanceSheet bs2_2022 = createBS("000858", "五粮液", "2022-12-31", "1200", "1200");

        when(stockQuoteRepository.findAll()).thenReturn(List.of(quote1, quote2));
        when(stockPerformanceReportRepository.findAll()).thenReturn(List.of(pr1_2025, pr1_2024, pr1_2023, pr2_2025, pr2_2024, pr2_2023));
        when(stockBalanceSheetRepository.findAll()).thenReturn(List.of(
                bs1_2025, bs1_2024, bs1_2023, bs1_2022,
                bs2_2025, bs2_2024, bs2_2023, bs2_2022
        ));
        when(stockDupontAnalysisRepository.findAll()).thenReturn(List.of());

        stockDupontAnalysisService.refreshDupontAnalysis();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockDupontAnalysis>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockDupontAnalysisRepository).saveAll(captor.capture());
        List<StockDupontAnalysis> saved = captor.getValue();

        assertThat(saved).hasSize(2);

        StockDupontAnalysis moutai = saved.stream()
                .filter(s -> "sh600519".equals(s.getStockCode()))
                .findFirst().orElseThrow();

        // 茅台 2025 指标验证
        assertThat(moutai.getIndustry()).isEqualTo("白酒");
        assertThat(moutai.getNetMarginLastYA()).isEqualByComparingTo("50.0000");
        assertThat(moutai.getAssetTurnoverLastYA()).isEqualByComparingTo("0.5000");
        assertThat(moutai.getEquityMultiplierLastYA()).isEqualByComparingTo("1.4000");
        assertThat(moutai.getRoeLastYA()).isEqualByComparingTo("35.0000");

        // 茅台 3年平均指标 (ROE: (35+30+25)/3 = 30)
        assertThat(moutai.getRoe3yAvg()).isEqualByComparingTo("30.0000");
        assertThat(moutai.getNetMargin3yAvg()).isEqualByComparingTo("50.0000");

        // 茅台 排名 (白酒行业第 1 名)
        assertThat(moutai.getRoe3yAvgRank()).isEqualByComparingTo("1");

        // 白酒行业平均与中值 (ROE 3年平均: 茅台30, 五粮液20 -> 均值25, 中值25)
        assertThat(moutai.getRoe3yAvgIndustryAvg()).isEqualByComparingTo("25.0000");
        assertThat(moutai.getRoe3yAvgIndustryMed()).isEqualByComparingTo("25.0000");
        assertThat(moutai.getQualityScore()).isGreaterThanOrEqualTo(new BigDecimal("80"));
        assertThat(moutai.getQualityLevel()).isEqualTo("优秀");

        StockDupontAnalysis wuliangye = saved.stream()
                .filter(s -> "sz000858".equals(s.getStockCode()))
                .findFirst().orElseThrow();

        assertThat(wuliangye.getRoe3yAvg()).isEqualByComparingTo("20.0000");
        assertThat(wuliangye.getRoe3yAvgRank()).isEqualByComparingTo("2");
    }

    @Test
    @DisplayName("Should handle edge cases gracefully: zero revenue/equity, missing report, negative equity")
    void shouldHandleEdgeCasesWithZeroOrMissingData() {
        StockQuote quote = createQuote("sh600000", "浦发银行");

        // 营收=0, 净资产=0
        StockPerformanceReport pr_2025 = createPR("600000", "浦发银行", "2025-12-31", "银行", "100", "0", null);
        StockBalanceSheet bs_2025 = createBS("600000", "浦发银行", "2025-12-31", "1000", "0");

        when(stockQuoteRepository.findAll()).thenReturn(List.of(quote));
        when(stockPerformanceReportRepository.findAll()).thenReturn(List.of(pr_2025));
        when(stockBalanceSheetRepository.findAll()).thenReturn(List.of(bs_2025));
        when(stockDupontAnalysisRepository.findAll()).thenReturn(List.of());

        stockDupontAnalysisService.refreshDupontAnalysis();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockDupontAnalysis>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockDupontAnalysisRepository).saveAll(captor.capture());
        List<StockDupontAnalysis> saved = captor.getValue();

        assertThat(saved).hasSize(1);
        StockDupontAnalysis item = saved.get(0);

        // 营收为0，净利率与周转率应安全返回 null 而非报错除零异常
        assertThat(item.getNetMarginLastYA()).isNull();
        assertThat(item.getAssetTurnoverLastYA()).isNull();
        // 净资产为0，权益乘数与 ROE 应安全返回 null
        assertThat(item.getEquityMultiplierLastYA()).isNull();
        assertThat(item.getRoeLastYA()).isNull();
        assertThat(item.getRoe3yAvg()).isNull();
        assertThat(item.getQualityScore()).isNull();
        assertThat(item.getQualityLevel()).isEqualTo("数据不足");
        assertThat(item.getConclusion()).contains("暂不进行质量评分");
    }

    @Test
    @DisplayName("Should ignore a sparsely disclosed newer annual period")
    void shouldIgnoreSparseLatestAnnualPeriod() {
        List<StockQuote> quotes = new ArrayList<>();
        List<StockPerformanceReport> reports = new ArrayList<>();
        List<StockBalanceSheet> balanceSheets = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String code = String.format("00000%d", i);
            quotes.add(createQuote("sz" + code, "测试公司" + i));
            reports.add(createPR(code, "测试公司" + i, "2025-12-31", "测试行业", "10", "100", "12"));
            reports.add(createPR(code, "测试公司" + i, "2024-12-31", "测试行业", "9", "100", "11"));
            reports.add(createPR(code, "测试公司" + i, "2023-12-31", "测试行业", "8", "100", "10"));
            balanceSheets.add(createBS(code, "测试公司" + i, "2025-12-31", "100", "60"));
            balanceSheets.add(createBS(code, "测试公司" + i, "2024-12-31", "100", "60"));
            balanceSheets.add(createBS(code, "测试公司" + i, "2023-12-31", "100", "60"));
            balanceSheets.add(createBS(code, "测试公司" + i, "2022-12-31", "100", "60"));
        }
        reports.add(createPR("000001", "测试公司1", "2026-12-31", "测试行业", "90", "100", "90"));
        balanceSheets.add(createBS("000001", "测试公司1", "2026-12-31", "100", "60"));
        when(stockQuoteRepository.findAll()).thenReturn(quotes);
        when(stockPerformanceReportRepository.findAll()).thenReturn(reports);
        when(stockBalanceSheetRepository.findAll()).thenReturn(balanceSheets);

        List<StockDupontAnalysis> result = stockDupontAnalysisService.calculateDupontAnalysis();

        assertThat(result).hasSize(5).allSatisfy(item ->
                assertThat(item.getRoeLastYA()).isEqualByComparingTo("12")
        );
    }

    @Test
    @DisplayName("Should cap extreme non-financial leverage at poor quality")
    void shouldCapExtremeNonFinancialLeverage() {
        StockDupontAnalysis item = createCompleteScoreItem("制造业", "25", "20", "1.5", "5.5");

        stockDupontAnalysisService.calculateScoreAndConclusion(item);

        assertThat(item.getQualityScore()).isLessThanOrEqualTo(new BigDecimal("49"));
        assertThat(item.getQualityLevel()).isEqualTo("较差");
    }

    @Test
    @DisplayName("Should evaluate financial leverage relative to its industry")
    void shouldUseIndustryLeverageForFinancialCompanies() {
        StockDupontAnalysis item = createCompleteScoreItem("银行", "20", "15", "0.05", "10");
        item.setEquityMultiplier3yAvgIndustryMed(new BigDecimal("10"));
        item.setNetMargin3yAvgIndustryMed(new BigDecimal("12"));
        item.setAssetTurnover3yAvgIndustryMed(new BigDecimal("0.05"));

        stockDupontAnalysisService.calculateScoreAndConclusion(item);

        assertThat(item.getQualityScore()).isGreaterThanOrEqualTo(new BigDecimal("65"));
        assertThat(item.getQualityLevel()).isIn("良好", "优秀");
    }

    private StockDupontAnalysis createCompleteScoreItem(
            String industry, String roe, String netMargin, String turnover, String equityMultiplier
    ) {
        StockDupontAnalysis item = new StockDupontAnalysis();
        item.setIndustry(industry);
        item.setRoe3yAvg(new BigDecimal(roe));
        item.setRoeLastYA(new BigDecimal(roe));
        item.setRoeLast2yA(new BigDecimal(roe));
        item.setRoeLast3yA(new BigDecimal(roe));
        item.setNetMargin3yAvg(new BigDecimal(netMargin));
        item.setNetMarginLastYA(new BigDecimal(netMargin));
        item.setNetMarginLast2yA(new BigDecimal(netMargin));
        item.setNetMarginLast3yA(new BigDecimal(netMargin));
        item.setAssetTurnover3yAvg(new BigDecimal(turnover));
        item.setAssetTurnoverLastYA(new BigDecimal(turnover));
        item.setAssetTurnoverLast2yA(new BigDecimal(turnover));
        item.setAssetTurnoverLast3yA(new BigDecimal(turnover));
        item.setEquityMultiplier3yAvg(new BigDecimal(equityMultiplier));
        item.setEquityMultiplierLastYA(new BigDecimal(equityMultiplier));
        item.setEquityMultiplierLast2yA(new BigDecimal(equityMultiplier));
        item.setEquityMultiplierLast3yA(new BigDecimal(equityMultiplier));
        return item;
    }

    private StockQuote createQuote(String code, String name) {
        StockQuote quote = new StockQuote();
        quote.setCode(code);
        quote.setName(name);
        return quote;
    }

    private StockPerformanceReport createPR(String code, String name, String date, String industry, String netProfit, String revenue, String roe) {
        StockPerformanceReport pr = new StockPerformanceReport();
        pr.setStockCode(code);
        pr.setStockName(name);
        pr.setReportDate(LocalDate.parse(date));
        pr.setIndustry(industry);
        pr.setNetProfit(netProfit != null ? new BigDecimal(netProfit) : null);
        pr.setTotalRevenue(revenue != null ? new BigDecimal(revenue) : null);
        pr.setRoe(roe != null ? new BigDecimal(roe) : null);
        return pr;
    }

    private StockBalanceSheet createBS(String code, String name, String date, String totalAssets, String totalEquity) {
        StockBalanceSheet bs = new StockBalanceSheet();
        bs.setStockCode(code);
        bs.setStockName(name);
        bs.setReportDate(LocalDate.parse(date));
        bs.setTotalAssets(totalAssets != null ? new BigDecimal(totalAssets) : null);
        bs.setTotalEquity(totalEquity != null ? new BigDecimal(totalEquity) : null);
        return bs;
    }

}
