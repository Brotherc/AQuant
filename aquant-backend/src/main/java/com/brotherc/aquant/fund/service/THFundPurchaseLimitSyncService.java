package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.constant.THFundConstant;
import com.brotherc.aquant.common.utils.DigestUtils;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.fund.entity.StockFundAnnouncementSync;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitAnnouncementDetail;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.fund.repository.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.integration.th.model.THFundAnnouncement;
import com.brotherc.aquant.integration.th.model.THFundAnnouncementPage;
import com.brotherc.aquant.integration.th.service.THFundService;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class THFundPurchaseLimitSyncService implements FundPurchaseLimitSyncService {

    private static final Set<String> TARGET_FUND_CODES = Set.of("018043", "018044", "022525");

    private final THFundService thFundService;
    private final THFundAnnouncementParser thFundAnnouncementParser;
    private final StockFundPurchaseLimitService stockFundPurchaseLimitService;
    private final StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;
    private final StockSyncRepository stockSyncRepository;

    @Override
    public String getSourceName() {
        return THFundConstant.SOURCE_NAME;
    }

    /**
     * 每天增量扫描天弘纳斯达克100指数基金的官方额度公告；首次取得三个份额当前规则后停止回溯。
     */
    @Override
    public void sync(LocalDateTime syncTime) {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_TH_FUND_PURCHASE_LIMIT_LATEST);
        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        LocalDate lastSyncDate = null;
        if (lastTimestamp != null) {
            lastSyncDate = Instant.ofEpochMilli(lastTimestamp).atZone(ZoneId.systemDefault()).toLocalDate();
            if (lastSyncDate.equals(syncTime.toLocalDate())) {
                log.info("天弘基金官方额度当天已同步，跳过本次同步，syncDate={}", lastSyncDate);
                return;
            }
        }

        boolean allSuccess = retryPendingAnnouncements(syncTime.toLocalDate());
        LocalDate latestProcessedDate = getLatestProcessedAnnouncementDate();
        boolean baselineCompleted = hasCompleteCurrentRules();
        boolean baselineInitiallyCompleted = baselineCompleted;
        LocalDate announcementStartDate = baselineCompleted
                ? (lastSyncDate != null ? lastSyncDate : latestProcessedDate) : null;
        int page = 1;
        boolean scanning = true;
        while (scanning) {
            THFundAnnouncementPage announcementPage;
            try {
                announcementPage = thFundService.getNasdaq100Announcements(page);
            } catch (Exception e) {
                allSuccess = false;
                log.error("获取天弘基金公告列表失败，page={}", page, e);
                break;
            }
            List<THFundAnnouncement> relevantAnnouncements = announcementPage.getContent().stream()
                    .filter(this::isPurchaseLimitAnnouncement)
                    .filter(announcement -> announcementStartDate == null
                            || !announcement.getAnnouncementDate().isBefore(announcementStartDate))
                    .sorted(Comparator.comparing(THFundAnnouncement::getAnnouncementDate).reversed())
                    .toList();
            Map<String, StockFundAnnouncementSync> existingMap = loadExisting(relevantAnnouncements);
            for (THFundAnnouncement announcement : relevantAnnouncements) {
                if (baselineInitiallyCompleted || scanning) {
                    StockFundAnnouncementSync existing = existingMap.get(announcement.getAnnouncementId());
                    boolean alreadyProcessed = baselineCompleted && existing != null
                            && (FundPurchaseLimitConstant.SYNC_SUCCESS.equals(existing.getStatus())
                            || FundPurchaseLimitConstant.SYNC_IGNORED.equals(existing.getStatus()));
                    boolean waitingForEffectiveDate = existing != null
                            && FundPurchaseLimitConstant.SYNC_PENDING.equals(existing.getStatus())
                            && existing.getRetryAfterDate() != null
                            && existing.getRetryAfterDate().isAfter(syncTime.toLocalDate());
                    if (!alreadyProcessed && !waitingForEffectiveDate
                            && !processAnnouncement(announcement, syncTime.toLocalDate())) {
                        allSuccess = false;
                    }
                    if (!baselineCompleted && hasCompleteCurrentRules()) {
                        baselineCompleted = true;
                        scanning = false;
                    }
                }
            }
            boolean hasNextPage = page < announcementPage.getTotalPages()
                    && containsNewerAnnouncement(announcementPage, announcementStartDate);
            if (scanning && hasNextPage) {
                page++;
            } else {
                scanning = false;
            }
        }

        if (!allSuccess || !baselineCompleted) {
            log.warn("天弘基金官方额度同步未完整完成，本次不更新同步标记，allSuccess={}, baselineCompleted={}",
                    allSuccess, baselineCompleted);
            return;
        }
        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName(StockSyncConstant.STOCK_TH_FUND_PURCHASE_LIMIT_LATEST);
        }
        stockSync.setValue(String.valueOf(syncTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        stockSyncRepository.save(stockSync);
        log.info("同步天弘基金官方额度完成，targetFundCount={}", TARGET_FUND_CODES.size());
    }

    private boolean retryPendingAnnouncements(LocalDate syncDate) {
        boolean success = true;
        List<StockFundAnnouncementSync> pendingRecords = stockFundAnnouncementSyncRepository
                .findBySourceAndStatusInOrderByAnnouncementDateDesc(
                        THFundConstant.SOURCE,
                        List.of(FundPurchaseLimitConstant.SYNC_FAILED, FundPurchaseLimitConstant.SYNC_PENDING)
                );
        for (StockFundAnnouncementSync pending : pendingRecords) {
            boolean waitingForEffectiveDate = FundPurchaseLimitConstant.SYNC_PENDING.equals(pending.getStatus())
                    && pending.getRetryAfterDate() != null && pending.getRetryAfterDate().isAfter(syncDate);
            if (!waitingForEffectiveDate) {
                THFundAnnouncement announcement = new THFundAnnouncement();
                announcement.setAnnouncementId(pending.getAnnouncementId());
                announcement.setAnnouncementDate(pending.getAnnouncementDate());
                announcement.setTitle(pending.getTitle());
                announcement.setDetailUrl(pending.getDetailUrl());
                announcement.setAttachmentUrl(pending.getAttachmentUrl());
                if (!processAnnouncement(announcement, syncDate)) {
                    success = false;
                }
            }
        }
        return success;
    }

    private boolean processAnnouncement(THFundAnnouncement announcement, LocalDate syncDate) {
        FundPurchaseLimitAnnouncementDetail detail = new FundPurchaseLimitAnnouncementDetail();
        detail.setDetailUrl(announcement.getDetailUrl());
        detail.setAttachmentUrl(announcement.getAttachmentUrl());
        detail.setAttachmentName(announcement.getAnnouncementId() + ".pdf");
        try {
            byte[] attachment = thFundService.downloadAnnouncement(announcement.getAttachmentUrl());
            List<FundPurchaseLimitRule> rules = thFundAnnouncementParser.parse(announcement.getTitle(), attachment);
            if (rules.isEmpty()) {
                throw new IllegalStateException("公告匹配天弘纳指100但未解析出额度规则");
            }
            LocalDate retryAfterDate = rules.stream()
                    .map(FundPurchaseLimitRule::getEffectiveDate)
                    .filter(date -> date != null && date.isAfter(syncDate))
                    .min(LocalDate::compareTo)
                    .orElse(null);
            String hash = DigestUtils.sha256(attachment);
            if (retryAfterDate != null) {
                stockFundPurchaseLimitService.savePending(
                        THFundConstant.SOURCE, announcement, detail, hash, retryAfterDate
                );
            } else {
                stockFundPurchaseLimitService.saveSuccess(
                        THFundConstant.SOURCE, THFundConstant.SOURCE_NAME, announcement, detail, hash, rules
                );
                log.info("处理天弘基金额度公告完成，announcementId={}, ruleCount={}",
                        announcement.getAnnouncementId(), rules.size());
            }
            return true;
        } catch (Exception e) {
            stockFundPurchaseLimitService.saveFailed(THFundConstant.SOURCE, announcement, detail, e);
            log.error("处理天弘基金额度公告失败，announcementId={}", announcement.getAnnouncementId(), e);
            return false;
        }
    }

    private Map<String, StockFundAnnouncementSync> loadExisting(List<THFundAnnouncement> announcements) {
        if (announcements.isEmpty()) {
            return Map.of();
        }
        Set<String> ids = new HashSet<>();
        for (THFundAnnouncement announcement : announcements) {
            ids.add(announcement.getAnnouncementId());
        }
        Map<String, StockFundAnnouncementSync> result = new HashMap<>();
        for (StockFundAnnouncementSync existing : stockFundAnnouncementSyncRepository
                .findBySourceAndAnnouncementIdIn(THFundConstant.SOURCE, ids)) {
            result.put(existing.getAnnouncementId(), existing);
        }
        return result;
    }

    private LocalDate getLatestProcessedAnnouncementDate() {
        LocalDate successDate = stockFundAnnouncementSyncRepository
                .findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                        THFundConstant.SOURCE, FundPurchaseLimitConstant.SYNC_SUCCESS
                ).map(StockFundAnnouncementSync::getAnnouncementDate).orElse(null);
        LocalDate ignoredDate = stockFundAnnouncementSyncRepository
                .findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                        THFundConstant.SOURCE, FundPurchaseLimitConstant.SYNC_IGNORED
                ).map(StockFundAnnouncementSync::getAnnouncementDate).orElse(null);
        if (successDate == null) {
            return ignoredDate;
        }
        return ignoredDate != null && ignoredDate.isAfter(successDate) ? ignoredDate : successDate;
    }

    private boolean hasCompleteCurrentRules() {
        return TARGET_FUND_CODES.stream().allMatch(code ->
                stockFundPurchaseLimitService.hasCurrentPurchaseLimit(THFundConstant.SOURCE, code));
    }

    private boolean containsNewerAnnouncement(THFundAnnouncementPage page, LocalDate latestProcessedDate) {
        return latestProcessedDate == null || page.getContent().stream()
                .anyMatch(item -> !item.getAnnouncementDate().isBefore(latestProcessedDate));
    }

    private boolean isPurchaseLimitAnnouncement(THFundAnnouncement announcement) {
        String title = announcement.getTitle().replace(" ", "");
        boolean holidayNotice = title.contains("节假日") || title.contains("境外主要市场")
                || title.contains("非交易日") || title.contains("暂停申购赎回等业务");
        return title.contains("天弘纳斯达克100") && !holidayNotice
                && (title.contains("调整大额申购") || title.contains("限制大额申购")
                || title.contains("恢复申购") || title.contains("暂停申购"));
    }

}
