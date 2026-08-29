package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.common.constant.JSFundConstant;
import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.integration.js.model.JSFundPurchaseLimit;
import com.brotherc.aquant.integration.js.service.JSFundService;
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
public class JSFundPurchaseLimitSyncService implements FundPurchaseLimitSyncService {

    private final JSFundService jsFundService;
    private final StockFundPurchaseLimitService stockFundPurchaseLimitService;
    private final StockSyncRepository stockSyncRepository;

    @Override
    public String getSourceName() {
        return JSFundConstant.SOURCE_NAME;
    }

    /**
     * 每天同步一次嘉实官网当前额度表中的纳斯达克100联接基金申购、定投状态。
     */
    @Override
    public void sync(LocalDateTime syncTime) {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_JS_FUND_PURCHASE_LIMIT_LATEST);
        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        if (lastTimestamp != null) {
            LocalDate lastSyncDate = Instant.ofEpochMilli(lastTimestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            if (lastSyncDate.equals(syncTime.toLocalDate())) {
                log.info("嘉实基金官方额度当天已同步，跳过本次同步，syncDate={}", lastSyncDate);
                return;
            }
        }

        List<JSFundPurchaseLimit> limits;
        try {
            limits = jsFundService.getNasdaq100PurchaseLimits();
        } catch (Exception e) {
            log.error("获取嘉实基金官方额度失败，本次不更新额度和同步水位", e);
            return;
        }
        List<FundPurchaseLimitRule> rules = new ArrayList<>();
        for (JSFundPurchaseLimit limit : limits) {
            rules.add(createRule(limit, FundPurchaseLimitConstant.BUSINESS_PURCHASE,
                    limit.getPurchaseStatus(), limit.getPurchaseLimitAmount()));
            rules.add(createRule(limit, FundPurchaseLimitConstant.BUSINESS_RECURRING,
                    limit.getRecurringStatus(), limit.getRecurringLimitAmount()));
        }
        stockFundPurchaseLimitService.saveCurrentRules(
                JSFundConstant.SOURCE, JSFundConstant.SOURCE_NAME, rules
        );

        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName(StockSyncConstant.STOCK_JS_FUND_PURCHASE_LIMIT_LATEST);
        }
        stockSync.setValue(String.valueOf(syncTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        stockSyncRepository.save(stockSync);
        log.info("同步嘉实基金官方额度完成，targetFundCount={}, ruleCount={}", limits.size(), rules.size());
    }

    private FundPurchaseLimitRule createRule(
            JSFundPurchaseLimit limit, String businessType, String status, BigDecimal amount
    ) {
        FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
        rule.setFundCode(limit.getFundCode());
        rule.setCurrency(limit.getCurrency());
        rule.setSalesChannel(limit.getSalesChannel());
        rule.setBusinessType(businessType);
        rule.setStatus(status);
        rule.setLimitAmount(FundPurchaseLimitConstant.STATUS_LIMITED.equals(status) ? amount : null);
        rule.setEffectiveDate(limit.getEffectiveDate());
        return rule;
    }

}
