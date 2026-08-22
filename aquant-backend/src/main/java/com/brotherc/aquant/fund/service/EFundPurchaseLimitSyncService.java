package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.common.constant.EFundConstant;
import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.utils.DigestUtils;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.fund.entity.StockFundAnnouncementSync;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitAnnouncementDetail;
import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitRule;
import com.brotherc.aquant.fund.repository.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.integration.efund.model.EFundAnnouncement;
import com.brotherc.aquant.integration.efund.model.EFundAnnouncementPage;
import com.brotherc.aquant.integration.efund.service.EFundService;
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
public class EFundPurchaseLimitSyncService implements FundPurchaseLimitSyncService {

    private static final Set<String> TARGET_FUND_CODES = Set.of("161130", "012870", "003722", "012871");

    private final EFundService eFundService;
    private final EFundAnnouncementParser eFundAnnouncementParser;
    private final StockFundPurchaseLimitService stockFundPurchaseLimitService;
    private final StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;
    private final StockSyncRepository stockSyncRepository;

    @Override
    public String getSourceName() {
        return EFundConstant.SOURCE_NAME;
    }

    /**
     * 每天增量扫描易方达纳斯达克100指数基金的官方额度公告；首次取得四个份额当前规则后停止回溯。
     */
    @Override
    public void sync(LocalDateTime syncTime) {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_EFUND_PURCHASE_LIMIT_LATEST);
        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        LocalDate lastSyncDate = null;
        if (lastTimestamp != null) {
            lastSyncDate = Instant.ofEpochMilli(lastTimestamp).atZone(ZoneId.systemDefault()).toLocalDate();
            if (lastSyncDate.equals(syncTime.toLocalDate())) {
                log.info("易方达基金官方额度当天已同步，跳过本次同步，syncDate={}", lastSyncDate);
                return;
            }
        }

        boolean allSuccess = retryPendingAnnouncements(syncTime.toLocalDate());
        LocalDate latestProcessedDate = getLatestProcessedAnnouncementDate();
        boolean baselineCompleted = hasCompleteCurrentRules();
        LocalDate announcementStartDate = baselineCompleted
                ? (lastSyncDate != null ? lastSyncDate : latestProcessedDate) : null;
        int page = 1;
        while (true) {
            EFundAnnouncementPage announcementPage;
            try {
                announcementPage = eFundService.getNasdaq100Announcements(page);
            } catch (Exception e) {
                allSuccess = false;
                log.error("获取易方达基金公告列表失败，page={}", page, e);
                break;
            }
            List<EFundAnnouncement> relevantAnnouncements = announcementPage.getContent().stream()
                    .filter(this::isPurchaseLimitAnnouncement)
                    .filter(announcement -> announcementStartDate == null
                            || !announcement.getAnnouncementDate().isBefore(announcementStartDate))
                    .sorted(Comparator.comparing(EFundAnnouncement::getAnnouncementDate).reversed())
                    .toList();
            Map<String, StockFundAnnouncementSync> existingMap = loadExisting(relevantAnnouncements);
            for (EFundAnnouncement announcement : relevantAnnouncements) {
                StockFundAnnouncementSync existing = existingMap.get(announcement.getAnnouncementId());
                if (baselineCompleted && existing != null
                        && (FundPurchaseLimitConstant.SYNC_SUCCESS.equals(existing.getStatus())
                        || FundPurchaseLimitConstant.SYNC_IGNORED.equals(existing.getStatus()))) {
                    continue;
                }
                if (existing != null && FundPurchaseLimitConstant.SYNC_PENDING.equals(existing.getStatus())
                        && existing.getRetryAfterDate() != null
                        && existing.getRetryAfterDate().isAfter(syncTime.toLocalDate())) {
                    continue;
                }
                if (!processAnnouncement(announcement, syncTime.toLocalDate())) {
                    allSuccess = false;
                }
                if (!baselineCompleted && hasCompleteCurrentRules()) {
                    baselineCompleted = true;
                    break;
                }
            }
            if ((lastTimestamp == null && baselineCompleted) || page >= announcementPage.getTotalPages()
                    || !containsNewerAnnouncement(announcementPage, announcementStartDate)) {
                break;
            }
            page++;
        }

