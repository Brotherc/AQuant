package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockShareChange;
import com.brotherc.aquant.model.dto.akshare.StockHoldChangeCninfo;
import com.brotherc.aquant.repository.StockShareChangeRepository;
import com.brotherc.aquant.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockShareChangeService {

    private final StockShareChangeRepository stockShareChangeRepository;

    @Transactional(rollbackFor = Exception.class)
    public int replaceAll(List<StockHoldChangeCninfo> stockHoldChanges) {
        if (CollectionUtils.isEmpty(stockHoldChanges)) {
            return 0;
        }

        Map<String, StockShareChange> shareChangeMap = new LinkedHashMap<>();
        for (StockHoldChangeCninfo stockHoldChange : stockHoldChanges) {
            if (stockHoldChange == null || StringUtils.isBlank(stockHoldChange.getStockCode())) {
                continue;
            }

            String wrappedStockCode = StockUtils.wrapExchangePrefix(stockHoldChange.getStockCode());
            LocalDate announcementDate = parseDate(stockHoldChange.getAnnouncementDate());
            LocalDate changeDate = parseDate(stockHoldChange.getChangeDate());
            StockShareChange entity = new StockShareChange();
            entity.setStockCode(wrappedStockCode);
            entity.setStockName(stockHoldChange.getStockName());
            entity.setMarket(stockHoldChange.getMarket());
            entity.setAnnouncementDate(announcementDate);
            entity.setChangeDate(changeDate);
            entity.setChangeReason(stockHoldChange.getChangeReason());
            entity.setTotalShares10k(stockHoldChange.getTotalShares());
            entity.setFloatingShares10k(stockHoldChange.getFloatingShares());
            entity.setFloatingRatio(stockHoldChange.getFloatingRatio());
            entity.setRestrictedShares10k(stockHoldChange.getRestrictedShares());

            String key = wrappedStockCode + "|" + Objects.toString(changeDate, "") + "|"
                    + Objects.toString(announcementDate, "") + "|" + Objects.toString(stockHoldChange.getChangeReason(), "");
            shareChangeMap.put(key, entity);
        }

        if (shareChangeMap.isEmpty()) {
            log.warn("股本变动数据为空，跳过保存，sourceCount={}", stockHoldChanges.size());
            return 0;
        }

        stockShareChangeRepository.deleteAllInBatch();
        stockShareChangeRepository.saveAll(shareChangeMap.values());
        return shareChangeMap.size();
    }

    private LocalDate parseDate(String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }
        try {
            return LocalDateTime.parse(date).toLocalDate();
        } catch (Exception e) {
            log.warn("股本变动日期解析失败，date={}", date);
            return null;
        }
    }

}
