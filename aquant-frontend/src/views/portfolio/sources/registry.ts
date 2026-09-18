import { defineAsyncComponent, type Component } from 'vue';

/**
 * 持仓数据源 = 券商。不同券商接口提供的统计指标不一致（东方财富证券有摊薄成本价/盈亏比例/
 * 保本价，其他券商可能没有），因此每个券商拥有独立的视图组件，选择数据源即动态加载对应页面。
 *
 * 「本地计算」不是券商，而是 AQuant 按导入流水重算的口径，用于无接口的账户（文件导入/手动录入）。
 *
 * 关于接入通道：同一券商可能有多种取数方式（如东方财富证券支持网页交易会话 EM_WEB 与
 * easytrader 客户端自动化 EASYTRADER），它们属于同一数据源的实现细节，由账户的 syncMode
 * 决定走哪条通道；接口返回的 channel 字段用于提示当前数据来自哪条通道，视图按实际可用字段渲染。
 *
 * 新增券商只需两步：
 *   1. 在本目录新增 XxxPositionView.vue（自行决定展示哪些指标、如何取数）；
 *   2. 在下方 PORTFOLIO_SOURCES 增加一行注册项。
 */
export type PortfolioDataSource = 'LOCAL' | 'EM';

export interface PortfolioSourceMeta {
    key: PortfolioDataSource;
    label: string;
    /** 该券商对应的账户 brokerCode 取值 */
    brokerCodes: string[];
    /** 一句话说明该源的数据口径，展示在选择器 title 中 */
    description: string;
    /** 该券商的专属视图组件；LOCAL 复用持仓页现有页签，无需组件 */
    view?: Component;
}

/** 本地计算源：复用持仓页现有页签（总览/交易流水/账户与资金/数据导入） */
export const LOCAL_SOURCE: PortfolioSourceMeta = {
    key: 'LOCAL',
    label: '本地计算',
    brokerCodes: [],
    description: 'AQuant 按导入的交易流水重算持仓与收益，适用于手动录入或文件导入的账户'
};

export const PORTFOLIO_SOURCES: PortfolioSourceMeta[] = [
    LOCAL_SOURCE,
    {
        key: 'EM',
        label: '东方财富证券',
        brokerCodes: ['EM', 'EASTMONEY'],
        description: '直取东方财富证券接口口径的资产与持仓指标（摊薄成本价、盈亏比例、保本价等）',
        view: defineAsyncComponent(() => import('./EastmoneyPositionView.vue'))
    }
];

export const findSourceMeta = (key: PortfolioDataSource): PortfolioSourceMeta =>
    PORTFOLIO_SOURCES.find((source) => source.key === key) ?? LOCAL_SOURCE;

/** 按账户券商代码反查数据源，用于提示当前账户属于哪个源 */
export const findSourceByBrokerCode = (brokerCode?: string | null): PortfolioSourceMeta | undefined => {
    if (!brokerCode) return undefined;
    const normalized = brokerCode.toUpperCase();
    return PORTFOLIO_SOURCES.find((source) => source.brokerCodes.includes(normalized));
};