        if (!allSuccess || !baselineCompleted) {
            log.warn("易方达基金官方额度同步未完整完成，本次不更新同步标记");
            return;
        }
        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName(StockSyncConstant.STOCK_EFUND_PURCHASE_LIMIT_LATEST);
        }
        stockSync.setValue(String.valueOf(syncTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        stockSyncRepository.save(stockSync);
        log.info("同步易方达基金官方额度完成，targetFundCount={}", TARGET_FUND_CODES.size());
    }

    private boolean retryPendingAnnouncements(LocalDate syncDate) {
        boolean success = true;
        List<StockFundAnnouncementSync> pendingRecords = stockFundAnnouncementSyncRepository
                .findBySourceAndStatusInOrderByAnnouncementDateDesc(
                        EFundConstant.SOURCE,
                        List.of(FundPurchaseLimitConstant.SYNC_FAILED, FundPurchaseLimitConstant.SYNC_PENDING)
                );
        for (StockFundAnnouncementSync pending : pendingRecords) {
            if (FundPurchaseLimitConstant.SYNC_PENDING.equals(pending.getStatus())
                    && pending.getRetryAfterDate() != null && pending.getRetryAfterDate().isAfter(syncDate)) {
                continue;
            }
            EFundAnnouncement announcement = new EFundAnnouncement();
            announcement.setAnnouncementId(pending.getAnnouncementId());
            announcement.setAnnouncementDate(pending.getAnnouncementDate());
            announcement.setTitle(pending.getTitle());
            announcement.setDetailUrl(pending.getDetailUrl());
            announcement.setAttachmentUrl(pending.getAttachmentUrl());
            if (!processAnnouncement(announcement, syncDate)) {
                success = false;
            }
        }
        return success;
    }

    private boolean processAnnouncement(EFundAnnouncement announcement, LocalDate syncDate) {
        FundPurchaseLimitAnnouncementDetail detail = new FundPurchaseLimitAnnouncementDetail();
        detail.setDetailUrl(announcement.getDetailUrl());
        detail.setAttachmentUrl(announcement.getAttachmentUrl());
        detail.setAttachmentName(announcement.getAnnouncementId() + ".pdf");
        try {
            byte[] attachment = eFundService.downloadAnnouncement(announcement.getAttachmentUrl());
            List<FundPurchaseLimitRule> rules = eFundAnnouncementParser.parse(announcement.getTitle(), attachment);
            if (rules.isEmpty()) {
                throw new IllegalStateException("公告匹配易方达纳指100但未解析出额度规则");
            }
            LocalDate retryAfterDate = rules.stream()
                    .map(FundPurchaseLimitRule::getEffectiveDate)
                    .filter(date -> date != null && date.isAfter(syncDate))
                    .min(LocalDate::compareTo)
                    .orElse(null);
            String hash = DigestUtils.sha256(attachment);
            if (retryAfterDate != null) {
                stockFundPurchaseLimitService.savePending(
                        EFundConstant.SOURCE, announcement, detail, hash, retryAfterDate
                );
                return true;
            }
            stockFundPurchaseLimitService.saveSuccess(
                    EFundConstant.SOURCE, EFundConstant.SOURCE_NAME, announcement, detail, hash, rules
            );
            log.info("处理易方达基金额度公告完成，announcementId={}, ruleCount={}",
                    announcement.getAnnouncementId(), rules.size());
            return true;
        } catch (Exception e) {
            stockFundPurchaseLimitService.saveFailed(EFundConstant.SOURCE, announcement, detail, e);
            log.error("处理易方达基金额度公告失败，announcementId={}", announcement.getAnnouncementId(), e);
            return false;
        }
    }

    private Map<String, StockFundAnnouncementSync> loadExisting(List<EFundAnnouncement> announcements) {
        if (announcements.isEmpty()) {
            return Map.of();
        }
        Set<String> ids = new HashSet<>();
        for (EFundAnnouncement announcement : announcements) {
            ids.add(announcement.getAnnouncementId());
        }
        Map<String, StockFundAnnouncementSync> result = new HashMap<>();
        for (StockFundAnnouncementSync existing : stockFundAnnouncementSyncRepository
                .findBySourceAndAnnouncementIdIn(EFundConstant.SOURCE, ids)) {
            result.put(existing.getAnnouncementId(), existing);
        }
        return result;
    }

    private LocalDate getLatestProcessedAnnouncementDate() {
        LocalDate successDate = stockFundAnnouncementSyncRepository
                .findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                        EFundConstant.SOURCE, FundPurchaseLimitConstant.SYNC_SUCCESS
                ).map(StockFundAnnouncementSync::getAnnouncementDate).orElse(null);
        LocalDate ignoredDate = stockFundAnnouncementSyncRepository
                .findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                        EFundConstant.SOURCE, FundPurchaseLimitConstant.SYNC_IGNORED
                ).map(StockFundAnnouncementSync::getAnnouncementDate).orElse(null);
        if (successDate == null) {
            return ignoredDate;
        }
        return ignoredDate != null && ignoredDate.isAfter(successDate) ? ignoredDate : successDate;
    }

    private boolean hasCompleteCurrentRules() {
        return TARGET_FUND_CODES.stream().allMatch(code ->
                stockFundPurchaseLimitService.hasCurrentPurchaseLimit(EFundConstant.SOURCE, code));
    }

    private boolean containsNewerAnnouncement(EFundAnnouncementPage page, LocalDate latestProcessedDate) {
        return latestProcessedDate == null || page.getContent().stream()
                .anyMatch(item -> !item.getAnnouncementDate().isBefore(latestProcessedDate));
    }

    private boolean isPurchaseLimitAnnouncement(EFundAnnouncement announcement) {
        String title = announcement.getTitle().replace(" ", "");
        boolean holidayNotice = title.contains("节假日") || title.contains("境外主要投资市场")
                || title.contains("非交易日") || title.contains("提示性公告");
        return title.contains("纳斯达克100") && !holidayNotice && (title.contains("大额申购")
                || title.contains("恢复申购") || title.contains("暂停申购")
                || (title.contains("申购") && title.contains("限制金额")));
    }

}
