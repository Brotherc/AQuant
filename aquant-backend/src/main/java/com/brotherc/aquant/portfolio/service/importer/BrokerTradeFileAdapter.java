package com.brotherc.aquant.portfolio.service.importer;

import com.brotherc.aquant.portfolio.model.dto.PortfolioTradeFileParseResult;

/**
 * 券商原始交易文件适配器，将不同券商格式统一转换为 AQuant 标准流水。
 */
public interface BrokerTradeFileAdapter {

    /** 适配器服务的券商代码（与 UserBrokerAccount.brokerCode 同一取值域），用于按账户路由优先匹配 */
    default String brokerCode() {
        return "";
    }

    boolean supports(String brokerCode, String fileName, byte[] content);

    PortfolioTradeFileParseResult parse(String fileName, byte[] content);

}
