package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.industry.entity.StockBoardConstituentQuote;
import com.brotherc.aquant.industry.model.vo.StockIndustryConstituentSnapshotVO;
import com.brotherc.aquant.industry.repository.StockBoardConstituentQuoteRepository;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryConsThs;
import com.brotherc.aquant.integration.akshare.service.AKShareIndustryService;
import com.brotherc.aquant.stock.model.dto.StockQuoteHistoryProjection;
import com.brotherc.aquant.stock.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBoardConstituentQuoteServiceTest {

    private static final String INDUSTRY = "银行";

    @Mock
    private StockBoardConstituentQuoteRepository quoteRepository;

    @Mock
    private StockQuoteHistoryRepository historyRepository;

    @Mock
    private StockSyncRepository stockSyncRepository;

    @Mock
    private AKShareIndustryService industryService;

    @Mock
    private StockBoardConstituentQuoteSyncService quoteSyncService;

    @InjectMocks
    private StockBoardConstituentQuoteService service;

    @Test
    void usesFreshCacheWithoutRequestingTheUpstream() {
        LocalDateTime now = LocalDateTime.now();
        StockBoardConstituentQuote cached = quote("600000", "浦发银行", now);
        setSyncWatermark(now.minusMinutes(1));
        when(quoteRepository.findByBoardCodeOrderByChangePercentDesc(INDUSTRY)).thenReturn(List.of(cached));
        emptyHistory();

        StockIndustryConstituentSnapshotVO snapshot = service.getSnapshot(INDUSTRY, null);

        assertThat(snapshot.isAvailable()).isTrue();
        assertThat(snapshot.isStale()).isFalse();
        assertThat(snapshot.getContent()).extracting(item -> item.getCode()).containsExactly("600000");
        verify(industryService, never()).stockBoardIndustryConstituentsThs(any());
    }

    @Test
    void requestsAnUpstreamRefreshWhenTheCacheIsStale() {
        LocalDateTime now = LocalDateTime.now();
        StockBoardConstituentQuote oldMember = quote("600000", "旧成员", now.minusMinutes(2));
        StockBoardConstituentQuote refreshedMember = quote("000001", "平安银行", now.plusMinutes(1));
        setSyncWatermark(now);
        when(quoteRepository.findByBoardCodeOrderByChangePercentDesc(INDUSTRY))
                .thenReturn(List.of(oldMember), List.of(oldMember), List.of(refreshedMember));
        when(industryService.stockBoardIndustryConstituentsThs(INDUSTRY))
                .thenReturn(List.of(source("000001", "平安银行")));
        emptyHistory();

        StockIndustryConstituentSnapshotVO snapshot = service.getSnapshot(INDUSTRY, null);

        assertThat(snapshot.isAvailable()).isTrue();
        assertThat(snapshot.isStale()).isFalse();
        assertThat(snapshot.getContent()).extracting(item -> item.getCode()).containsExactly("000001");
        verify(quoteSyncService).sync(eq(INDUSTRY), anyList());
    }

    @Test
    void keepsStaleCacheWhenUpstreamRequestFails() {
        LocalDateTime now = LocalDateTime.now();
        StockBoardConstituentQuote cached = quote("600000", "浦发银行", now.minusMinutes(2));
        setSyncWatermark(now);
        when(quoteRepository.findByBoardCodeOrderByChangePercentDesc(INDUSTRY)).thenReturn(List.of(cached), List.of(cached));
        when(industryService.stockBoardIndustryConstituentsThs(INDUSTRY)).thenThrow(new IllegalStateException("upstream failed"));
        emptyHistory();

        StockIndustryConstituentSnapshotVO snapshot = service.getSnapshot(INDUSTRY, null);

        assertThat(snapshot.isAvailable()).isTrue();
        assertThat(snapshot.isStale()).isTrue();
        assertThat(snapshot.getMessage()).contains("缓存数据");
        assertThat(snapshot.getContent()).extracting(item -> item.getCode()).containsExactly("600000");
        verify(quoteRepository, never()).deleteByBoardCodeAndStockCodeNotIn(any(), anyList());
    }

    @Test
    void clearsSelectedDateMetricsWhenThatStockHasNoExactDailyQuote() {
        LocalDateTime now = LocalDateTime.now();
        StockBoardConstituentQuote cached = quote("600000", "浦发银行", now);
        LocalDate tradeDate = LocalDate.of(2026, 8, 28);
        setSyncWatermark(now.minusMinutes(1));
        when(quoteRepository.findByBoardCodeOrderByChangePercentDesc(INDUSTRY)).thenReturn(List.of(cached));
        when(historyRepository.findRecentTradeDatesBefore(tradeDate.toString(), 10))
                .thenReturn(List.of("2026-08-28", "2026-08-27"));
        when(historyRepository.findByTradeDateInAndCodeInOrderByTradeDateAsc(anyList(), anyList()))
                .thenReturn(List.of(history("sh600000", "2026-08-27", "10.00")));

        StockIndustryConstituentSnapshotVO snapshot = service.getSnapshot(INDUSTRY, tradeDate);

        assertThat(snapshot.getContent()).hasSize(1);
        assertEquals(List.of(new BigDecimal("10.00")), snapshot.getContent().get(0).getHistoryPrices());
        assertThat(snapshot.getContent().get(0).getLatestPrice()).isNull();
        assertThat(snapshot.getContent().get(0).getChangeAmount()).isNull();
        assertThat(snapshot.getContent().get(0).getChangePercent()).isNull();
    }

    private void setSyncWatermark(LocalDateTime time) {
        StockSync sync = new StockSync();
        sync.setName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST);
        sync.setValue(String.valueOf(time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()));
        when(stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST)).thenReturn(sync);
    }

    private void emptyHistory() {
        when(historyRepository.findRecentTradeDates(10)).thenReturn(List.of());
    }

    private StockBoardConstituentQuote quote(String code, String name, LocalDateTime createdAt) {
        StockBoardConstituentQuote quote = new StockBoardConstituentQuote();
        quote.setBoardCode(INDUSTRY);
        quote.setStockCode(code);
        quote.setStockName(name);
        quote.setLatestPrice(new BigDecimal("10.00"));
        quote.setChangeAmount(new BigDecimal("0.10"));
        quote.setChangePercent(new BigDecimal("1.00"));
        quote.setCreatedAt(createdAt);
        return quote;
    }

    private StockBoardIndustryConsThs source(String code, String name) {
        StockBoardIndustryConsThs source = new StockBoardIndustryConsThs();
        source.setStockCode(code);
        source.setStockName(name);
        source.setLatestPrice(new BigDecimal("10.00"));
        source.setChangeAmount(new BigDecimal("0.10"));
        source.setChangePercent(new BigDecimal("1.00"));
        return source;
    }

    private StockQuoteHistoryProjection history(String code, String tradeDate, String closePrice) {
        return new StockQuoteHistoryProjection() {
            @Override
            public String getCode() {
                return code;
            }

            @Override
            public String getTradeDate() {
                return tradeDate;
            }

            @Override
            public BigDecimal getClosePrice() {
                return new BigDecimal(closePrice);
            }
        };
    }
}
