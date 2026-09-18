package com.brotherc.aquant.portfolio.service.sync;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.utils.UserContext;
import com.brotherc.aquant.portfolio.entity.UserBrokerAccount;
import com.brotherc.aquant.portfolio.model.vo.BrokerPositionSnapshotVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioImportResultVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeBatchReqVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;
import com.brotherc.aquant.portfolio.repository.UserBrokerAccountRepository;
import com.brotherc.aquant.portfolio.service.UserPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 券商账户持仓/成交的自动同步入口：按 {@code UserBrokerAccount.syncMode}
 * 把账户路由到对应的数据渠道（多数据源切换）。
 *
 * <ul>
 *   <li>MANUAL —— 手动导入交割单文件，不走本服务</li>
 *   <li>EASYTRADER —— easytrader 客户端自动化（Windows 常驻）</li>
 *   <li>EM_WEB —— 东方财富证券网页交易会话（Cookie + validatekey）</li>
 * </ul>
 *
 * <p>每次同步按渠道拉取全量「当日成交」（多证券），经 saveTrades 的
 * dedupKey 幂等去重后落库，重复触发安全。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioBrokerSyncService {

    private final UserBrokerAccountRepository accountRepository;
    private final UserPortfolioService portfolioService;
    private final List<PortfolioBrokerSyncProvider> providers;

    /** 按账户同步方式拉取最近成交并入库 */
    public PortfolioImportResultVO syncTrades(Long accountId) {
        return syncTrades(accountId, UserContext.requireCurrentUserId());
    }

    PortfolioImportResultVO syncTrades(Long accountId, Long userId) {
        UserBrokerAccount account = validateAccount(accountId, userId);
        return syncAccount(account);
    }

    /**
     * 查询券商持仓快照：读最近一次成功同步的持久化数据（Cookie 失效后仍可展示），
     * 实时刷新走 {@link #refreshPositions(Long)}。
     */
    public BrokerPositionSnapshotVO positions(Long accountId) {
        return positions(accountId, UserContext.requireCurrentUserId());
    }

    BrokerPositionSnapshotVO positions(Long accountId, Long userId) {
        UserBrokerAccount account = validateAccount(accountId, userId);
        return portfolioService.getBrokerSnapshot(account.getId());
    }

    /** 实时刷新券商持仓快照（需交易会话有效），成功后持久化并返回 */
    public BrokerPositionSnapshotVO refreshPositions(Long accountId) {
        Long userId = UserContext.requireCurrentUserId();
        UserBrokerAccount account = validateAccount(accountId, userId);
        PortfolioBrokerSyncProvider provider = route(account.getSyncMode());
        BrokerPositionSnapshotVO snapshot = provider.fetchPositions();
        portfolioService.replacePositionsFromBroker(account.getId(), userId, snapshot);
        portfolioService.saveBrokerSnapshot(account.getId(), snapshot);
        return snapshot;
    }

    /**
     * 交易 Cookie 更新后自动触发：同步当前用户所有 EM_WEB 渠道账户的最近成交
     * （成交入库后由 saveTrades 联动重建持仓与收益快照）。逐账户独立容错，
     * 单个渠道失败不影响其他账户。异步执行，Cookie 保存接口立即返回。
     */
    @Async
    public void triggerSynchronizeAfterCookieUpdate(Long userId, String username) {
        List<UserBrokerAccount> accounts = accountRepository.findAllByUserIdAndDeletedFalse(userId).stream()
                .filter(account -> "EM_WEB".equalsIgnoreCase(account.getSyncMode()))
                .toList();
        if (accounts.isEmpty()) {
            log.info("交易 Cookie 已更新，但当前用户没有 EM_WEB 渠道的券商账户，跳过自动同步");
            return;
        }
        // @Async 线程没有 HTTP 请求上下文（UserContext 为 ThreadLocal），
        // saveTrades 内部的 requireCurrentUserId 需要它，必须显式重建；结束后清理防泄漏
        UserContext.set(userId, username);
        try {
            for (UserBrokerAccount account : accounts) {
                try {
                    PortfolioImportResultVO result = syncAccount(account);
                    log.info("交易 Cookie 更新后自动同步完成: accountId={}, 总数={}, 成功={}, 跳过={}",
                            account.getId(), result.getTotalCount(), result.getSuccessCount(), result.getSkipCount());
                } catch (RuntimeException exception) {
                    log.warn("交易 Cookie 更新后自动同步失败（不影响其他账户）: accountId={}, 原因={}",
                            account.getId(), exception.getMessage());
                }
            }
        } finally {
            UserContext.clear();
        }
    }

    /** 单账户同步核心：按 syncMode 路由渠道，拉取成交与持仓快照落库 */
    private PortfolioImportResultVO syncAccount(UserBrokerAccount account) {
        PortfolioBrokerSyncProvider provider = route(account.getSyncMode());
        List<PortfolioTradeSaveReqVO> trades = provider.fetchTrades();
        if (trades.isEmpty()) {
            throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                    provider.channel() + " 渠道未返回任何成交（当日无交易或客户端/会话未就绪）");
        }
        PortfolioTradeBatchReqVO request = new PortfolioTradeBatchReqVO();
        request.setAccountId(account.getId());
        request.setSource(provider.channel());
        request.setTrades(trades);
        PortfolioImportResultVO result = portfolioService.saveTrades(request);
        // 券商渠道持仓以接口快照为准（成交流水只有增量，反推会把历史持仓算丢）；
        // 快照失败不影响成交入库，持仓维持原值，下次同步重试
        try {
            BrokerPositionSnapshotVO snapshot = provider.fetchPositions();
            portfolioService.replacePositionsFromBroker(account.getId(), account.getUserId(), snapshot);
        } catch (RuntimeException exception) {
            log.warn("券商持仓快照同步失败（成交已入库）：accountId={}, 原因={}",
                    account.getId(), exception.getMessage());
        }
        return result;
    }

    private PortfolioBrokerSyncProvider route(String syncMode) {
        String mode = StringUtils.defaultIfBlank(syncMode, "MANUAL").toUpperCase();
        if ("MANUAL".equals(mode)) {
            throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                    "该账户为手动导入模式（MANUAL），请使用交割单文件导入");
        }
        return providers.stream()
                .filter(provider -> provider.channel().equalsIgnoreCase(mode))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                        "暂不支持该账户同步方式：" + mode + "（可用：" + providers.stream()
                                .map(PortfolioBrokerSyncProvider::channel).toList() + "）"));
    }

    private UserBrokerAccount validateAccount(Long accountId, Long userId) {
        if (accountId == null) {
            throw ExceptionEnum.PORTFOLIO_ACCOUNT_NOT_FOUND.toException();
        }
        return accountRepository.findByIdAndUserIdAndDeletedFalse(accountId, userId)
                .orElseThrow(ExceptionEnum.PORTFOLIO_ACCOUNT_NOT_FOUND::toException);
    }
}
