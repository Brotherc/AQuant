package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.fund.repository.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.integration.nf.model.NFFundAnnouncement;
import com.brotherc.aquant.integration.nf.model.NFFundAnnouncementPage;
import com.brotherc.aquant.integration.nf.service.NFFundService;
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
class NFFundPurchaseLimitSyncServiceTest {

    @Mock
    private NFFundService nfFundService;
    @Mock
    private NFFundAnnouncementParser nfFundAnnouncementParser;
    @Mock
    private StockFundPurchaseLimitService stockFundPurchaseLimitService;
    @Mock
    private StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;
    @Mock
    private StockSyncRepository stockSyncRepository;

    @InjectMocks
    private NFFundPurchaseLimitSyncService syncService;

    @Test
    void shouldProcessLatestLimitAnnouncementAndSaveWatermark() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 24, 10, 0);
        NFFundAnnouncement holiday = announcement("129740", LocalDate.of(2026, 1, 8),
                "关于南方纳斯达克100指数基金境外主要市场节假日申购赎回安排的公告");
        NFFundAnnouncement limit = announcement("133684", LocalDate.of(2026, 7, 8),
                "关于调整南方纳斯达克100指数基金申购、定投及转换转入业务金额限制的公告");
        NFFundAnnouncementPage page = new NFFundAnnouncementPage();
        page.setTotalPages(2);
        page.setContent(List.of(limit, holiday));
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(stockFundAnnouncementSyncRepository.findBySourceAndStatusInOrderByAnnouncementDateDesc(
                anyString(), anyCollection())).thenReturn(List.of());
        when(stockFundAnnouncementSyncRepository.findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                anyString(), anyString())).thenReturn(Optional.empty());
        when(stockFundAnnouncementSyncRepository.findBySourceAndAnnouncementIdIn(
                anyString(), anyCollection())).thenReturn(List.of());
        when(nfFundService.getNasdaq100Announcements(1)).thenReturn(page);
        when(nfFundService.downloadAnnouncement(anyString())).thenReturn(new byte[]{1});
        List<FundPurchaseLimitRule> rules = new ArrayList<>();
        for (String code : List.of("016452", "016453", "021000")) {
            FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
            rule.setFundCode(code);
            rule.setBusinessType("PURCHASE");
            rule.setSalesChannel("ALL_CHANNELS");
            rule.setStatus("LIMITED");
            rules.add(rule);
        }
        when(nfFundAnnouncementParser.parse(anyString(), any())).thenReturn(rules);
        when(stockFundPurchaseLimitService.hasCurrentPurchaseLimit(anyString(), anyString()))
                .thenReturn(false, true, true, true);

        syncService.sync(syncTime);

        verify(nfFundService, never()).downloadAnnouncement(holiday.getAttachmentUrl());
        verify(stockFundPurchaseLimitService).saveSuccess(
                anyString(), anyString(), any(), any(), anyString(), any()
        );
        verify(stockSyncRepository).save(any(StockSync.class));
    }

    @Test
    void shouldSkipWhenAlreadySyncedToday() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 24, 10, 0);
        StockSync stockSync = new StockSync();
        stockSync.setValue(String.valueOf(LocalDate.of(2026, 8, 24).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()));
        when(stockSyncRepository.findByName(anyString())).thenReturn(stockSync);

        syncService.sync(syncTime);

        verify(nfFundService, never()).getNasdaq100Announcements(1);
    }

    private NFFundAnnouncement announcement(String id, LocalDate date, String title) {
        NFFundAnnouncement announcement = new NFFundAnnouncement();
        announcement.setAnnouncementId(id);
        announcement.setAnnouncementDate(date);
        announcement.setTitle(title);
        announcement.setDetailUrl("https://www.nffund.com/main/files/2026/07/08/" + id + ".pdf");
        announcement.setAttachmentUrl(announcement.getDetailUrl());
        return announcement;
    }

}
