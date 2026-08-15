package com.brotherc.aquant.service.fund;

import com.brotherc.aquant.constant.CCBFundConstant;
import com.brotherc.aquant.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.entity.fund.StockFundAnnouncementSync;
import com.brotherc.aquant.entity.sync.StockSync;
import com.brotherc.aquant.model.dto.ccbfund.CCBFundAnnouncement;
import com.brotherc.aquant.model.dto.ccbfund.CCBFundAnnouncementPage;
import com.brotherc.aquant.model.dto.ccbfund.CCBFundInfo;
import com.brotherc.aquant.repository.fund.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.repository.sync.StockSyncRepository;
import com.brotherc.aquant.service.ccb.CCBFundService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CCBFundPurchaseLimitSyncServiceTest {

    @Mock
    private CCBFundService ccbFundService;
    @Mock
    private CCBFundAnnouncementParser ccbFundAnnouncementParser;
    @Mock
    private StockFundPurchaseLimitService stockFundPurchaseLimitService;
    @Mock
    private StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;
    @Mock
    private StockSyncRepository stockSyncRepository;

    @InjectMocks
    private CCBFundPurchaseLimitSyncService syncService;

    @Test
    void shouldNotDownloadAttachmentForSuccessfullyProcessedAnnouncement() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 12, 10, 0);
        StockSync watermark = new StockSync();
        watermark.setValue(String.valueOf(LocalDate.of(2026, 8, 11).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()));
        when(stockSyncRepository.findByName(any())).thenReturn(watermark);

        CCBFundInfo fund = new CCBFundInfo();
        fund.setFundCode("539001");
        fund.setFundName("建信纳斯达克100指数（QDII）");
        when(ccbFundService.getNasdaq100IndexFunds()).thenReturn(List.of(fund));
        when(stockFundAnnouncementSyncRepository.findBySourceAndStatusInOrderByAnnouncementDateDesc(
                CCBFundConstant.SOURCE,
                List.of(FundPurchaseLimitConstant.SYNC_FAILED, FundPurchaseLimitConstant.SYNC_PENDING)))
                .thenReturn(List.of());

        CCBFundAnnouncement announcement = new CCBFundAnnouncement();
        announcement.setAnnouncementId("303416");
        announcement.setAnnouncementDate(LocalDate.of(2026, 8, 12));
        CCBFundAnnouncementPage page = new CCBFundAnnouncementPage();
        page.setContent(List.of(announcement));
        page.setTotalPages(3);
        when(stockFundAnnouncementSyncRepository.findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                CCBFundConstant.SOURCE, FundPurchaseLimitConstant.SYNC_SUCCESS)).thenReturn(Optional.empty());
        when(stockFundAnnouncementSyncRepository.findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                CCBFundConstant.SOURCE, FundPurchaseLimitConstant.SYNC_IGNORED)).thenReturn(Optional.empty());
        when(ccbFundService.getPurchaseLimitAnnouncements(
                "539001", null, syncTime.toLocalDate(), 1)).thenReturn(page);

        StockFundAnnouncementSync processed = new StockFundAnnouncementSync();
        processed.setAnnouncementId("303416");
        processed.setStatus(FundPurchaseLimitConstant.SYNC_SUCCESS);
        when(stockFundAnnouncementSyncRepository.findBySourceAndAnnouncementIdIn(
                CCBFundConstant.SOURCE, List.of("303416")))
                .thenReturn(List.of(processed));

        syncService.sync(syncTime);

        verify(ccbFundService, never()).getAnnouncementDetail(any());
        verify(stockSyncRepository).save(watermark);
    }

    @Test
    void shouldIgnoreAnnouncementsBeforeIncrementalStartDate() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 8, 14, 10, 0);
        StockSync watermark = new StockSync();
        watermark.setValue(String.valueOf(LocalDate.of(2026, 8, 13).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()));
        when(stockSyncRepository.findByName(any())).thenReturn(watermark);

        CCBFundInfo fund = new CCBFundInfo();
        fund.setFundCode("539001");
        fund.setFundName("建信纳斯达克100指数（QDII）");
        when(ccbFundService.getNasdaq100IndexFunds()).thenReturn(List.of(fund));
        when(stockFundAnnouncementSyncRepository.findBySourceAndStatusInOrderByAnnouncementDateDesc(
                CCBFundConstant.SOURCE,
                List.of(FundPurchaseLimitConstant.SYNC_FAILED, FundPurchaseLimitConstant.SYNC_PENDING)))
                .thenReturn(List.of());

        StockFundAnnouncementSync latest = new StockFundAnnouncementSync();
        latest.setAnnouncementDate(LocalDate.of(2026, 8, 12));
        when(stockFundAnnouncementSyncRepository.findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                CCBFundConstant.SOURCE, FundPurchaseLimitConstant.SYNC_SUCCESS)).thenReturn(Optional.of(latest));
        when(stockFundAnnouncementSyncRepository.findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                CCBFundConstant.SOURCE, FundPurchaseLimitConstant.SYNC_IGNORED)).thenReturn(Optional.empty());

        CCBFundAnnouncement oldAnnouncement = new CCBFundAnnouncement();
        oldAnnouncement.setAnnouncementId("298719");
        oldAnnouncement.setAnnouncementDate(LocalDate.of(2026, 2, 25));
        CCBFundAnnouncementPage page = new CCBFundAnnouncementPage();
        page.setContent(List.of(oldAnnouncement));
        page.setTotalPages(14);
        when(ccbFundService.getPurchaseLimitAnnouncements(
                "539001", LocalDate.of(2026, 8, 12), syncTime.toLocalDate(), 1)).thenReturn(page);

        syncService.sync(syncTime);

        verify(ccbFundService, never()).getAnnouncementDetail(any());
        verify(stockFundAnnouncementSyncRepository, never()).findBySourceAndAnnouncementIdIn(any(), any());
        verify(stockSyncRepository).save(watermark);
    }
}
