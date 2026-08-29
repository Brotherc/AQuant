package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.integration.chinaamc.model.ChinaAMCFundPurchaseLimit;
import com.brotherc.aquant.integration.chinaamc.service.ChinaAMCFundService;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class ChinaAMCFundPurchaseLimitSyncServiceTest {

    @Mock
    private ChinaAMCFundService chinaAMCFundService;
    @Mock
    private StockFundPurchaseLimitService stockFundPurchaseLimitService;
    @Mock
    private StockSyncRepository stockSyncRepository;

    @InjectMocks
    private ChinaAMCFundPurchaseLimitSyncService syncService;

    @Test
    void shouldSavePurchaseAndRecurringRulesForEveryShare() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 29, 10, 0);
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(chinaAMCFundService.getNasdaq100PurchaseLimits()).thenReturn(createLimits());

        syncService.sync(syncTime);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FundPurchaseLimitRule>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockFundPurchaseLimitService).saveCurrentRules(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).hasSize(6).allSatisfy(rule -> {
            assertThat(rule.getStatus()).isEqualTo("SUSPENDED");
            assertThat(rule.getSalesChannel()).isEqualTo("DIRECT");
            assertThat(rule.getEffectiveDate()).isNull();
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

        verify(chinaAMCFundService, never()).getNasdaq100PurchaseLimits();
        verify(stockFundPurchaseLimitService, never()).saveCurrentRules(anyString(), anyString(), anyList());
    }

    private List<ChinaAMCFundPurchaseLimit> createLimits() {
        List<ChinaAMCFundPurchaseLimit> result = new ArrayList<>();
        for (String code : List.of("015299", "015300", "015518")) {
            ChinaAMCFundPurchaseLimit limit = new ChinaAMCFundPurchaseLimit();
            limit.setFundCode(code);
            limit.setCurrency("015518".equals(code) ? "USD" : "CNY");
            limit.setSalesChannel("DIRECT");
            limit.setPurchaseStatus("SUSPENDED");
            limit.setRecurringStatus("SUSPENDED");
            result.add(limit);
        }
        return result;
    }

}
