package com.brotherc.aquant.service.index;

import com.brotherc.aquant.model.dto.stockquote.StockQuoteSentimentDTO;
import com.brotherc.aquant.model.vo.sentiment.MarketSentimentVO;
import com.brotherc.aquant.repository.industry.StockIndustryBoardRepository;
import com.brotherc.aquant.repository.stock.StockQuoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMarketSentimentTest {

    @Mock
    private StockIndustryBoardRepository stockIndustryBoardRepository;

    @Mock
    private StockQuoteRepository stockQuoteRepository;

    @InjectMocks
    private StockMarketService stockMarketService;

    @Test
    @DisplayName("Should return default initialized VO when quotes is empty")
    void shouldReturnDefaultVoWhenQuotesEmpty() {
        when(stockQuoteRepository.findAllSentimentQuotes()).thenReturn(List.of());

        MarketSentimentVO vo = stockMarketService.getMarketSentiment();

        assertThat(vo).isNotNull();
        assertThat(vo.getTotalCount()).isEqualTo(0);
        assertThat(vo.getRiseCount()).isEqualTo(0);
        assertThat(vo.getFallCount()).isEqualTo(0);
        assertThat(vo.getFlatCount()).isEqualTo(0);
        assertThat(vo.getTotalTurnover()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(vo.getTurnoverChangeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should accurately accumulate market sentiment metrics from stock quotes")
    void shouldAccumulateMarketSentimentAccurately() {
        StockQuoteSentimentDTO q1 = createDTO("10.00", "100000000"); // 涨停 >= 9.8%
        StockQuoteSentimentDTO q2 = createDTO("8.50", "80000000");   // 8%~9.8%
        StockQuoteSentimentDTO q3 = createDTO("6.20", "50000000");   // 6%~8%
        StockQuoteSentimentDTO q4 = createDTO("4.50", "40000000");   // 4%~6%
        StockQuoteSentimentDTO q5 = createDTO("2.50", "30000000");   // 2%~4%
        StockQuoteSentimentDTO q6 = createDTO("1.20", "20000000");   // 1%~2%
        StockQuoteSentimentDTO q7 = createDTO("0.50", "10000000");   // 0%~1%
        StockQuoteSentimentDTO q8 = createDTO("0.00", "5000000");    // 平盘
        StockQuoteSentimentDTO q9 = createDTO("-0.80", "10000000");  // 0%~-1%
        StockQuoteSentimentDTO q10 = createDTO("-1.50", "20000000"); // -1%~-2%
        StockQuoteSentimentDTO q11 = createDTO("-3.00", "30000000"); // -2%~-4%
        StockQuoteSentimentDTO q12 = createDTO("-5.00", "40000000"); // -4%~-6%
        StockQuoteSentimentDTO q13 = createDTO("-7.00", "50000000"); // -6%~-8%
        StockQuoteSentimentDTO q14 = createDTO("-9.00", "60000000"); // -8%~-9.8%
        StockQuoteSentimentDTO q15 = createDTO("-10.00", "70000000"); // 跌停 <= -9.8%

        when(stockQuoteRepository.findAllSentimentQuotes()).thenReturn(List.of(
                q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, q11, q12, q13, q14, q15
        ));

        MarketSentimentVO vo = stockMarketService.getMarketSentiment();

        assertThat(vo.getTotalCount()).isEqualTo(15);
        assertThat(vo.getRiseCount()).isEqualTo(7);
        assertThat(vo.getFlatCount()).isEqualTo(1);
        assertThat(vo.getFallCount()).isEqualTo(7);

        assertThat(vo.getLimitUpCount()).isEqualTo(1);
        assertThat(vo.getUp8ToMaxCount()).isEqualTo(1);
        assertThat(vo.getUp6To8Count()).isEqualTo(1);
        assertThat(vo.getUp4To6Count()).isEqualTo(1);
        assertThat(vo.getUp2To4Count()).isEqualTo(1);
        assertThat(vo.getUp1To2Count()).isEqualTo(1);
        assertThat(vo.getUp0To1Count()).isEqualTo(1);

        assertThat(vo.getDown0To1Count()).isEqualTo(1);
        assertThat(vo.getDown1To2Count()).isEqualTo(1);
        assertThat(vo.getDown2To4Count()).isEqualTo(1);
        assertThat(vo.getDown4To6Count()).isEqualTo(1);
        assertThat(vo.getDown6To8Count()).isEqualTo(1);
        assertThat(vo.getDown8ToMinCount()).isEqualTo(1);
        assertThat(vo.getLimitDownCount()).isEqualTo(1);

        // 总成交额 100+80+50+40+30+20+10+5+10+20+30+40+50+60+70 = 615 百万 = 615000000
        assertThat(vo.getTotalTurnover()).isEqualByComparingTo(new BigDecimal("615000000"));
        assertThat(vo.getTurnoverChangeAmount()).isEqualByComparingTo(new BigDecimal("30750000.00"));
    }

    private StockQuoteSentimentDTO createDTO(String changePercent, String turnover) {
        return new StockQuoteSentimentDTO(
                changePercent != null ? new BigDecimal(changePercent) : null,
                turnover != null ? new BigDecimal(turnover) : null
        );
    }

}
