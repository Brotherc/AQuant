package com.brotherc.aquant.fund.service;

import java.time.LocalDateTime;

/**
 * 基金管理人官方渠道额度同步接口，每家基金公司提供一个实现。
 */
public interface FundPurchaseLimitSyncService {

    String getSourceName();

    void sync(LocalDateTime syncTime);

}
