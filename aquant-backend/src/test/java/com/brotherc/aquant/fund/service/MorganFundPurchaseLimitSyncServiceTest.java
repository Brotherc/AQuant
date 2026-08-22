package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.common.constant.MorganFundConstant;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitAnnouncement;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.fund.repository.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.integration.morgan.model.MorganFundAnnouncementPage;
import com.brotherc.aquant.integration.morgan.service.MorganFundService;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class MorganFundPurchaseLimitSyncServiceTest {

    @Mock
    private MorganFundService morganFundService;
    @Mock
    private MorganFundAnnouncementParser morganFundAnnouncementParser;
    @Mock
    private StockFundPurchaseLimitService stockFundPurchaseLimitService;
    @Mock
    private StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;
    @Mock
    private StockSyncRepository stockSyncRepository;

    @InjectMocks
    private MorganFundPurchaseLimitSyncService syncService;

    @Test
    void shouldProcessLatestAnnouncementAndSaveWatermark() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 22, 10, 0);
        FundPurchaseLimitAnnouncement announcement = new FundPurchaseLimitAnnouncement();
        announcement.setAnnouncementId("P020260724555732437745");
        announcement.setAnnouncementDate(LocalDate.of(2026, 7, 24));
        announcement.setTitle("摩根纳斯达克100指数基金调整大额申购限制金额的公告");
        MorganFundAnnouncementPage page = new MorganFundAnnouncementPage();
        page.setTotalPages(11);
        page.setContent(List.of(announcement));
        when(stockSyncRepository.findByName(anyString())).thenReturn(null);
        when(stockFundAnnouncementSyncRepository.findBySourceAndStatusInOrderByAnnouncementDateDesc(
                anyString(), anyCollection())).thenReturn(List.of());
        when(stockFundAnnouncementSyncRepository.findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                anyString(), anyString())).thenReturn(Optional.empty());
        when(stockFundAnnouncementSyncRepository.findBySourceAndAnnouncementIdIn(
                anyString(), anyCollection())).thenReturn(List.of());
        when(morganFundService.getNasdaq100Announcements(1)).thenReturn(page);
        when(morganFundService.getAnnouncementUrl(anyString(), any(LocalDate.class)))
                .thenReturn("https://www.cifm.com/fund/019172/announce/202607/test.pdf");
        when(morganFundService.downloadAnnouncement(anyString())).thenReturn(new byte[]{1});
        List<FundPurchaseLimitRule> rules = new ArrayList<>();
        for (String code : List.of("019172", "019173", "019174", "019175")) {
            FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
            rule.setFundCode(code);
            rule.setBusinessType("PURCHASE");
            rule.setSalesChannel("ALL_CHANNELS");
            rule.setStatus("LIMITED");
            rule.setLimitAmount(BigDecimal.TEN);
            rules.add(rule);
        }
        when(morganFundAnnouncementParser.parse(anyString(), any())).thenReturn(rules);
        when(stockFundPurchaseLimitService.hasCurrentPurchaseLimit(anyString(), anyString()))
                .thenReturn(false, true, true, true, true);

        syncService.sync(syncTime);

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

        verify(morganFundService, never()).getNasdaq100Announcements(1);
        verify(stockFundPurchaseLimitService, never()).saveSuccess(
                anyString(), anyString(), any(), any(), anyString(), any()
        );
    }

}
