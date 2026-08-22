package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.fund.repository.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.integration.efund.model.EFundAnnouncement;
import com.brotherc.aquant.integration.efund.model.EFundAnnouncementPage;
import com.brotherc.aquant.integration.efund.service.EFundService;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EFundPurchaseLimitSyncServiceTest {

    @Mock
    private EFundService eFundService;
    @Mock
    private EFundAnnouncementParser eFundAnnouncementParser;
    @Mock
    private StockFundPurchaseLimitService stockFundPurchaseLimitService;
    @Mock
    private StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;
    @Mock
    private StockSyncRepository stockSyncRepository;

    @InjectMocks
    private EFundPurchaseLimitSyncService syncService;

    @Test
    void shouldProcessLatestLongTermAnnouncementAndSaveWatermark() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 22, 10, 0);
        EFundAnnouncement holiday = announcement("800001", LocalDate.of(2026, 6, 30),
                "易方达纳斯达克100指数基金境外主要投资市场节假日暂停申购的公告");
        EFundAnnouncement limit = announcement("793529", LocalDate.of(2026, 3, 18),
                "易方达纳斯达克100指数基金暂停申购及定期定额投资业务的公告");
        EFundAnnouncementPage page = new EFundAnnouncementPage();
        page.setTotalPages(20);
        page.setContent(List.of(holiday, limit));
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(stockFundAnnouncementSyncRepository.findBySourceAndStatusInOrderByAnnouncementDateDesc(
                anyString(), anyCollection())).thenReturn(List.of());
        when(stockFundAnnouncementSyncRepository.findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                anyString(), anyString())).thenReturn(Optional.empty());
        when(stockFundAnnouncementSyncRepository.findBySourceAndAnnouncementIdIn(
                anyString(), anyCollection())).thenReturn(List.of());
        when(eFundService.getNasdaq100Announcements(1)).thenReturn(page);
        when(eFundService.downloadAnnouncement(anyString())).thenReturn(new byte[]{1});
        List<FundPurchaseLimitRule> rules = new ArrayList<>();
        for (String code : List.of("161130", "012870", "003722", "012871")) {
            FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
            rule.setFundCode(code);
            rule.setBusinessType("PURCHASE");
            rule.setSalesChannel("ALL_CHANNELS");
            rule.setStatus("SUSPENDED");
            rules.add(rule);
        }
        when(eFundAnnouncementParser.parse(anyString(), any())).thenReturn(rules);
        when(stockFundPurchaseLimitService.hasCurrentPurchaseLimit(anyString(), anyString()))
                .thenReturn(false, true, true, true, true);

        syncService.sync(syncTime);

        verify(eFundService, never()).downloadAnnouncement(holiday.getAttachmentUrl());
        verify(stockFundPurchaseLimitService).saveSuccess(
                anyString(), anyString(), any(), any(), anyString(), any()
        );
        verify(stockSyncRepository).save(any(StockSync.class));
    }

    @Test
    void shouldSkipWhenAlreadySyncedToday() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 22, 10, 0);
        StockSync stockSync = new StockSync();
        stockSync.setValue(String.valueOf(LocalDate.of(2026, 8, 22).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()));
        when(stockSyncRepository.findByName(anyString())).thenReturn(stockSync);

        syncService.sync(syncTime);

        verify(eFundService, never()).getNasdaq100Announcements(1);
    }

    private EFundAnnouncement announcement(String id, LocalDate date, String title) {
        EFundAnnouncement announcement = new EFundAnnouncement();
        announcement.setAnnouncementId(id);
        announcement.setAnnouncementDate(date);
        announcement.setTitle(title);
        announcement.setDetailUrl("https://www.efunds.com.cn/c/" + id + ".shtml");
        announcement.setAttachmentUrl("https://cdn.efunds.com.cn/announcements/" + id + ".pdf");
        return announcement;
    }

}
