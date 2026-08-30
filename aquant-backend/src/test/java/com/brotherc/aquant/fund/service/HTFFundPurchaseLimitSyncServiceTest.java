package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.entity.StockFundAnnouncementSync;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.fund.repository.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.integration.htf.model.HTFFundAnnouncement;
import com.brotherc.aquant.integration.htf.service.HTFFundService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HTFFundPurchaseLimitSyncServiceTest {

    @Mock
    private HTFFundService htfFundService;
    @Mock
    private HTFFundAnnouncementParser htfFundAnnouncementParser;
    @Mock
    private StockFundPurchaseLimitService stockFundPurchaseLimitService;
    @Mock
    private StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;
    @Mock
    private StockSyncRepository stockSyncRepository;

    @InjectMocks
    private HTFFundPurchaseLimitSyncService syncService;

    @Test
    void shouldSaveAllCurrentShareRules() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 29, 10, 0);
        HTFFundAnnouncement announcement = createAnnouncement();
        byte[] attachment = "pdf".getBytes();
        List<FundPurchaseLimitRule> rules = createRules();
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(htfFundService.getLatestNasdaq100LimitAnnouncement()).thenReturn(announcement);
        when(stockFundAnnouncementSyncRepository.findBySourceAndAnnouncementId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(stockFundPurchaseLimitService.hasCurrentPurchaseLimit(anyString(), anyString())).thenReturn(false);
        when(htfFundService.downloadAnnouncement(announcement.getAttachmentUrl())).thenReturn(attachment);
        when(htfFundAnnouncementParser.parse(announcement.getTitle(), attachment)).thenReturn(rules);

        syncService.sync(syncTime);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FundPurchaseLimitRule>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockFundPurchaseLimitService).saveSuccess(
                eq("HTF"), eq("汇添富基金"), any(), any(), anyString(), captor.capture()
        );
        assertThat(captor.getValue()).hasSize(10);
        verify(stockSyncRepository).save(any(StockSync.class));
    }

    @Test
    void shouldNotDownloadProcessedAnnouncementAgain() {
        HTFFundAnnouncement announcement = createAnnouncement();
        StockFundAnnouncementSync existing = new StockFundAnnouncementSync();
        existing.setStatus("SUCCESS");
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(htfFundService.getLatestNasdaq100LimitAnnouncement()).thenReturn(announcement);
        when(stockFundAnnouncementSyncRepository.findBySourceAndAnnouncementId(anyString(), anyString()))
                .thenReturn(Optional.of(existing));
        when(stockFundPurchaseLimitService.hasCurrentPurchaseLimit(anyString(), anyString())).thenReturn(true);

        syncService.sync(LocalDateTime.of(2026, 8, 29, 10, 0));

        verify(htfFundService, never()).downloadAnnouncement(anyString());
        verify(stockFundPurchaseLimitService, never()).saveSuccess(
                anyString(), anyString(), any(), any(), anyString(), any()
        );
        verify(stockSyncRepository).save(any(StockSync.class));
    }

    private HTFFundAnnouncement createAnnouncement() {
        HTFFundAnnouncement result = new HTFFundAnnouncement();
        result.setAnnouncementId("12865832");
        result.setAnnouncementDate(LocalDate.of(2026, 7, 17));
        result.setTitle("关于汇添富纳斯达克100基金调整大额申购、定期定额投资业务限制金额的公告");
        result.setDetailUrl("https://www.99fund.com/main/a/20260717/12865832.shtml");
        result.setAttachmentUrl("https://www.99fund.com/announcement/latest.pdf");
        return result;
    }

    private List<FundPurchaseLimitRule> createRules() {
        List<FundPurchaseLimitRule> result = new ArrayList<>();
        for (String code : List.of("018966", "018967", "018969", "018968", "021773")) {
            for (String businessType : List.of("PURCHASE", "RECURRING_INVESTMENT")) {
                FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
                rule.setFundCode(code);
                rule.setBusinessType(businessType);
                rule.setStatus("LIMITED");
                rule.setLimitAmount(BigDecimal.TEN);
                rule.setEffectiveDate(LocalDate.of(2026, 7, 17));
                result.add(rule);
            }
        }
        return result;
    }

}
