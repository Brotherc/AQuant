package com.brotherc.aquant.dividend.service;

import com.brotherc.aquant.dividend.entity.StockDividend;
import com.brotherc.aquant.dividend.model.vo.StockDividendStatVO;
import com.brotherc.aquant.dividend.repository.StockDividendRepository;
import com.brotherc.aquant.indicator.entity.StockDupontAnalysis;
import com.brotherc.aquant.indicator.repository.StockDupontAnalysisRepository;
import com.brotherc.aquant.indicator.repository.StockValuationMetricsRepository;
import com.brotherc.aquant.integration.akshare.service.AKShareDividendService;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.watchlist.repository.StockWatchlistStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDividendServiceScoreTest {

    @Mock
    private AKShareDividendService akShareDividendService;
    @Mock
    private StockDividendRepository stockDividendRepository;
    @Mock
    private StockQuoteRepository stockQuoteRepository;
    @Mock
    private StockValuationMetricsRepository stockValuationMetricsRepository;
    @Mock
    private StockDupontAnalysisRepository stockDupontAnalysisRepository;
    @Mock
    private StockWatchlistStockRepository stockWatchlistStockRepository;

    @InjectMocks
    private StockDividendService stockDividendService;

    @BeforeEach
    void setUpSupportingData() {
        StockQuote quote = new StockQuote();
        quote.setCode("sz000001");
        quote.setLatestPrice(new BigDecimal("10"));
        when(stockQuoteRepository.findAll()).thenReturn(List.of(quote));
        when(stockValuationMetricsRepository.findAll()).thenReturn(List.of());

        StockDupontAnalysis dupont = new StockDupontAnalysis();
        dupont.setStockCode("000001");
        dupont.setRoe3yAvg(new BigDecimal("15"));
        when(stockDupontAnalysisRepository.findAll()).thenReturn(List.of(dupont));
    }

    @Test
    @DisplayName("评分应使用最近完整年度并按三年复合增长率计算")
    void shouldUseLatestCompletedYearAndCalculateCagr() {
        int currentYear = LocalDate.now().getYear();
        List<StockDividend> dividends = new ArrayList<>();
        dividends.add(dividend(currentYear - 4, "4"));
        dividends.add(dividend(currentYear - 3, "4.4"));
        dividends.add(dividend(currentYear - 2, "4.8"));
        dividends.add(dividend(currentYear - 1, "5.324"));
        dividends.add(dividend(currentYear, "1"));
        StockDividend unsupportedQuarter = dividend(currentYear - 1, "100");
        unsupportedQuarter.setReportDate((currentYear - 1) + "0930");
        dividends.add(unsupportedQuarter);
        when(stockDividendRepository.findAll()).thenReturn(dividends);

        StockDividendStatVO result = stockDividendService
                .calcFullDividendStats(3, null, null, null, null).get(0);

        assertThat(result.getLatestYearDividend()).isEqualByComparingTo("5.324");
        assertThat(result.getDividendGrowth3y()).isEqualByComparingTo("10.0");
        assertThat(result.getPayoutRatio()).isEqualByComparingTo("53.2");
        assertThat(result.getDividendScore()).isNotNull();
    }

    @Test
    @DisplayName("缺少三年前分红时不应虚构增长率或增长分")
    void shouldNotInventGrowthWhenHistoryIsMissing() {
        int lastYear = LocalDate.now().getYear() - 1;
        when(stockDividendRepository.findAll()).thenReturn(List.of(dividend(lastYear, "5")));

        StockDividendStatVO result = stockDividendService
                .calcFullDividendStats(3, null, null, null, null).get(0);

        assertThat(result.getDividendGrowth3y()).isNull();
        assertThat(result.getDividendScore()).isEqualByComparingTo("59");
        assertThat(result.getDividendLevel()).isEqualTo("分红观察");
    }

    private StockDividend dividend(int year, String cashDividendRatio) {
        StockDividend dividend = new StockDividend();
        dividend.setStockCode("000001");
        dividend.setStockName("平安银行");
        dividend.setReportDate(year + "1231");
        dividend.setCashDividendRatio(new BigDecimal(cashDividendRatio));
        dividend.setEarningsPerShare(BigDecimal.ONE);
        dividend.setLatestAnnouncementDate(LocalDate.of(year + 1, 3, 1));
        dividend.setPlanStatus("实施分配");
        return dividend;
    }
}
