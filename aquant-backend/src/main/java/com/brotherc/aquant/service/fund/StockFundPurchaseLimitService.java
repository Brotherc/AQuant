package com.brotherc.aquant.service.fund;

import com.brotherc.aquant.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.entity.fund.StockFundAnnouncementSync;
import com.brotherc.aquant.entity.fund.StockFundPurchaseLimit;
import com.brotherc.aquant.model.dto.fund.FundPurchaseLimitAnnouncement;
import com.brotherc.aquant.model.dto.fund.FundPurchaseLimitAnnouncementDetail;
import com.brotherc.aquant.model.dto.fund.FundPurchaseLimitRule;
import com.brotherc.aquant.model.vo.stockfund.StockFundPurchaseLimitVO;
import com.brotherc.aquant.repository.fund.StockFundAnnouncementSyncRepository;
import com.brotherc.aquant.repository.fund.StockFundPurchaseLimitRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockFundPurchaseLimitService {

    private final StockFundPurchaseLimitRepository stockFundPurchaseLimitRepository;
    private final StockFundAnnouncementSyncRepository stockFundAnnouncementSyncRepository;

    @Transactional(rollbackFor = Exception.class)
    public void saveSuccess(
            String source, String sourceName, FundPurchaseLimitAnnouncement announcement,
            FundPurchaseLimitAnnouncementDetail detail, String attachmentHash,
            List<? extends FundPurchaseLimitRule> rules
    ) {
        for (FundPurchaseLimitRule rule : rules) {
            StockFundPurchaseLimit entity = stockFundPurchaseLimitRepository
                    .findBySourceAndFundCodeAndSalesChannelAndBusinessType(
                            source, rule.getFundCode(), rule.getSalesChannel(), rule.getBusinessType()
                    ).orElseGet(StockFundPurchaseLimit::new);
            if (entity.getId() != null && isOlderRule(entity, rule, announcement.getAnnouncementDate())) {
                continue;
            }
            entity.setFundCode(rule.getFundCode());
            entity.setSource(source);
            entity.setSourceName(sourceName);
            entity.setSalesChannel(rule.getSalesChannel());
            entity.setSalesChannelName(FundPurchaseLimitConstant.CHANNEL_DIRECT.equals(rule.getSalesChannel())
                    ? FundPurchaseLimitConstant.CHANNEL_DIRECT_NAME
                    : FundPurchaseLimitConstant.CHANNEL_ALL_NAME);
            entity.setBusinessType(rule.getBusinessType());
            entity.setStatus(rule.getStatus());
            entity.setLimitAmount(rule.getLimitAmount());
            entity.setCurrency(rule.getCurrency());
            entity.setEffectiveDate(rule.getEffectiveDate());
            entity.setAnnouncementId(announcement.getAnnouncementId());
            entity.setAnnouncementDate(announcement.getAnnouncementDate());
            entity.setAnnouncementTitle(announcement.getTitle());
            entity.setAnnouncementUrl(detail.getDetailUrl());
            stockFundPurchaseLimitRepository.save(entity);
        }
        saveAnnouncementStatus(
                source, announcement, detail, attachmentHash,
                FundPurchaseLimitConstant.SYNC_SUCCESS, null, null
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveIgnored(
            String source, FundPurchaseLimitAnnouncement announcement,
            FundPurchaseLimitAnnouncementDetail detail, String attachmentHash
    ) {
        saveAnnouncementStatus(
                source, announcement, detail, attachmentHash,
                FundPurchaseLimitConstant.SYNC_IGNORED, null, null
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void savePending(
            String source, FundPurchaseLimitAnnouncement announcement,
            FundPurchaseLimitAnnouncementDetail detail, String attachmentHash, LocalDate retryAfterDate
    ) {
        saveAnnouncementStatus(
                source, announcement, detail, attachmentHash,
                FundPurchaseLimitConstant.SYNC_PENDING, null, retryAfterDate
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveFailed(
            String source, FundPurchaseLimitAnnouncement announcement,
            FundPurchaseLimitAnnouncementDetail detail, Exception exception
    ) {
        StockFundAnnouncementSync entity = stockFundAnnouncementSyncRepository
                .findBySourceAndAnnouncementId(source, announcement.getAnnouncementId())
                .orElseGet(StockFundAnnouncementSync::new);
        entity.setAnnouncementId(announcement.getAnnouncementId());
        entity.setSource(source);
        entity.setTitle(announcement.getTitle());
        entity.setAnnouncementDate(announcement.getAnnouncementDate());
        if (detail != null) {
            entity.setDetailUrl(detail.getDetailUrl());
            entity.setAttachmentUrl(detail.getAttachmentUrl());
        }
        entity.setStatus(FundPurchaseLimitConstant.SYNC_FAILED);
        entity.setFailureCount(entity.getFailureCount() == null ? 1 : entity.getFailureCount() + 1);
        entity.setLastError(StringUtils.abbreviate(exception.getMessage(), 1000));
        entity.setRetryAfterDate(null);
        entity.setProcessedTime(LocalDateTime.now());
        stockFundAnnouncementSyncRepository.save(entity);
    }

    public Map<String, StockFundPurchaseLimit> getPurchaseSummaries(Collection<String> fundCodes) {
        if (CollectionUtils.isEmpty(fundCodes)) {
            return Map.of();
        }
        List<StockFundPurchaseLimit> limits = stockFundPurchaseLimitRepository
                .findByBusinessTypeAndFundCodeIn(FundPurchaseLimitConstant.BUSINESS_PURCHASE, fundCodes);
        Map<String, StockFundPurchaseLimit> result = new HashMap<>();
        for (StockFundPurchaseLimit limit : limits) {
            StockFundPurchaseLimit existing = result.get(limit.getFundCode());
            if (existing == null || isPreferredSummary(limit, existing)) {
                result.put(limit.getFundCode(), limit);
            }
        }
        return result;
    }

    public List<StockFundPurchaseLimitVO> getCurrentLimits(String fundCode) {
        if (StringUtils.isBlank(fundCode)) {
            return List.of();
        }
        List<StockFundPurchaseLimitVO> result = new ArrayList<>();
        for (StockFundPurchaseLimit entity : stockFundPurchaseLimitRepository
                .findByFundCodeOrderBySourceAscBusinessTypeAscSalesChannelAsc(fundCode)) {
            StockFundPurchaseLimitVO vo = new StockFundPurchaseLimitVO();
            vo.setSource(entity.getSource());
            vo.setSourceName(entity.getSourceName());
            vo.setSalesChannel(entity.getSalesChannel());
            vo.setSalesChannelName(entity.getSalesChannelName());
            vo.setBusinessType(entity.getBusinessType());
            vo.setStatus(entity.getStatus());
            vo.setLimitAmount(entity.getLimitAmount());
            vo.setCurrency(entity.getCurrency());
            vo.setEffectiveDate(entity.getEffectiveDate());
            vo.setAnnouncementDate(entity.getAnnouncementDate());
            vo.setAnnouncementTitle(entity.getAnnouncementTitle());
            vo.setAnnouncementUrl(entity.getAnnouncementUrl());
            result.add(vo);
        }
        result.sort(Comparator
                .comparing(StockFundPurchaseLimitVO::getSourceName, Comparator.nullsLast(String::compareTo))
                .thenComparingInt((StockFundPurchaseLimitVO item) -> FundPurchaseLimitConstant.CHANNEL_DIRECT
                        .equals(item.getSalesChannel()) ? 0 : 1)
                .thenComparing(StockFundPurchaseLimitVO::getBusinessType));
        return result;
    }

    public boolean hasCurrentPurchaseLimit(String source, String fundCode) {
        return stockFundPurchaseLimitRepository.existsBySourceAndFundCodeAndBusinessType(
                source, fundCode, FundPurchaseLimitConstant.BUSINESS_PURCHASE
        );
    }

    private boolean isOlderRule(
            StockFundPurchaseLimit existing, FundPurchaseLimitRule incoming, LocalDate incomingAnnouncementDate
    ) {
        if (existing.getEffectiveDate() != null && incoming.getEffectiveDate() != null
                && incoming.getEffectiveDate().isBefore(existing.getEffectiveDate())) {
            return true;
        }
        return existing.getAnnouncementDate() != null && incomingAnnouncementDate != null
                && incomingAnnouncementDate.isBefore(existing.getAnnouncementDate());
    }

    private int channelPriority(StockFundPurchaseLimit limit) {
        return FundPurchaseLimitConstant.CHANNEL_DIRECT.equals(limit.getSalesChannel()) ? 0 : 1;
    }

    private boolean isPreferredSummary(StockFundPurchaseLimit incoming, StockFundPurchaseLimit existing) {
        int priorityComparison = Integer.compare(channelPriority(incoming), channelPriority(existing));
        if (priorityComparison != 0) {
            return priorityComparison < 0;
        }
        if (incoming.getEffectiveDate() != null && existing.getEffectiveDate() != null
                && !incoming.getEffectiveDate().equals(existing.getEffectiveDate())) {
            return incoming.getEffectiveDate().isAfter(existing.getEffectiveDate());
        }
        return incoming.getAnnouncementDate() != null && (existing.getAnnouncementDate() == null
                || incoming.getAnnouncementDate().isAfter(existing.getAnnouncementDate()));
    }

    private void saveAnnouncementStatus(
            String source, FundPurchaseLimitAnnouncement announcement,
            FundPurchaseLimitAnnouncementDetail detail, String attachmentHash,
            String status, String error, LocalDate retryAfterDate
    ) {
        StockFundAnnouncementSync entity = stockFundAnnouncementSyncRepository
                .findBySourceAndAnnouncementId(source, announcement.getAnnouncementId())
                .orElseGet(StockFundAnnouncementSync::new);
        entity.setAnnouncementId(announcement.getAnnouncementId());
        entity.setSource(source);
        entity.setTitle(announcement.getTitle());
        entity.setAnnouncementDate(announcement.getAnnouncementDate());
        if (detail != null) {
            entity.setDetailUrl(detail.getDetailUrl());
            entity.setAttachmentUrl(detail.getAttachmentUrl());
        }
        entity.setAttachmentHash(attachmentHash);
        entity.setStatus(status);
        entity.setFailureCount(0);
        entity.setLastError(error);
        entity.setRetryAfterDate(retryAfterDate);
        entity.setProcessedTime(LocalDateTime.now());
        stockFundAnnouncementSyncRepository.save(entity);
    }

}
