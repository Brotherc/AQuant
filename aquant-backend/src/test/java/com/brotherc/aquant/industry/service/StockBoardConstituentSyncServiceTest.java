package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.industry.entity.StockIndustryBoard;
import com.brotherc.aquant.industry.repository.StockBoardConstituentRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardRepository;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryConsThs;
import com.brotherc.aquant.integration.akshare.service.AKShareIndustryService;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jsoup.HttpStatusException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBoardConstituentSyncServiceTest {

    @Mock
    private StockHelper stockHelper;

    @Mock
    private StockSyncRepository stockSyncRepository;

    @Mock
    private StockIndustryBoardRepository boardRepository;

    @Mock
    private StockBoardConstituentRepository memberRepository;

    @Mock
    private AKShareIndustryService industryService;

    @Mock
    private StockBoardConstituentPersistenceService persistenceService;

    @InjectMocks
    private StockBoardConstituentSyncService service;

    @Test
    void skipsAllUpstreamRequestsWhenTheIndustryWatermarkIsCurrent() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 16, 0);
        when(stockHelper.getLatestClosedTradeDaySyncWatermark(now)).thenReturn(100L);
        when(stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST)).thenReturn(sync("board", 100L));
        when(boardRepository.findAll()).thenReturn(List.of(board("银行")));
        when(memberRepository.existsByBoardCode("银行")).thenReturn(true);
        when(stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_CONSTITUENT_LATEST_PREFIX + "银行"))
                .thenReturn(sync("member", 100L));

        service.synchronizeAllIfRequired(now);

        verify(industryService, never()).stockBoardIndustryConstituentsThs(any());
        verify(persistenceService, never()).replace(any(), any(), anyLong());
    }

    @Test
    void keepsExistingMembersWhenOneIndustrySourceFails() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 16, 0);
        when(stockHelper.getLatestClosedTradeDaySyncWatermark(now)).thenReturn(100L);
        when(stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST)).thenReturn(sync("board", 100L));
        when(boardRepository.findAll()).thenReturn(List.of(board("银行")));
        when(memberRepository.existsByBoardCode("银行")).thenReturn(true);
        when(stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_CONSTITUENT_LATEST_PREFIX + "银行"))
                .thenReturn(sync("member", 99L));
        when(industryService.stockBoardIndustryConstituentsThs("银行")).thenThrow(new IllegalStateException("upstream failed"));

        service.synchronizeAllIfRequired(now);

        verify(persistenceService, never()).replace(any(), any(), eq(100L));
    }

    @Test
    void retriesTheIndustryRequestAndOnlyPersistsAfterASuccessfulAttempt() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 16, 0);
        when(stockHelper.getLatestClosedTradeDaySyncWatermark(now)).thenReturn(100L);
        when(stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST)).thenReturn(sync("board", 100L));
        when(boardRepository.findAll()).thenReturn(List.of(board("银行")));
        when(memberRepository.existsByBoardCode("银行")).thenReturn(false);
        StockBoardIndustryConsThs member = new StockBoardIndustryConsThs();
        member.setStockCode("600000");
        member.setStockName("浦发银行");
        when(industryService.stockBoardIndustryConstituentsThs("银行"))
                .thenThrow(new IllegalStateException("temporary failure"))
                .thenReturn(List.of(member));

        service.synchronizeAllIfRequired(now);

        verify(industryService, times(2)).stockBoardIndustryConstituentsThs("银行");
        verify(persistenceService).replace("银行", List.of(member), 100L);
    }

    @Test
    void doesNotRetryOrAdvanceWatermarkWhenTheThsSourceReturnsForbidden() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 16, 0);
        when(stockHelper.getLatestClosedTradeDaySyncWatermark(now)).thenReturn(100L);
        when(stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST)).thenReturn(sync("board", 100L));
        when(boardRepository.findAll()).thenReturn(List.of(board("银行")));
        when(memberRepository.existsByBoardCode("银行")).thenReturn(false);
        HttpStatusException forbidden = new HttpStatusException("forbidden", 403, "https://q.10jqka.com.cn/");
        when(industryService.stockBoardIndustryConstituentsThs("银行"))
                .thenThrow(new IllegalStateException("upstream failed", forbidden));

        service.synchronizeAllIfRequired(now);

        verify(industryService).stockBoardIndustryConstituentsThs("银行");
        verify(persistenceService, never()).replace(any(), any(), anyLong());
    }

    private StockIndustryBoard board(String name) {
        StockIndustryBoard board = new StockIndustryBoard();
        board.setSectorName(name);
        return board;
    }

    private StockSync sync(String name, long value) {
        StockSync sync = new StockSync();
        sync.setName(name);
        sync.setValue(String.valueOf(value));
        return sync;
    }
}
