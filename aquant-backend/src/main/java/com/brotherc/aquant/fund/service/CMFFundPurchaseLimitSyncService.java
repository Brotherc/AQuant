package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.common.constant.CMFFundConstant;
import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.fund.entity.StockFundAnnouncementSync;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitAnnouncement;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitAnnouncementDetail;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.fund.repository.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.integration.cmf.model.CMFFundPurchaseLimit;
import com.brotherc.aquant.integration.cmf.service.CMFFundService;
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
public class CMFFundPurchaseLimitSyncService implements FundPurchaseLimitSyncService {

    private static final List<String> TARGET_FUND_CODES = List.of("019547", "019548");

    private final CMFFundService cmfFundService;
    private final StockFundPurchaseLimitService stockFundPurchaseLimitService;
    private final StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;
    private final StockSyncRepository stockSyncRepository;

    @Override
    public String getSourceName() {
        return CMFFundConstant.SOURCE_NAME;
    }

    /**
     * 每天检查招商纳斯达克100产品页，仅在出现新公告或上次处理失败时重新解析正文。
     */
    @Override
    public void sync(LocalDateTime syncTime) {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_CMF_FUND_PURCHASE_LIMIT_LATEST);
        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        if (lastTimestamp != null) {
            LocalDate lastSyncDate = Instant.ofEpochMilli(lastTimestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            if (lastSyncDate.equals(syncTime.toLocalDate())) {
                log.info("招商基金官方额度当天已同步，跳过本次同步，syncDate={}", lastSyncDate);
                return;
            }
        }

        CMFFundPurchaseLimit announcement;
        try {
            announcement = cmfFundService.getLatestNasdaq100DirectLimitAnnouncement();
        } catch (Exception e) {
            log.error("获取招商基金官方额度公告失败，本次不更新同步水位", e);
            return;
        }

        StockFundAnnouncementSync existing = stockFundAnnouncementSyncRepository
                .findBySourceAndAnnouncementId(CMFFundConstant.SOURCE, announcement.getAnnouncementId())
                .orElse(null);
        boolean currentRulesComplete = TARGET_FUND_CODES.stream().allMatch(code ->
                stockFundPurchaseLimitService.hasCurrentPurchaseLimit(CMFFundConstant.SOURCE, code));
        boolean alreadyProcessed = existing != null
                && FundPurchaseLimitConstant.SYNC_SUCCESS.equals(existing.getStatus()) && currentRulesComplete;
        boolean waitingForEffectiveDate = existing != null
                && FundPurchaseLimitConstant.SYNC_PENDING.equals(existing.getStatus())
                && existing.getRetryAfterDate() != null
                && existing.getRetryAfterDate().isAfter(syncTime.toLocalDate());
        if (alreadyProcessed || waitingForEffectiveDate) {
            saveSyncWatermark(stockSync, syncTime);
            log.info("招商基金额度公告无须重复处理，announcementId={}, status={}",
                    announcement.getAnnouncementId(), existing.getStatus());
            return;
        }

        FundPurchaseLimitAnnouncement sourceAnnouncement = toSourceAnnouncement(announcement);
        FundPurchaseLimitAnnouncementDetail detail = new FundPurchaseLimitAnnouncementDetail();
        detail.setDetailUrl(announcement.getDetailUrl());
        try {
            CMFFundPurchaseLimit limit = cmfFundService.getNasdaq100DirectPurchaseLimit(announcement);
            List<FundPurchaseLimitRule> rules = createRules(limit);
            if (limit.getEffectiveDate().isAfter(syncTime.toLocalDate())) {
                stockFundPurchaseLimitService.savePending(
                        CMFFundConstant.SOURCE, sourceAnnouncement, detail, null, limit.getEffectiveDate()
                );
            } else {
                stockFundPurchaseLimitService.saveSuccess(
                        CMFFundConstant.SOURCE, CMFFundConstant.SOURCE_NAME,
                        sourceAnnouncement, detail, null, rules
                );
            }
            saveSyncWatermark(stockSync, syncTime);
            log.info("同步招商基金官方额度完成，announcementId={}, targetFundCount={}, ruleCount={}",
                    announcement.getAnnouncementId(), TARGET_FUND_CODES.size(), rules.size());
        } catch (Exception e) {
            stockFundPurchaseLimitService.saveFailed(CMFFundConstant.SOURCE, sourceAnnouncement, detail, e);
            log.error("处理招商基金官方额度公告失败，本次不更新同步水位，announcementId={}",
                    announcement.getAnnouncementId(), e);
        }
    }

    private List<FundPurchaseLimitRule> createRules(CMFFundPurchaseLimit limit) {
        List<FundPurchaseLimitRule> rules = new ArrayList<>();
        for (String fundCode : TARGET_FUND_CODES) {
            rules.add(createRule(fundCode, FundPurchaseLimitConstant.BUSINESS_PURCHASE,
                    limit.getPurchaseStatus(), limit.getPurchaseLimitAmount(), limit.getEffectiveDate()));
            if (limit.getRecurringStatus() != null) {
                rules.add(createRule(fundCode, FundPurchaseLimitConstant.BUSINESS_RECURRING,
                        limit.getRecurringStatus(), limit.getRecurringLimitAmount(), limit.getEffectiveDate()));
            }
        }
        return rules;
    }

    private FundPurchaseLimitRule createRule(
            String fundCode, String businessType, String status, BigDecimal amount, LocalDate effectiveDate
    ) {
        FundPurchaseLimitRule rule = new FundPurchaseLimitRule();
        rule.setFundCode(fundCode);
        rule.setCurrency("CNY");
        rule.setSalesChannel(FundPurchaseLimitConstant.CHANNEL_DIRECT);
        rule.setBusinessType(businessType);
        rule.setStatus(status);
        rule.setLimitAmount(FundPurchaseLimitConstant.STATUS_LIMITED.equals(status) ? amount : null);
        rule.setEffectiveDate(effectiveDate);
        return rule;
    }

    private FundPurchaseLimitAnnouncement toSourceAnnouncement(CMFFundPurchaseLimit limit) {
        FundPurchaseLimitAnnouncement announcement = new FundPurchaseLimitAnnouncement();
        announcement.setAnnouncementId(limit.getAnnouncementId());
        announcement.setAnnouncementDate(limit.getAnnouncementDate());
        announcement.setTitle(limit.getTitle());
        return announcement;
    }

    private void saveSyncWatermark(StockSync stockSync, LocalDateTime syncTime) {
        StockSync target = stockSync;
        if (target == null) {
            target = new StockSync();
            target.setName(StockSyncConstant.STOCK_CMF_FUND_PURCHASE_LIMIT_LATEST);
        }
        target.setValue(String.valueOf(syncTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        stockSyncRepository.save(target);
    }

}
