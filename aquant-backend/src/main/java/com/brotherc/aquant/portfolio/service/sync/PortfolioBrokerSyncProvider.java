package com.brotherc.aquant.portfolio.service.sync;

import com.brotherc.aquant.portfolio.model.vo.BrokerPositionSnapshotVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;

import java.util.List;

/**
 * 券商持仓/成交数据的自动同步渠道。每种渠道对应 {@code UserBrokerAccount.syncMode}
 * 的一个取值（如 EASYTRADER 客户端自动化、EM_WEB 网页交易会话），
 * 由 PortfolioBrokerSyncService 按账户配置路由，文件导入（MANUAL）不在此列。
 *
 * <p>所有实现返回的都是多证券数据：一次同步覆盖账户下全部持仓/当日全部成交；
 * 持仓快照的资产与成本/盈亏指标直取券商接口原值，本地不推算。</p>
 */
public interface PortfolioBrokerSyncProvider {

    /** 渠道标识，与 UserBrokerAccount.syncMode 的取值对应 */
    String channel();

    /** 拉取最近成交并转换为标准流水（多证券） */
    List<PortfolioTradeSaveReqVO> fetchTrades();

    /** 拉取券商实时持仓快照（多证券，指标直取券商口径） */
    BrokerPositionSnapshotVO fetchPositions();

}
