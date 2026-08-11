package com.brotherc.aquant.service.indicators;

import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.entity.StockPerformanceReport;
import com.brotherc.aquant.model.dto.akshare.StockYjbbEm;
import com.brotherc.aquant.repository.StockPerformanceReportRepository;
import com.brotherc.aquant.repository.StockQuoteRepository;
import com.brotherc.aquant.utils.StockUtils;
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
public class StockPerformanceReportService {

    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StockPerformanceReportRepository stockPerformanceReportRepository;
    private final StockQuoteRepository stockQuoteRepository;

    public boolean existsByReportDate(String reportDate) {
        return stockPerformanceReportRepository.existsByReportDate(LocalDate.parse(reportDate, REPORT_DATE_FORMATTER));
    }

    public boolean hasAnyReport() {
        return stockPerformanceReportRepository.count() > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(String reportDate, List<StockYjbbEm> stockYjbbEms) {
        if (StringUtils.isBlank(reportDate) || CollectionUtils.isEmpty(stockYjbbEms)) {
            return;
        }

        LocalDate parsedReportDate = LocalDate.parse(reportDate, REPORT_DATE_FORMATTER);
        List<StockYjbbEm> validSourceList = stockYjbbEms.stream()
                .filter(stockYjbbEm -> stockYjbbEm != null && StringUtils.isNotBlank(stockYjbbEm.getStockCode()))
                .toList();
        Map<String, StockYjbbEm> uniqueReportMap = validSourceList.stream()
                .collect(LinkedHashMap::new, (map, stockYjbbEm) -> map.putIfAbsent(stockYjbbEm.getStockCode(), stockYjbbEm), Map::putAll);
        Set<String> validStockCodes = findValidStockCodes(new ArrayList<>(uniqueReportMap.values()));
        if (CollectionUtils.isEmpty(validStockCodes)) {
            log.warn("股票业绩报表未匹配到当前股票池，跳过保存，reportDate={}, sourceCount={}",
                    reportDate, stockYjbbEms.size());
            return;
        }

        stockPerformanceReportRepository.deleteByReportDate(parsedReportDate);
        stockPerformanceReportRepository.flush();

        List<StockPerformanceReport> saveList = new ArrayList<>();
        int filteredCount = 0;
        for (StockYjbbEm stockYjbbEm : uniqueReportMap.values()) {
            if (!validStockCodes.contains(stockYjbbEm.getStockCode())) {
                filteredCount++;
                continue;
            }
            StockPerformanceReport entity = new StockPerformanceReport();
            entity.setReportDate(parsedReportDate);
            entity.setStockCode(stockYjbbEm.getStockCode());
            entity.setStockName(stockYjbbEm.getStockName());
            entity.setEarningsPerShare(stockYjbbEm.getEarningsPerShare());
            entity.setTotalRevenue(stockYjbbEm.getTotalRevenue());
            entity.setTotalRevenueYoY(stockYjbbEm.getTotalRevenueYoY());
            entity.setTotalRevenueQoQ(stockYjbbEm.getTotalRevenueQoQ());
            entity.setNetProfit(stockYjbbEm.getNetProfit());
            entity.setNetProfitYoY(stockYjbbEm.getNetProfitYoY());
            entity.setNetProfitQoQ(stockYjbbEm.getNetProfitQoQ());
            entity.setNetAssetsPerShare(stockYjbbEm.getNetAssetsPerShare());
            entity.setRoe(stockYjbbEm.getRoe());
            entity.setOperatingCashFlowPerShare(stockYjbbEm.getOperatingCashFlowPerShare());
            entity.setGrossProfitMargin(stockYjbbEm.getGrossProfitMargin());
            entity.setIndustry(stockYjbbEm.getIndustry());

            if (StringUtils.isNotBlank(stockYjbbEm.getLatestAnnouncementDate())) {
                try {
                    entity.setLatestAnnouncementDate(
                            LocalDateTime.parse(stockYjbbEm.getLatestAnnouncementDate()).toLocalDate()
                    );
                } catch (Exception e) {
                    log.warn("股票业绩报表最新公告日期解析失败，stockCode={}, latestAnnouncementDate={}",
                            stockYjbbEm.getStockCode(), stockYjbbEm.getLatestAnnouncementDate());
                }
            }
            saveList.add(entity);
        }

        if (!saveList.isEmpty()) {
            stockPerformanceReportRepository.saveAll(saveList);
        }
        if (filteredCount > 0) {
            log.info("股票业绩报表已过滤非当前股票池数据，reportDate={}, filteredCount={}, savedCount={}",
                    reportDate, filteredCount, saveList.size());
        }
        int duplicateCount = validSourceList.size() - uniqueReportMap.size();
        if (duplicateCount > 0) {
            log.info("股票业绩报表已过滤重复股票代码数据，reportDate={}, duplicateCount={}, savedCount={}",
                    reportDate, duplicateCount, saveList.size());
        }
    }

    private Set<String> findValidStockCodes(List<StockYjbbEm> stockYjbbEms) {
        Set<String> reportStockCodes = stockYjbbEms.stream()
                .filter(stockYjbbEm -> stockYjbbEm != null && StringUtils.isNotBlank(stockYjbbEm.getStockCode()))
                .map(StockYjbbEm::getStockCode)
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
