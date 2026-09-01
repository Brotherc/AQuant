package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.industry.entity.StockBoardConstituent;
import com.brotherc.aquant.industry.model.vo.StockIndustryConstituentSnapshotVO;
import com.brotherc.aquant.industry.repository.StockBoardConstituentRepository;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.stock.model.dto.StockQuoteHistoryProjection;
import com.brotherc.aquant.stock.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBoardConstituentServiceTest {

    private static final String INDUSTRY = "银行";

    @Mock
    private StockBoardConstituentRepository memberRepository;

    @Mock
    private StockQuoteRepository quoteRepository;

    @Mock
    private StockQuoteHistoryRepository historyRepository;

    @Mock
    private StockSyncRepository stockSyncRepository;

    @Mock
    private StockHelper stockHelper;

    @InjectMocks
    private StockBoardConstituentService service;

    @Test
    void usesStockQuoteForCurrentMetricsAndHistoryOnlyForTrend() {
        when(memberRepository.findByBoardCodeOrderByStockCodeAsc(INDUSTRY))
                .thenReturn(List.of(member("600000", "浦发银行")));
        setCurrentWatermark(100L);
        when(stockHelper.getLatestClosedTradeDaySyncWatermark(org.mockito.ArgumentMatchers.any())).thenReturn(100L);
        StockQuote quote = new StockQuote();
        quote.setCode("sh600000");
        quote.setLatestPrice(new BigDecimal("11.20"));
        quote.setChangeAmount(new BigDecimal("0.30"));
        quote.setChangePercent(new BigDecimal("2.75"));
        when(quoteRepository.findByCodeIn(List.of("sh600000"))).thenReturn(List.of(quote));
        when(historyRepository.findRecentTradeDates(10)).thenReturn(List.of("2026-08-28", "2026-08-29"));
        when(historyRepository.findByTradeDateInAndCodeInOrderByTradeDateAsc(anyList(), anyList()))
                .thenReturn(List.of(history("sh600000", "2026-08-28", "10.90"), history("sh600000", "2026-08-29", "11.20")));

        StockIndustryConstituentSnapshotVO snapshot = service.getSnapshot(INDUSTRY, null);

        assertThat(snapshot.isAvailable()).isTrue();
        assertThat(snapshot.isStale()).isFalse();
        assertThat(snapshot.getContent()).singleElement().satisfies(item -> {
            assertThat(item.getLatestPrice()).isEqualByComparingTo("11.20");
            assertThat(item.getChangeAmount()).isEqualByComparingTo("0.30");
            assertThat(item.getHistoryPrices()).containsExactly(new BigDecimal("10.90"), new BigDecimal("11.20"));
        });
        verify(quoteRepository).findByCodeIn(List.of("sh600000"));
    }

    @Test
    void returnsMissingMetricsWhenTheSelectedDateHasNoExactQuote() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 29);
        when(memberRepository.findByBoardCodeOrderByStockCodeAsc(INDUSTRY))
                .thenReturn(List.of(member("600000", "浦发银行")));
        setCurrentWatermark(100L);
        when(stockHelper.getLatestClosedTradeDaySyncWatermark(org.mockito.ArgumentMatchers.any())).thenReturn(100L);
        when(historyRepository.findRecentTradeDatesBefore(tradeDate.toString(), 10))
                .thenReturn(List.of("2026-08-28", "2026-08-27"));
        when(historyRepository.findByTradeDateInAndCodeInOrderByTradeDateAsc(anyList(), anyList()))
                .thenReturn(List.of(history("sh600000", "2026-08-28", "10.00")));

        StockIndustryConstituentSnapshotVO snapshot = service.getSnapshot(INDUSTRY, tradeDate);

        assertThat(snapshot.getContent()).singleElement().satisfies(item -> {
            assertThat(item.getLatestPrice()).isNull();
            assertThat(item.getChangeAmount()).isNull();
            assertThat(item.getChangePercent()).isNull();
        });
    }

    private void setCurrentWatermark(long value) {
        StockSync sync = new StockSync();
        sync.setName(StockSyncConstant.STOCK_BOARD_CONSTITUENT_LATEST_PREFIX + INDUSTRY);
        sync.setValue(String.valueOf(value));
        when(stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_CONSTITUENT_LATEST_PREFIX + INDUSTRY))
                .thenReturn(sync);
    }

    private StockBoardConstituent member(String code, String name) {
        StockBoardConstituent member = new StockBoardConstituent();
        member.setBoardCode(INDUSTRY);
        member.setStockCode(code);
        member.setStockName(name);
        member.setSourceUpdatedAt(java.time.LocalDateTime.now());
        return member;
    }

    private StockQuoteHistoryProjection history(String code, String date, String closePrice) {
        return new StockQuoteHistoryProjection() {
            @Override
            public String getCode() {
                return code;
            }

            @Override
            public String getTradeDate() {
                return date;
            }

            @Override
            public BigDecimal getClosePrice() {
                return new BigDecimal(closePrice);
            }
        };
    }
}
