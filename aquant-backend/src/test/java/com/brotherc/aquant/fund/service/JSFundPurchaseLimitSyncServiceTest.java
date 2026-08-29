package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.integration.js.model.JSFundPurchaseLimit;
import com.brotherc.aquant.integration.js.service.JSFundService;
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
class JSFundPurchaseLimitSyncServiceTest {

    @Mock
    private JSFundService jsFundService;
    @Mock
    private StockFundPurchaseLimitService stockFundPurchaseLimitService;
    @Mock
    private StockSyncRepository stockSyncRepository;

    @InjectMocks
    private JSFundPurchaseLimitSyncService syncService;

    @Test
    void shouldSavePurchaseAndRecurringRulesForEveryShare() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 29, 10, 0);
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(jsFundService.getNasdaq100PurchaseLimits()).thenReturn(createLimits());

        syncService.sync(syncTime);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FundPurchaseLimitRule>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockFundPurchaseLimitService).saveCurrentRules(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).hasSize(10);
        assertThat(captor.getValue()).extracting(FundPurchaseLimitRule::getBusinessType)
                .containsOnly("PURCHASE", "RECURRING_INVESTMENT");
        assertThat(captor.getValue()).allSatisfy(rule -> {
            assertThat(rule.getStatus()).isEqualTo("SUSPENDED");
            assertThat(rule.getSalesChannel()).isEqualTo("ALL_CHANNELS");
            assertThat(rule.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 2, 3));
        });
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

        verify(jsFundService, never()).getNasdaq100PurchaseLimits();
        verify(stockFundPurchaseLimitService, never()).saveCurrentRules(anyString(), anyString(), anyList());
    }

    private List<JSFundPurchaseLimit> createLimits() {
        List<JSFundPurchaseLimit> result = new ArrayList<>();
        for (String code : List.of("016532", "016533", "021838", "016534", "016535")) {
            JSFundPurchaseLimit limit = new JSFundPurchaseLimit();
            limit.setFundCode(code);
            limit.setCurrency(code.startsWith("01653") && !"016532".equals(code) && !"016533".equals(code)
                    ? "USD" : "CNY");
            limit.setSalesChannel("ALL_CHANNELS");
            limit.setPurchaseStatus("SUSPENDED");
            limit.setRecurringStatus("SUSPENDED");
            limit.setEffectiveDate(LocalDate.of(2026, 2, 3));
            result.add(limit);
        }
        return result;
    }

}
