package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.entity.StockFundAnnouncementSync;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.fund.repository.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.integration.cmf.model.CMFFundPurchaseLimit;
import com.brotherc.aquant.integration.cmf.service.CMFFundService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CMFFundPurchaseLimitSyncServiceTest {

    @Mock
    private CMFFundService cmfFundService;
    @Mock
    private StockFundPurchaseLimitService stockFundPurchaseLimitService;
    @Mock
    private StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;
    @Mock
    private StockSyncRepository stockSyncRepository;

    @InjectMocks
    private CMFFundPurchaseLimitSyncService syncService;

    @Test
    void shouldSaveCurrentRulesAndAnnouncement() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 29, 10, 0);
        CMFFundPurchaseLimit announcement = createLimit();
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(cmfFundService.getLatestNasdaq100DirectLimitAnnouncement()).thenReturn(announcement);
        when(stockFundAnnouncementSyncRepository.findBySourceAndAnnouncementId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(stockFundPurchaseLimitService.hasCurrentPurchaseLimit(anyString(), anyString())).thenReturn(false);
        when(cmfFundService.getNasdaq100DirectPurchaseLimit(announcement)).thenReturn(announcement);

        syncService.sync(syncTime);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FundPurchaseLimitRule>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockFundPurchaseLimitService).saveSuccess(
                eq("CMF"), eq("招商基金"), any(), any(), isNull(), captor.capture()
        );
        assertThat(captor.getValue()).hasSize(4).allSatisfy(rule -> {
            assertThat(rule.getCurrency()).isEqualTo("CNY");
            assertThat(rule.getSalesChannel()).isEqualTo("DIRECT");
            assertThat(rule.getStatus()).isEqualTo("LIMITED");
            assertThat(rule.getLimitAmount()).isEqualByComparingTo("100");
            assertThat(rule.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 7, 27));
        });
        assertThat(captor.getValue()).extracting(FundPurchaseLimitRule::getFundCode)
                .containsExactly("019547", "019547", "019548", "019548");
        verify(stockSyncRepository).save(any(StockSync.class));
    }

    @Test
    void shouldNotDownloadProcessedAnnouncementAgain() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 29, 10, 0);
        CMFFundPurchaseLimit announcement = createLimit();
        StockFundAnnouncementSync existing = new StockFundAnnouncementSync();
        existing.setStatus("SUCCESS");
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(cmfFundService.getLatestNasdaq100DirectLimitAnnouncement()).thenReturn(announcement);
        when(stockFundAnnouncementSyncRepository.findBySourceAndAnnouncementId(anyString(), anyString()))
                .thenReturn(Optional.of(existing));
        when(stockFundPurchaseLimitService.hasCurrentPurchaseLimit(anyString(), anyString())).thenReturn(true);

        syncService.sync(syncTime);

        verify(cmfFundService, never()).getNasdaq100DirectPurchaseLimit(any());
        verify(stockFundPurchaseLimitService, never()).saveSuccess(
                anyString(), anyString(), any(), any(), any(), any()
        );
        verify(stockSyncRepository).save(any(StockSync.class));
    }

    private CMFFundPurchaseLimit createLimit() {
        CMFFundPurchaseLimit limit = new CMFFundPurchaseLimit();
        limit.setAnnouncementId("225156");
        limit.setAnnouncementDate(LocalDate.of(2026, 7, 24));
        limit.setTitle("招商纳斯达克100基金在直销机构调整大额申购公告");
        limit.setDetailUrl("https://www.cmfchina.com/web/noticedetails/225156/index.html");
        limit.setPurchaseStatus("LIMITED");
        limit.setPurchaseLimitAmount(BigDecimal.valueOf(100));
        limit.setRecurringStatus("LIMITED");
        limit.setRecurringLimitAmount(BigDecimal.valueOf(100));
        limit.setEffectiveDate(LocalDate.of(2026, 7, 27));
        return limit;
    }

}
