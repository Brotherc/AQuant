package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.industry.entity.StockBoardConstituentQuote;
import com.brotherc.aquant.industry.repository.StockBoardConstituentQuoteRepository;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryConsThs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBoardConstituentQuoteSyncServiceTest {

    @Mock
    private StockBoardConstituentQuoteRepository quoteRepository;

    @InjectMocks
    private StockBoardConstituentQuoteSyncService service;

    @Test
    void upsertsSourceMembersAndRemovesMembersMissingFromASuccessfulResponse() {
        StockBoardConstituentQuote existing = new StockBoardConstituentQuote();
        existing.setBoardCode("银行");
        existing.setStockCode("600000");
        existing.setStockName("旧名称");
        when(quoteRepository.findByBoardCodeOrderByChangePercentDesc("银行")).thenReturn(List.of(existing));

        service.sync("银行", List.of(
                source("600000", "浦发银行"),
                source("000001", "平安银行"),
                source("000001", "平安银行")
        ));

        ArgumentCaptor<List<StockBoardConstituentQuote>> saved = ArgumentCaptor.forClass(List.class);
        verify(quoteRepository).saveAll(saved.capture());
        assertEquals(2, saved.getValue().size());
        assertEquals(
                java.util.Set.of("600000", "000001"),
                saved.getValue().stream().map(StockBoardConstituentQuote::getStockCode).collect(java.util.stream.Collectors.toSet())
        );
        assertEquals("浦发银行", existing.getStockName());
        verify(quoteRepository).deleteByBoardCodeAndStockCodeNotIn(
                "银行", List.of("600000", "000001")
        );
    }

    @Test
    void rejectsAnEmptyResponseWithoutTouchingTheCache() {
        assertThrows(IllegalStateException.class, () -> service.sync("银行", List.of()));

        verify(quoteRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(quoteRepository, never()).deleteByBoardCodeAndStockCodeNotIn(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList()
        );
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
}
