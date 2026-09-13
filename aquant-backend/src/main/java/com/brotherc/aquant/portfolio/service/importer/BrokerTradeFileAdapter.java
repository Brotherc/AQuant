package com.brotherc.aquant.portfolio.service.importer;

import com.brotherc.aquant.portfolio.model.dto.PortfolioTradeFileParseResult;

/**
 * 券商原始交易文件适配器，将不同券商格式统一转换为 AQuant 标准流水。
 */
public interface BrokerTradeFileAdapter {

    boolean supports(String brokerCode, String fileName, byte[] content);

    PortfolioTradeFileParseResult parse(String fileName, byte[] content);

}
