package com.brotherc.aquant.indicator.service;

import com.brotherc.aquant.indicator.entity.StockBalanceSheet;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.integration.akshare.model.StockZcfzEm;
import com.brotherc.aquant.indicator.repository.StockBalanceSheetRepository;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.common.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockBalanceSheetService {

    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StockBalanceSheetRepository stockBalanceSheetRepository;
    private final StockQuoteRepository stockQuoteRepository;

    public boolean existsByReportDate(String reportDate) {
        return stockBalanceSheetRepository.existsByReportDate(LocalDate.parse(reportDate, REPORT_DATE_FORMATTER));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean save(String reportDate, List<StockZcfzEm> sourceList) {
        if (StringUtils.isBlank(reportDate) || CollectionUtils.isEmpty(sourceList)) {
            return false;
        }

        LocalDate parsedReportDate = LocalDate.parse(reportDate, REPORT_DATE_FORMATTER);
        List<StockZcfzEm> validSourceList = sourceList.stream()
                .filter(item -> item != null && StringUtils.isNotBlank(item.getStockCode()))
                .toList();
        Map<String, StockZcfzEm> uniqueReportMap = validSourceList.stream()
                .collect(LinkedHashMap::new,
                        (map, item) -> map.put(item.getStockCode(), item),
                        Map::putAll);
        Set<String> validStockCodes = findValidStockCodes(new ArrayList<>(uniqueReportMap.values()));
        if (CollectionUtils.isEmpty(validStockCodes)) {
            log.warn("股票资产负债表未匹配到当前股票池，跳过保存，reportDate={}, sourceCount={}",
                    reportDate, sourceList.size());
            return false;
        }

        stockBalanceSheetRepository.deleteByReportDate(parsedReportDate);
        stockBalanceSheetRepository.flush();

        List<StockBalanceSheet> saveList = new ArrayList<>();
        int filteredCount = 0;
        for (StockZcfzEm source : uniqueReportMap.values()) {
            if (!validStockCodes.contains(source.getStockCode())) {
                filteredCount++;
                continue;
            }

            StockBalanceSheet entity = new StockBalanceSheet();
            entity.setReportDate(parsedReportDate);
            entity.setStockCode(source.getStockCode());
            entity.setStockName(source.getStockName());
            entity.setMonetaryFunds(source.getAssetMonetaryFunds());
            entity.setAccountsReceivable(source.getAssetAccountsReceivable());
            entity.setInventory(source.getAssetInventory());
            entity.setTotalAssets(source.getAssetTotalAssets());
            entity.setTotalAssetsYoY(source.getAssetTotalAssetsYoY());
            entity.setAccountsPayable(source.getLiabilityAccountsPayable());
            entity.setAdvanceReceipts(source.getLiabilityAdvanceReceipts());
            entity.setTotalLiabilities(source.getLiabilityTotalLiabilities());
            entity.setTotalLiabilitiesYoY(source.getLiabilityTotalLiabilitiesYoY());
            entity.setAssetLiabilityRatio(source.getAssetLiabilityRatio());
            entity.setTotalEquity(source.getTotalEquity());
            if (StringUtils.isNotBlank(source.getNoticeDate())) {
                try {
                    entity.setAnnouncementDate(LocalDateTime.parse(source.getNoticeDate()).toLocalDate());
                } catch (Exception e) {
                    log.warn("股票资产负债表公告日期解析失败，stockCode={}, announcementDate={}",
                            source.getStockCode(), source.getNoticeDate());
                }
            }
            saveList.add(entity);
        }

        if (!saveList.isEmpty()) {
            stockBalanceSheetRepository.saveAll(saveList);
        }
        if (filteredCount > 0) {
            log.info("股票资产负债表已过滤非当前股票池数据，reportDate={}, filteredCount={}, savedCount={}",
                    reportDate, filteredCount, saveList.size());
        }
        int duplicateCount = validSourceList.size() - uniqueReportMap.size();
        if (duplicateCount > 0) {
            log.info("股票资产负债表已合并沪深和北交所重复股票代码，reportDate={}, duplicateCount={}, savedCount={}",
                    reportDate, duplicateCount, saveList.size());
        }
        return !saveList.isEmpty();
    }

    private Set<String> findValidStockCodes(List<StockZcfzEm> sourceList) {
        Set<String> reportStockCodes = sourceList.stream()
                .map(StockZcfzEm::getStockCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollectionUtils.isEmpty(reportStockCodes)) {
            return Set.of();
        }

        List<String> quoteCodes = reportStockCodes.stream().map(StockUtils::wrapExchangePrefix).toList();
        Set<String> existsQuoteCodeSet = stockQuoteRepository.findByCodeIn(quoteCodes).stream()
                .map(StockQuote::getCode)
                .collect(Collectors.toSet());
        return reportStockCodes.stream()
                .filter(stockCode -> existsQuoteCodeSet.contains(StockUtils.wrapExchangePrefix(stockCode)))
                .collect(Collectors.toSet());
    }

}
