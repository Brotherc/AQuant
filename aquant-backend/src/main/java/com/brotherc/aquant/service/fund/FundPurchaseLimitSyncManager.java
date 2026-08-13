package com.brotherc.aquant.service.fund;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundPurchaseLimitSyncManager {

    private final List<FundPurchaseLimitSyncService> syncServices;

    public void sync(LocalDateTime syncTime) {
        for (FundPurchaseLimitSyncService syncService : syncServices) {
            try {
                syncService.sync(syncTime);
            } catch (Exception e) {
                log.error("同步基金官方渠道额度失败，source={}", syncService.getSourceName(), e);
            }
        }
    }

}
