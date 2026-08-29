package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.common.constant.ChinaAMCFundConstant;
import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.integration.chinaamc.model.ChinaAMCFundPurchaseLimit;
import com.brotherc.aquant.integration.chinaamc.service.ChinaAMCFundService;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChinaAMCFundPurchaseLimitSyncService implements FundPurchaseLimitSyncService {

    private final ChinaAMCFundService chinaAMCFundService;
    private final StockFundPurchaseLimitService stockFundPurchaseLimitService;
    private final StockSyncRepository stockSyncRepository;

    @Override
    public String getSourceName() {
        return ChinaAMCFundConstant.SOURCE_NAME;
    }

    /**
     * 每天同步一次华夏官网当前开放状态中的纳斯达克100联接基金申购、定投规则。
     */
    @Override
    public void sync(LocalDateTime syncTime) {
        StockSync stockSync = stockSyncRepository.findByName(
                StockSyncConstant.STOCK_CHINA_AMC_FUND_PURCHASE_LIMIT_LATEST
        );
        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        if (lastTimestamp != null) {
            LocalDate lastSyncDate = Instant.ofEpochMilli(lastTimestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            if (lastSyncDate.equals(syncTime.toLocalDate())) {
                log.info("华夏基金官方额度当天已同步，跳过本次同步，syncDate={}", lastSyncDate);
                return;
            }
        }

        List<ChinaAMCFundPurchaseLimit> limits;
        try {
            limits = chinaAMCFundService.getNasdaq100PurchaseLimits();
        } catch (Exception e) {
            log.error("获取华夏基金官方额度失败，本次不更新额度和同步水位", e);
            return;
        }
        List<FundPurchaseLimitRule> rules = new ArrayList<>();
        for (ChinaAMCFundPurchaseLimit limit : limits) {
            rules.add(createRule(limit, FundPurchaseLimitConstant.BUSINESS_PURCHASE,
                    limit.getPurchaseStatus(), limit.getPurchaseLimitAmount()));
            rules.add(createRule(limit, FundPurchaseLimitConstant.BUSINESS_RECURRING,
                    limit.getRecurringStatus(), limit.getRecurringLimitAmount()));
        }
        stockFundPurchaseLimitService.saveCurrentRules(
                ChinaAMCFundConstant.SOURCE, ChinaAMCFundConstant.SOURCE_NAME, rules
        );

        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName(StockSyncConstant.STOCK_CHINA_AMC_FUND_PURCHASE_LIMIT_LATEST);
        }
        stockSync.setValue(String.valueOf(syncTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        stockSyncRepository.save(stockSync);
        log.info("同步华夏基金官方额度完成，targetFundCount={}, ruleCount={}", limits.size(), rules.size());
    }

    private FundPurchaseLimitRule createRule(
            ChinaAMCFundPurchaseLimit limit, String businessType, String status, BigDecimal amount
    ) {
        FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
        rule.setFundCode(limit.getFundCode());
        rule.setCurrency(limit.getCurrency());
        rule.setSalesChannel(limit.getSalesChannel());
        rule.setBusinessType(businessType);
        rule.setStatus(status);
        rule.setLimitAmount(FundPurchaseLimitConstant.STATUS_LIMITED.equals(status) ? amount : null);
        return rule;
    }

}
