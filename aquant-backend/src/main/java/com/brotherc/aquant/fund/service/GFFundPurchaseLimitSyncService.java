package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.common.constant.GFFundConstant;
import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.integration.gf.model.GFFundPurchaseLimit;
import com.brotherc.aquant.integration.gf.service.GFFundService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GFFundPurchaseLimitSyncService implements FundPurchaseLimitSyncService {

    private static final Map<String, String> TARGET_FUND_CURRENCIES = createTargetFundCurrencies();

    private final GFFundService gfFundService;
    private final StockFundPurchaseLimitService stockFundPurchaseLimitService;
    private final StockSyncRepository stockSyncRepository;

    @Override
    public String getSourceName() {
        return GFFundConstant.SOURCE_NAME;
    }

    /**
     * 每天同步一次广发纳指100ETF联接各份额在广发官网展示的个人客户直销申购上限。
     */
    @Override
    public void sync(LocalDateTime syncTime) {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_GF_FUND_PURCHASE_LIMIT_LATEST);
        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        if (lastTimestamp != null) {
            LocalDate lastSyncDate = Instant.ofEpochMilli(lastTimestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            if (lastSyncDate.equals(syncTime.toLocalDate())) {
                log.info("广发基金官方额度当天已同步，跳过本次同步，syncDate={}", lastSyncDate);
                return;
            }
        }

        List<FundPurchaseLimitRule> rules = new ArrayList<>();
        try {
            for (Map.Entry<String, String> target : TARGET_FUND_CURRENCIES.entrySet()) {
                GFFundPurchaseLimit limit = gfFundService.getPersonalPurchaseLimit(target.getKey());
                FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
                rule.setFundCode(limit.getFundCode());
                rule.setSalesChannel(FundPurchaseLimitConstant.CHANNEL_DIRECT);
                rule.setBusinessType(FundPurchaseLimitConstant.BUSINESS_PURCHASE);
                BigDecimal maximum = limit.getMaximumPurchaseAmount();
                if (maximum == null) {
                    rule.setStatus(FundPurchaseLimitConstant.STATUS_OPEN);
                } else if (maximum.compareTo(BigDecimal.ZERO) > 0) {
                    rule.setStatus(FundPurchaseLimitConstant.STATUS_LIMITED);
                } else {
                    rule.setStatus(FundPurchaseLimitConstant.STATUS_SUSPENDED);
                }
                rule.setLimitAmount(maximum != null && maximum.compareTo(BigDecimal.ZERO) > 0 ? maximum : null);
                rule.setCurrency(target.getValue());
                rules.add(rule);
            }
        } catch (Exception e) {
            log.error("获取广发基金官方额度失败，本次不更新额度和同步水位", e);
            return;
        }

        stockFundPurchaseLimitService.saveCurrentRules(
                GFFundConstant.SOURCE, GFFundConstant.SOURCE_NAME, rules
        );
        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName(StockSyncConstant.STOCK_GF_FUND_PURCHASE_LIMIT_LATEST);
        }
        long timestamp = syncTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        stockSync.setValue(String.valueOf(timestamp));
        stockSyncRepository.save(stockSync);
        log.info("同步广发基金官方额度完成，targetFundCount={}", rules.size());
    }

    private static Map<String, String> createTargetFundCurrencies() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("270042", "CNY");
        result.put("006479", "CNY");
        result.put("021778", "CNY");
        result.put("000055", "USD");
        result.put("006480", "USD");
        return result;
    }

}
