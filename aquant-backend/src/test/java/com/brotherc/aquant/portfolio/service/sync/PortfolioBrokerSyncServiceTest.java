package com.brotherc.aquant.portfolio.service.sync;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.portfolio.entity.UserBrokerAccount;
import com.brotherc.aquant.portfolio.model.vo.BrokerPositionSnapshotVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioImportResultVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeBatchReqVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;
import com.brotherc.aquant.portfolio.repository.UserBrokerAccountRepository;
import com.brotherc.aquant.portfolio.service.UserPortfolioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PortfolioBrokerSyncServiceTest {

    @Mock
    private UserBrokerAccountRepository accountRepository;

    @Mock
    private UserPortfolioService portfolioService;

    @Mock
    private PortfolioBrokerSyncProvider easytraderProvider;

    @Mock
    private PortfolioBrokerSyncProvider emWebProvider;

    private PortfolioBrokerSyncService service() {
        when(easytraderProvider.channel()).thenReturn("EASYTRADER");
        when(emWebProvider.channel()).thenReturn("EM_WEB");
        return new PortfolioBrokerSyncService(accountRepository, portfolioService,
                List.of(easytraderProvider, emWebProvider));
    }

    private UserBrokerAccount account(String syncMode) {
        UserBrokerAccount account = new UserBrokerAccount();
        account.setId(1L);
        account.setUserId(9L);
        account.setSyncMode(syncMode);
        return account;
    }

    @Test
    void routesToEasytraderProviderByAccountSyncMode() {
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 9L))
                .thenReturn(Optional.of(account("EASYTRADER")));
        when(easytraderProvider.fetchTrades()).thenReturn(List.of(new PortfolioTradeSaveReqVO()));

        service().syncTrades(1L, 9L);

        ArgumentCaptor<PortfolioTradeBatchReqVO> captor = ArgumentCaptor.forClass(PortfolioTradeBatchReqVO.class);
        verify(portfolioService).saveTrades(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo("EASYTRADER");
        assertThat(captor.getValue().getAccountId()).isEqualTo(1L);
        assertThat(captor.getValue().getTrades()).hasSize(1);
    }

    @Test
    void routesToEastmoneyWebProviderByAccountSyncMode() {
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 9L))
                .thenReturn(Optional.of(account("EM_WEB")));
        when(emWebProvider.fetchTrades()).thenReturn(List.of(new PortfolioTradeSaveReqVO()));

        service().syncTrades(1L, 9L);

        verify(portfolioService).saveTrades(any(PortfolioTradeBatchReqVO.class));
        verify(emWebProvider).fetchTrades();
    }

    @Test
    void rejectsManualAccountsWithFileImportHint() {
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 9L))
                .thenReturn(Optional.of(account("MANUAL")));

        assertThatThrownBy(() -> service().syncTrades(1L, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手动导入模式");
    }

    @Test
    void rejectsUnknownSyncMode() {
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 9L))
                .thenReturn(Optional.of(account("UNKNOWN")));

        assertThatThrownBy(() -> service().syncTrades(1L, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("暂不支持该账户同步方式");
    }

    @Test
    void rejectsEmptyTradesFromChannel() {
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 9L))
                .thenReturn(Optional.of(account("EASYTRADER")));
        when(easytraderProvider.fetchTrades()).thenReturn(List.of());

        assertThatThrownBy(() -> service().syncTrades(1L, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未返回任何成交");
    }

    @Test
    void positionsRouteByAccountSyncMode() {
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 9L))
                .thenReturn(Optional.of(account("EM_WEB")));
        BrokerPositionSnapshotVO snapshot = new BrokerPositionSnapshotVO();
        snapshot.setChannel("EM_WEB");
        when(emWebProvider.fetchPositions()).thenReturn(snapshot);
        when(portfolioService.getBrokerSnapshot(1L)).thenReturn(snapshot);

        assertThat(service().positions(1L, 9L)).isSameAs(snapshot);
    }

    @Test
    void saveTradesResultIsReturnedToCaller() {
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 9L))
                .thenReturn(Optional.of(account("EASYTRADER")));
        when(easytraderProvider.fetchTrades()).thenReturn(List.of(new PortfolioTradeSaveReqVO()));
        when(portfolioService.saveTrades(any(PortfolioTradeBatchReqVO.class)))
                .thenReturn(new PortfolioImportResultVO(10L, 1, 1, 0));

        assertThat(service().syncTrades(1L, 9L).getSuccessCount()).isEqualTo(1);
    }

    @Test
    void cookieUpdateTriggerSyncsOnlyEmWebAccounts() {
        UserBrokerAccount emWeb = account("EM_WEB");
        UserBrokerAccount easytrader = account("EASYTRADER");
        UserBrokerAccount manual = account("MANUAL");
        when(accountRepository.findAllByUserIdAndDeletedFalse(9L)).thenReturn(List.of(emWeb, easytrader, manual));
        when(emWebProvider.fetchTrades()).thenReturn(List.of(new PortfolioTradeSaveReqVO()));

        service().triggerSynchronizeAfterCookieUpdate(9L, "uitest");

        // 仅 EM_WEB 账户参与 Cookie 驱动的自动同步，EASYTRADER/MANUAL 不触发
        verify(emWebProvider).fetchTrades();
        verify(easytraderProvider, org.mockito.Mockito.never()).fetchTrades();
        // 成交入库 + 持仓快照落库（券商渠道持仓以接口为准）
        verify(portfolioService).saveTrades(any(PortfolioTradeBatchReqVO.class));
        verify(portfolioService).replacePositionsFromBroker(eq(1L), eq(9L), any());
    }

    @Test
    void cookieUpdateTriggerSurvivesSingleAccountFailure() {
        UserBrokerAccount failing = account("EM_WEB");
        UserBrokerAccount healthy = account("EM_WEB");
        when(accountRepository.findAllByUserIdAndDeletedFalse(9L)).thenReturn(List.of(failing, healthy));
        when(emWebProvider.fetchTrades())
                .thenThrow(new RuntimeException("会话过期"))
                .thenReturn(List.of(new PortfolioTradeSaveReqVO()));

        service().triggerSynchronizeAfterCookieUpdate(9L, "uitest");

        // 第一个账户失败（会话过期等）不影响第二个账户继续同步
        verify(portfolioService, org.mockito.Mockito.times(1)).saveTrades(any(PortfolioTradeBatchReqVO.class));
    }

    @Test
    void cookieUpdateTriggerSkipsWhenNoEmWebAccount() {
        when(accountRepository.findAllByUserIdAndDeletedFalse(9L)).thenReturn(List.of(account("MANUAL")));

        service().triggerSynchronizeAfterCookieUpdate(9L, "uitest");

        org.mockito.Mockito.verifyNoInteractions(emWebProvider, portfolioService);
    }
}
