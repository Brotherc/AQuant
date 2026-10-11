package com.brotherc.aquant.stock.service;

import com.brotherc.aquant.stock.entity.StockAbnormal;
import com.brotherc.aquant.stock.repository.StockAbnormalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAbnormalServiceTest {

    @Mock
    private StockAbnormalRepository repository;

    @InjectMocks
    private StockAbnormalService service;

    @Test
    void shouldOnlyInsertCodesNotAlreadyArchived() {
        when(repository.findCodesByCodeIn(anyCollection())).thenReturn(List.of("sz000001"));

        service.saveDelistedStocks(Map.of("sz000001", "退市股票", "sz000002", "另一股票"),
                "stock_info_sz_delist", "深交所终止上市公司");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockAbnormal>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<StockAbnormal> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved).allSatisfy(stock -> {
            assertThat(stock.getAbnormalType()).isEqualTo("DELISTED");
            assertThat(stock.getAbnormalSource()).isEqualTo("stock_info_sz_delist");
            assertThat(stock.getAbnormalReason()).isEqualTo("深交所终止上市公司");
        });
        assertThat(saved.get(0).getCode()).isEqualTo("sz000002");
        assertThat(saved.get(0).getName()).isEqualTo("另一股票");
        assertThat(saved.get(0).getId()).isNull();
    }

    @Test
    void shouldSkipSavingWhenAllCodesAlreadyExist() {
        when(repository.findCodesByCodeIn(anyCollection())).thenReturn(List.of("sz000001"));

        service.saveDelistedStocks(Map.of("sz000001", "退市股票"),
                "stock_info_sz_delist", "深交所终止上市公司");

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void shouldSkipEmptyExchangeList() {
        service.saveDelistedStocks(Map.of(), "stock_info_sz_delist", "深交所终止上市公司");
        verifyNoInteractions(repository);
    }
}
