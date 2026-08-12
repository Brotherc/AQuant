package com.brotherc.aquant.service.indicator;

import com.brotherc.aquant.entity.indicator.StockBalanceSheet;
import com.brotherc.aquant.entity.stock.StockQuote;
import com.brotherc.aquant.model.dto.akshare.StockZcfzEm;
import com.brotherc.aquant.repository.indicator.StockBalanceSheetRepository;
import com.brotherc.aquant.repository.stock.StockQuoteRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBalanceSheetServiceTest {

    @Mock
    private StockBalanceSheetRepository stockBalanceSheetRepository;

    @Mock
    private StockQuoteRepository stockQuoteRepository;

    @InjectMocks
    private StockBalanceSheetService stockBalanceSheetService;

    @Test
    void shouldMergeDuplicateCodesAndSaveCurrentStockPool() {
        StockZcfzEm oldMainItem = createSource("600000", "浦发银行", "100.01", "20.01");
        StockZcfzEm latestMainItem = createSource("600000", "浦发银行", "101.01", "21.01");
        StockZcfzEm bjItem = createSource("920703", "广厦环能", "200.02", "150.02");
        StockZcfzEm invalidItem = createSource("123456", "非股票池数据", "300.03", "100.03");

        StockQuote mainQuote = new StockQuote();
        mainQuote.setCode("sh600000");
        StockQuote bjQuote = new StockQuote();
        bjQuote.setCode("bj920703");
        when(stockQuoteRepository.findByCodeIn(anyList())).thenReturn(List.of(mainQuote, bjQuote));

        stockBalanceSheetService.save(
                "20260630", List.of(oldMainItem, latestMainItem, bjItem, invalidItem));

        verify(stockBalanceSheetRepository).deleteByReportDate(LocalDate.of(2026, 6, 30));
        verify(stockBalanceSheetRepository).flush();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<StockBalanceSheet>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(stockBalanceSheetRepository).saveAll(captor.capture());
        List<StockBalanceSheet> savedList = ((List<StockBalanceSheet>) captor.getValue());

        assertThat(savedList).hasSize(2);
        assertThat(savedList).extracting(StockBalanceSheet::getStockCode)
                .containsExactlyInAnyOrder("600000", "920703");
        StockBalanceSheet mainEntity = savedList.stream()
                .filter(item -> "600000".equals(item.getStockCode()))
                .findFirst()
                .orElseThrow();
        assertThat(mainEntity.getReportDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(mainEntity.getTotalAssets()).isEqualByComparingTo("101.01");
        assertThat(mainEntity.getTotalEquity()).isEqualByComparingTo("21.01");
        assertThat(mainEntity.getAnnouncementDate()).isEqualTo(LocalDate.of(2026, 8, 12));
    }

    @Test
    void shouldKeepExistingReportWhenNoSourceMatchesCurrentStockPool() {
        StockZcfzEm source = createSource("123456", "非股票池数据", "100", "20");
        when(stockQuoteRepository.findByCodeIn(anyList())).thenReturn(List.of());

        stockBalanceSheetService.save("20260630", List.of(source));

        verify(stockBalanceSheetRepository, never()).deleteByReportDate(LocalDate.of(2026, 6, 30));
        verify(stockBalanceSheetRepository, never()).saveAll(anyList());
    }

    private StockZcfzEm createSource(String code, String name, String totalAssets, String totalEquity) {
        StockZcfzEm source = new StockZcfzEm();
        source.setStockCode(code);
        source.setStockName(name);
        source.setAssetTotalAssets(new BigDecimal(totalAssets));
        source.setTotalEquity(new BigDecimal(totalEquity));
        source.setNoticeDate("2026-08-12T00:00:00.000");
        return source;
    }

}
