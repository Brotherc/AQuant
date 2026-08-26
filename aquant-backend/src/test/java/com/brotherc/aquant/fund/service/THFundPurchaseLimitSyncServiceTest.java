package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.fund.repository.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.integration.th.model.THFundAnnouncement;
import com.brotherc.aquant.integration.th.model.THFundAnnouncementPage;
import com.brotherc.aquant.integration.th.service.THFundService;
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
class THFundPurchaseLimitSyncServiceTest {

    @Mock
    private THFundService thFundService;
    @Mock
    private THFundAnnouncementParser thFundAnnouncementParser;
    @Mock
    private StockFundPurchaseLimitService stockFundPurchaseLimitService;
    @Mock
    private StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;
    @Mock
    private StockSyncRepository stockSyncRepository;

    @InjectMocks
    private THFundPurchaseLimitSyncService syncService;

    @Test
    void shouldProcessLatestAnnouncementAndStopInitialBackfill() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 24, 10, 0);
        THFundAnnouncement suspension = announcement("574b", LocalDate.of(2026, 5, 29),
                "天弘纳斯达克100指数基金暂停申购及定期定额投资业务的公告");
        THFundAnnouncement holiday = announcement("e771", LocalDate.of(2026, 1, 15),
                "天弘纳斯达克100指数基金在境外主要投资场所节假日暂停申购的公告");
        THFundAnnouncementPage page = new THFundAnnouncementPage();
        page.setTotalPages(2);
        page.setContent(List.of(suspension, holiday));
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(stockFundAnnouncementSyncRepository.findBySourceAndStatusInOrderByAnnouncementDateDesc(
                anyString(), anyCollection())).thenReturn(List.of());
        when(stockFundAnnouncementSyncRepository.findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                anyString(), anyString())).thenReturn(Optional.empty());
        when(stockFundAnnouncementSyncRepository.findBySourceAndAnnouncementIdIn(
                anyString(), anyCollection())).thenReturn(List.of());
        when(thFundService.getNasdaq100Announcements(1)).thenReturn(page);
        when(thFundService.downloadAnnouncement(anyString())).thenReturn(new byte[]{1});
        List<FundPurchaseLimitRule> rules = new ArrayList<>();
        for (String code : List.of("018043", "018044", "022525")) {
            FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
            rule.setFundCode(code);
            rule.setBusinessType("PURCHASE");
            rule.setSalesChannel("ALL_CHANNELS");
            rule.setStatus("SUSPENDED");
            rules.add(rule);
        }
        when(thFundAnnouncementParser.parse(anyString(), any())).thenReturn(rules);
        when(stockFundPurchaseLimitService.hasCurrentPurchaseLimit(anyString(), anyString()))
                .thenReturn(false, true, true, true);

        syncService.sync(syncTime);

        verify(thFundService, never()).downloadAnnouncement(holiday.getAttachmentUrl());
        verify(thFundService, never()).getNasdaq100Announcements(2);
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

        verify(thFundService, never()).getNasdaq100Announcements(1);
    }

    private THFundAnnouncement announcement(String id, LocalDate date, String title) {
        THFundAnnouncement announcement = new THFundAnnouncement();
        announcement.setAnnouncementId(id);
        announcement.setAnnouncementDate(date);
        announcement.setTitle(title);
        announcement.setDetailUrl("https://www.thfund.com.cn/notice_list?title=天弘纳斯达克100");
        announcement.setAttachmentUrl("https://pdf.dfcfw.com/pdf/H2_" + id + "_1.pdf");
        return announcement;
    }

}
