package com.brotherc.aquant.service.fund;

import com.brotherc.aquant.constant.CCBFundConstant;
import com.brotherc.aquant.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.constant.StockSyncConstant;
import com.brotherc.aquant.entity.fund.StockFundAnnouncementSync;
import com.brotherc.aquant.entity.sync.StockSync;
import com.brotherc.aquant.model.dto.ccbfund.CCBFundAnnouncement;
import com.brotherc.aquant.model.dto.ccbfund.CCBFundAnnouncementDetail;
import com.brotherc.aquant.model.dto.ccbfund.CCBFundAnnouncementPage;
import com.brotherc.aquant.model.dto.ccbfund.CCBFundAnnouncementParseResult;
import com.brotherc.aquant.model.dto.ccbfund.CCBFundInfo;
import com.brotherc.aquant.model.dto.ccbfund.CCBFundPurchaseRule;
import com.brotherc.aquant.repository.fund.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.repository.sync.StockSyncRepository;
import com.brotherc.aquant.service.ccb.CCBFundService;
import com.brotherc.aquant.utils.DigestUtils;
import com.brotherc.aquant.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CCBFundPurchaseLimitSyncService implements FundPurchaseLimitSyncService {

    private static final int MAX_ATTACHMENT_SIZE = 15 * 1024 * 1024;

    private final CCBFundService ccbFundService;
    private final CCBFundAnnouncementParser ccbFundAnnouncementParser;
    private final StockFundPurchaseLimitService stockFundPurchaseLimitService;
    private final StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;
    private final StockSyncRepository stockSyncRepository;

    @Value("${ccbfund-address}")
    private String ccbFundAddress;

    @Override
    public String getSourceName() {
        return CCBFundConstant.SOURCE_NAME;
    }

    /**
     * 每天同步一次建信官网纳斯达克100指数基金的官方申购额度公告。
     *
     * <p>同步只在所有需要处理的公告都成功或确认可忽略后才更新水位，避免中途失败后第二天跳过历史补偿。
     */
    @Override
    public void sync(LocalDateTime syncTime) {
        // 以本地日期作为每日水位；当天成功跑过一次后不再重复扫描官网公告。
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_CCB_FUND_PURCHASE_LIMIT_LATEST);
        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        if (lastTimestamp != null) {
            LocalDate lastSyncDate = Instant.ofEpochMilli(lastTimestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            if (lastSyncDate.equals(syncTime.toLocalDate())) {
                log.info("建信基金官方额度当天已同步，跳过本次同步，syncDate={}", lastSyncDate);
                return;
            }
        }

        // 只关注建信官网“海外基金”分类中的纳斯达克100指数基金及其不同份额。
        List<CCBFundInfo> targetFunds = ccbFundService.getNasdaq100IndexFunds();
        if (CollectionUtils.isEmpty(targetFunds)) {
            log.warn("建信基金官网未返回纳斯达克100指数基金，不更新同步标记");
            return;
        }
        Set<String> targetCodes = new HashSet<>();
        for (CCBFundInfo targetFund : targetFunds) {
            targetCodes.add(targetFund.getFundCode());
        }

        Set<String> handledThisRun = new HashSet<>();
        // 先补偿历史失败和未生效公告，再扫描列表，保证旧问题不会被增量停止逻辑跳过。
        boolean allSuccess = retryPendingAnnouncements(syncTime.toLocalDate(), targetCodes, handledThisRun);
        // 首次同步只向前查到目标份额已有当前申购规则，不再遍历更早的历史公告。
        boolean baselineCompleted = lastTimestamp != null;
        // 完成首次基线后才使用日期水位；首次中断时仍能继续向前找到当前有效规则。
        LocalDate announcementStartDate = baselineCompleted ? getLatestProcessedAnnouncementDate() : null;
        for (CCBFundInfo fund : targetFunds) {
            if (!baselineCompleted && stockFundPurchaseLimitService.hasCurrentPurchaseLimit(
                    CCBFundConstant.SOURCE, fund.getFundCode())) {
                continue;
            }
            int page = 1;
            boolean currentRuleFound = false;
            while (true) {
                CCBFundAnnouncementPage announcementPage;
                try {
                    announcementPage = ccbFundService.getPurchaseLimitAnnouncements(
                            fund.getFundCode(), announcementStartDate, syncTime.toLocalDate(), page
                    );
                } catch (Exception e) {
                    allSuccess = false;
                    log.error("获取建信基金公告列表失败，fundCode={}, page={}", fund.getFundCode(), page, e);
                    break;
                }
                if (CollectionUtils.isEmpty(announcementPage.getContent())) {
                    break;
                }

                // 官网曾在结束日期为空时忽略开始日期；本地再限制一次，避免异常响应触发历史回扫。
                List<CCBFundAnnouncement> announcements = announcementPage.getContent().stream()
                        .filter(announcement -> announcement.getAnnouncementDate() == null
                                || ((announcementStartDate == null
                                || !announcement.getAnnouncementDate().isBefore(announcementStartDate))
                                && !announcement.getAnnouncementDate().isAfter(syncTime.toLocalDate())))
                        .toList();
                if (CollectionUtils.isEmpty(announcements)) {
                    break;
                }

                Map<String, StockFundAnnouncementSync> existingMap = loadExisting(announcements);
                boolean pageHasPending = false;
                for (CCBFundAnnouncement announcement : announcements) {
                    StockFundAnnouncementSync existing = existingMap.get(announcement.getAnnouncementId());
                    // 成功和确认无关的公告不再下载详情或附件，减少每天重复请求历史附件。
                    if (existing != null && (FundPurchaseLimitConstant.SYNC_SUCCESS.equals(existing.getStatus())
                            || FundPurchaseLimitConstant.SYNC_IGNORED.equals(existing.getStatus()))) {
                        continue;
                    }
                    pageHasPending = true;
                    // 同一公告可能出现在多个基金份额列表里，本轮只处理一次，依靠 cntId 全局去重。
                    if (!handledThisRun.add(announcement.getAnnouncementId())) {
                        if (existing != null && FundPurchaseLimitConstant.SYNC_FAILED.equals(existing.getStatus())) {
                            allSuccess = false;
                        }
                        continue;
                    }
                    if (!processAnnouncement(announcement, null, targetCodes, syncTime.toLocalDate())) {
                        allSuccess = false;
                    }
                    if (!baselineCompleted && stockFundPurchaseLimitService.hasCurrentPurchaseLimit(
                            CCBFundConstant.SOURCE, fund.getFundCode())) {
                        currentRuleFound = true;
                        break;
                    }
                }

                if (currentRuleFound) {
                    log.info("已获取基金最新官方申购规则，停止扫描更早公告，fundCode={}, page={}",
                            fund.getFundCode(), page);
                    break;
                }
                if (baselineCompleted && !pageHasPending) {
                    // 增量同步时，当前页全是已完成/已忽略公告，后续更旧页面通常也无需继续扫描。
                    break;
                }
                if (page >= announcementPage.getTotalPages()) {
                    break;
                }
                page++;
            }
        }

        if (!allSuccess) {
            // 不更新水位，下一次启动或定时同步会继续重试失败公告。
            log.warn("建信基金官方额度存在失败公告，本次不更新同步标记");
            return;
        }
        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName(StockSyncConstant.STOCK_CCB_FUND_PURCHASE_LIMIT_LATEST);
        }
        long timestamp = syncTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        stockSync.setValue(String.valueOf(timestamp));
        stockSyncRepository.save(stockSync);
        log.info("同步建信基金官方额度完成，targetFundCount={}", targetFunds.size());
    }

    private LocalDate getLatestProcessedAnnouncementDate() {
        LocalDate successDate = stockFundAnnouncementSyncRepository
                .findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                        CCBFundConstant.SOURCE, FundPurchaseLimitConstant.SYNC_SUCCESS
                )
                .map(StockFundAnnouncementSync::getAnnouncementDate)
                .orElse(null);
        LocalDate ignoredDate = stockFundAnnouncementSyncRepository
                .findTopBySourceAndStatusOrderByAnnouncementDateDesc(
                        CCBFundConstant.SOURCE, FundPurchaseLimitConstant.SYNC_IGNORED
                )
                .map(StockFundAnnouncementSync::getAnnouncementDate)
                .orElse(null);
        if (successDate == null) {
            return ignoredDate;
        }
        return ignoredDate != null && ignoredDate.isAfter(successDate) ? ignoredDate : successDate;
    }

    /**
     * 先重试历史失败公告和已解析但尚未生效的公告，避免每天只扫描列表时遗漏需要补处理的记录。
     */
    private boolean retryPendingAnnouncements(
            LocalDate syncDate, Set<String> targetCodes, Set<String> handledThisRun
    ) {
        boolean success = true;
        List<StockFundAnnouncementSync> failedRecords = stockFundAnnouncementSyncRepository
                .findBySourceAndStatusInOrderByAnnouncementDateDesc(
                        CCBFundConstant.SOURCE,
                        List.of(FundPurchaseLimitConstant.SYNC_FAILED, FundPurchaseLimitConstant.SYNC_PENDING)
                );
        for (StockFundAnnouncementSync failed : failedRecords) {
            String announcementId = failed.getAnnouncementId();
            handledThisRun.add(announcementId);
            if (FundPurchaseLimitConstant.SYNC_PENDING.equals(failed.getStatus())
                    && failed.getRetryAfterDate() != null && failed.getRetryAfterDate().isAfter(syncDate)) {
                continue;
            }
            CCBFundAnnouncement announcement = new CCBFundAnnouncement();
            announcement.setAnnouncementId(announcementId);
            announcement.setAnnouncementDate(failed.getAnnouncementDate());
            announcement.setTitle(failed.getTitle());
            CCBFundAnnouncementDetail detail = null;
            if (failed.getAttachmentUrl() != null) {
                detail = new CCBFundAnnouncementDetail();
                detail.setDetailUrl(failed.getDetailUrl());
                detail.setAttachmentUrl(failed.getAttachmentUrl());
                String url = failed.getAttachmentUrl();
                detail.setAttachmentName(url.substring(url.lastIndexOf('/') + 1));
            }
            if (!processAnnouncement(announcement, detail, targetCodes, syncDate)) {
                success = false;
            }
        }
        return success;
    }

    private boolean processAnnouncement(
            CCBFundAnnouncement announcement, CCBFundAnnouncementDetail savedDetail,
            Set<String> targetCodes, LocalDate syncDate
    ) {
        CCBFundAnnouncementDetail detail = savedDetail;
        try {
            if (detail == null) {
                detail = ccbFundService.getAnnouncementDetail(announcement.getAnnouncementId());
            }
            HttpUrl attachmentUrl = HttpUrl.parse(detail.getAttachmentUrl());
            if (attachmentUrl == null) {
                throw new IllegalArgumentException("附件地址不合法");
            }
            HttpUrl officialBase = HttpUrl.get(ccbFundAddress);
            if (!officialBase.host().equals(attachmentUrl.host()) || !"https".equals(attachmentUrl.scheme())) {
                throw new IllegalArgumentException("仅允许访问配置的建信基金 HTTPS 域名");
            }
            String path = attachmentUrl.encodedPath().toLowerCase(Locale.ROOT);
            if (!path.endsWith(".doc") && !path.endsWith(".docx")) {
                throw new IllegalArgumentException("仅允许下载 Word 公告附件");
            }
            byte[] attachment = ccbFundService.execute(attachmentUrl, MAX_ATTACHMENT_SIZE);
            CCBFundAnnouncementParseResult parseResult = ccbFundAnnouncementParser.parse(
                    announcement.getTitle(), detail.getAttachmentName(), attachment, targetCodes
            );
            String hash = DigestUtils.sha256(attachment);
            if (!parseResult.isMatchedTargetFund()) {
                stockFundPurchaseLimitService.saveIgnored(CCBFundConstant.SOURCE, announcement, detail, hash);
                return true;
            }
            if (CollectionUtils.isEmpty(parseResult.getRules())) {
                throw new IllegalStateException("公告匹配目标基金但未解析出额度规则");
            }
            boolean futureEffectiveRule = parseResult.getRules().stream()
                    .anyMatch(rule -> rule.getEffectiveDate() != null && rule.getEffectiveDate().isAfter(syncDate));
            if (futureEffectiveRule) {
                LocalDate retryAfterDate = parseResult.getRules().stream()
                        .map(CCBFundPurchaseRule::getEffectiveDate)
                        .filter(Objects::nonNull)
                        .filter(date -> date.isAfter(syncDate))
                        .min(LocalDate::compareTo)
                        .orElse(syncDate.plusDays(1));
                stockFundPurchaseLimitService.savePending(
                        CCBFundConstant.SOURCE, announcement, detail, hash, retryAfterDate
                );
                log.info("建信基金额度公告尚未生效，暂不覆盖当前规则，announcementId={}",
                        announcement.getAnnouncementId());
                return true;
            }
            stockFundPurchaseLimitService.saveSuccess(
                    CCBFundConstant.SOURCE, CCBFundConstant.SOURCE_NAME,
                    announcement, detail, hash, parseResult.getRules()
            );
            log.info("处理建信基金额度公告完成，announcementId={}, ruleCount={}",
                    announcement.getAnnouncementId(), parseResult.getRules().size());
            return true;
        } catch (Exception e) {
            stockFundPurchaseLimitService.saveFailed(CCBFundConstant.SOURCE, announcement, detail, e);
            log.error("处理建信基金额度公告失败，announcementId={}", announcement.getAnnouncementId(), e);
            return false;
        }
    }

    private Map<String, StockFundAnnouncementSync> loadExisting(List<CCBFundAnnouncement> announcements) {
        List<String> ids = announcements.stream().map(CCBFundAnnouncement::getAnnouncementId).toList();
        Map<String, StockFundAnnouncementSync> result = new HashMap<>();
        for (StockFundAnnouncementSync entity : stockFundAnnouncementSyncRepository
                .findBySourceAndAnnouncementIdIn(CCBFundConstant.SOURCE, ids)) {
            result.put(entity.getAnnouncementId(), entity);
        }
        return result;
    }

}
