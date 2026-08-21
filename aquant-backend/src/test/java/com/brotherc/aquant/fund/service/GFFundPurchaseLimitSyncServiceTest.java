package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.common.constant.GFFundConstant;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.integration.gf.model.GFFundPurchaseLimit;
import com.brotherc.aquant.integration.gf.service.GFFundService;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GFFundPurchaseLimitSyncServiceTest {

    @Mock
    private GFFundService gfFundService;
    @Mock
    private StockFundPurchaseLimitService stockFundPurchaseLimitService;
    @Mock
    private StockSyncRepository stockSyncRepository;

    @InjectMocks
    private GFFundPurchaseLimitSyncService syncService;

    @Test
    void shouldSaveAllNasdaq100ShareLimitsAndWatermark() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 21, 10, 0);
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(gfFundService.getPersonalPurchaseLimit(anyString())).thenAnswer(invocation -> {
            GFFundPurchaseLimit limit = new GFFundPurchaseLimit();
            limit.setFundCode(invocation.getArgument(0));
            limit.setMaximumPurchaseAmount(new BigDecimal("5"));
            return limit;
        });

        syncService.sync(syncTime);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FundPurchaseLimitRule>> rulesCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockFundPurchaseLimitService).saveCurrentRules(
                anyString(), anyString(), rulesCaptor.capture()
        );
        assertThat(rulesCaptor.getValue()).hasSize(5);
        assertThat(rulesCaptor.getValue()).extracting(FundPurchaseLimitRule::getFundCode)
                .containsExactly("270042", "006479", "021778", "000055", "006480");
        assertThat(rulesCaptor.getValue()).allSatisfy(rule -> {
            assertThat(rule.getLimitAmount()).isEqualByComparingTo("5");
            assertThat(rule.getSalesChannel()).isEqualTo("DIRECT");
            assertThat(rule.getBusinessType()).isEqualTo("PURCHASE");
        });
        verify(stockSyncRepository).save(any(StockSync.class));
    }

    @Test
    void shouldSkipWhenAlreadySyncedToday() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 21, 10, 0);
        StockSync stockSync = new StockSync();
        stockSync.setValue(String.valueOf(LocalDate.of(2026, 8, 21).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()));
        when(stockSyncRepository.findByName(anyString())).thenReturn(stockSync);

        syncService.sync(syncTime);

        verify(gfFundService, never()).getPersonalPurchaseLimit(anyString());
        verify(stockFundPurchaseLimitService, never()).saveCurrentRules(anyString(), anyString(), anyList());
        verify(stockSyncRepository, never()).save(any());
    }

    @Test
    void shouldNotSavePartialResultWhenOneRequestFails() {
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(gfFundService.getPersonalPurchaseLimit(anyString()))
                .thenThrow(new IllegalStateException("request failed"));

        syncService.sync(LocalDateTime.of(2026, 8, 21, 10, 0));

        verify(stockFundPurchaseLimitService, never()).saveCurrentRules(anyString(), anyString(), anyList());
        verify(stockSyncRepository, never()).save(any());
    }

}
