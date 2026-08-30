package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.integration.dc.model.DCFundPurchaseLimit;
import com.brotherc.aquant.integration.dc.service.DCFundService;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DCFundPurchaseLimitSyncServiceTest {

    @Mock
    private DCFundService dcFundService;
    @Mock
    private StockFundPurchaseLimitService stockFundPurchaseLimitService;
    @Mock
    private StockSyncRepository stockSyncRepository;

    @InjectMocks
    private DCFundPurchaseLimitSyncService syncService;

    @Test
    void shouldSavePurchaseAndRecurringRulesForBothShares() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 29, 10, 0);
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(dcFundService.getNasdaq100PurchaseLimits()).thenReturn(createLimits());

        syncService.sync(syncTime);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FundPurchaseLimitRule>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockFundPurchaseLimitService).saveCurrentRules(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).hasSize(4).allSatisfy(rule -> {
            assertThat(rule.getSalesChannel()).isEqualTo("DIRECT");
            assertThat(rule.getStatus()).isEqualTo("LIMITED");
            assertThat(rule.getLimitAmount()).isEqualByComparingTo("100");
        });
        assertThat(captor.getValue()).extracting(FundPurchaseLimitRule::getBusinessType)
                .containsOnly("PURCHASE", "RECURRING_INVESTMENT");
        verify(stockSyncRepository).save(any(StockSync.class));
    }

    @Test
    void shouldSkipWhenAlreadySyncedToday() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 29, 10, 0);
        StockSync stockSync = new StockSync();
        stockSync.setValue(String.valueOf(LocalDate.of(2026, 8, 29).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()));
        when(stockSyncRepository.findByName(anyString())).thenReturn(stockSync);

        syncService.sync(syncTime);

        verify(dcFundService, never()).getNasdaq100PurchaseLimits();
        verify(stockFundPurchaseLimitService, never()).saveCurrentRules(anyString(), anyString(), anyList());
    }

    @Test
    void shouldKeepOldRulesAndWatermarkWhenOfficialPageFails() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 29, 10, 0);
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(dcFundService.getNasdaq100PurchaseLimits()).thenThrow(new IllegalStateException("官网结构变化"));

        syncService.sync(syncTime);

        verify(stockFundPurchaseLimitService, never()).saveCurrentRules(anyString(), anyString(), anyList());
        verify(stockSyncRepository, never()).save(any(StockSync.class));
    }

    private List<DCFundPurchaseLimit> createLimits() {
        List<DCFundPurchaseLimit> result = new ArrayList<>();
        for (String fundCode : List.of("000834", "008971")) {
            DCFundPurchaseLimit limit = new DCFundPurchaseLimit();
            limit.setFundCode(fundCode);
            limit.setCurrency("CNY");
            limit.setSalesChannel("DIRECT");
            limit.setPurchaseStatus("LIMITED");
            limit.setPurchaseLimitAmount(new BigDecimal("100"));
            limit.setRecurringStatus("LIMITED");
            limit.setRecurringLimitAmount(new BigDecimal("100"));
            result.add(limit);
        }
        return result;
    }

}
