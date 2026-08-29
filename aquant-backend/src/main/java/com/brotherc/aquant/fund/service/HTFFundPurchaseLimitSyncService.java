package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.common.constant.HTFFundConstant;
import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.utils.DigestUtils;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.fund.entity.StockFundAnnouncementSync;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitAnnouncement;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitAnnouncementDetail;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.fund.repository.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.integration.htf.model.HTFFundAnnouncement;
import com.brotherc.aquant.integration.htf.service.HTFFundService;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HTFFundPurchaseLimitSyncService implements FundPurchaseLimitSyncService {

    private static final List<String> TARGET_FUND_CODES = List.of(
            "018966", "018967", "018969", "018968", "021773"
    );

    private final HTFFundService htfFundService;
    private final HTFFundAnnouncementParser htfFundAnnouncementParser;
    private final StockFundPurchaseLimitService stockFundPurchaseLimitService;
    private final StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;
    private final StockSyncRepository stockSyncRepository;

    @Override
    public String getSourceName() {
        return HTFFundConstant.SOURCE_NAME;
    }

    /**
     * 每天检查汇添富纳斯达克100产品公告，仅在出现新公告或上次处理失败时下载附件。
     */
    @Override
    public void sync(LocalDateTime syncTime) {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_HTF_FUND_PURCHASE_LIMIT_LATEST);
        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        if (lastTimestamp != null) {
            LocalDate lastSyncDate = Instant.ofEpochMilli(lastTimestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            if (lastSyncDate.equals(syncTime.toLocalDate())) {
                log.info("汇添富基金官方额度当天已同步，跳过本次同步，syncDate={}", lastSyncDate);
                return;
            }
        }

        HTFFundAnnouncement announcement;
        try {
            announcement = htfFundService.getLatestNasdaq100LimitAnnouncement();
        } catch (Exception e) {
            log.error("获取汇添富基金官方额度公告失败，本次不更新同步水位", e);
            return;
        }
        StockFundAnnouncementSync existing = stockFundAnnouncementSyncRepository
                .findBySourceAndAnnouncementId(HTFFundConstant.SOURCE, announcement.getAnnouncementId())
                .orElse(null);
        boolean currentRulesComplete = TARGET_FUND_CODES.stream().allMatch(code ->
                stockFundPurchaseLimitService.hasCurrentPurchaseLimit(HTFFundConstant.SOURCE, code));
        boolean alreadyProcessed = existing != null
                && FundPurchaseLimitConstant.SYNC_SUCCESS.equals(existing.getStatus()) && currentRulesComplete;
        boolean waitingForEffectiveDate = existing != null
                && FundPurchaseLimitConstant.SYNC_PENDING.equals(existing.getStatus())
                && existing.getRetryAfterDate() != null
                && existing.getRetryAfterDate().isAfter(syncTime.toLocalDate());
        if (alreadyProcessed || waitingForEffectiveDate) {
            saveSyncWatermark(stockSync, syncTime);
            log.info("汇添富基金额度公告无须重复处理，announcementId={}, status={}",
                    announcement.getAnnouncementId(), existing.getStatus());
            return;
        }

        FundPurchaseLimitAnnouncement sourceAnnouncement = toSourceAnnouncement(announcement);
        FundPurchaseLimitAnnouncementDetail detail = new FundPurchaseLimitAnnouncementDetail();
        detail.setDetailUrl(announcement.getDetailUrl());
        detail.setAttachmentUrl(announcement.getAttachmentUrl());
        detail.setAttachmentName(announcement.getAnnouncementId() + ".pdf");
        try {
            byte[] attachment = htfFundService.downloadAnnouncement(announcement.getAttachmentUrl());
            List<FundPurchaseLimitRule> rules = htfFundAnnouncementParser.parse(announcement.getTitle(), attachment);
            LocalDate effectiveDate = rules.stream().map(FundPurchaseLimitRule::getEffectiveDate)
                    .filter(date -> date != null).min(LocalDate::compareTo)
                    .orElseThrow(() -> new IllegalStateException("汇添富基金额度规则缺少生效日期"));
            String attachmentHash = DigestUtils.sha256(attachment);
            if (effectiveDate.isAfter(syncTime.toLocalDate())) {
                stockFundPurchaseLimitService.savePending(
                        HTFFundConstant.SOURCE, sourceAnnouncement, detail, attachmentHash, effectiveDate
                );
            } else {
                stockFundPurchaseLimitService.saveSuccess(
                        HTFFundConstant.SOURCE, HTFFundConstant.SOURCE_NAME,
                        sourceAnnouncement, detail, attachmentHash, rules
                );
            }
            saveSyncWatermark(stockSync, syncTime);
            log.info("同步汇添富基金官方额度完成，announcementId={}, targetFundCount={}, ruleCount={}",
                    announcement.getAnnouncementId(), TARGET_FUND_CODES.size(), rules.size());
        } catch (Exception e) {
            stockFundPurchaseLimitService.saveFailed(HTFFundConstant.SOURCE, sourceAnnouncement, detail, e);
            log.error("处理汇添富基金官方额度公告失败，本次不更新同步水位，announcementId={}",
                    announcement.getAnnouncementId(), e);
        }
    }

    private FundPurchaseLimitAnnouncement toSourceAnnouncement(HTFFundAnnouncement source) {
        FundPurchaseLimitAnnouncement announcement = new FundPurchaseLimitAnnouncement();
        announcement.setAnnouncementId(source.getAnnouncementId());
        announcement.setAnnouncementDate(source.getAnnouncementDate());
        announcement.setTitle(source.getTitle());
        return announcement;
    }

    private void saveSyncWatermark(StockSync stockSync, LocalDateTime syncTime) {
        StockSync target = stockSync;
        if (target == null) {
            target = new StockSync();
            target.setName(StockSyncConstant.STOCK_HTF_FUND_PURCHASE_LIMIT_LATEST);
        }
        target.setValue(String.valueOf(syncTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        stockSyncRepository.save(target);
    }

}
