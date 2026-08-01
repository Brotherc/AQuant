package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockPerformanceReport;
import com.brotherc.aquant.model.dto.akshare.StockYjbbEm;
import com.brotherc.aquant.repository.StockPerformanceReportRepository;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockPerformanceReportService {

    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StockPerformanceReportRepository stockPerformanceReportRepository;

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
        stockPerformanceReportRepository.deleteByReportDate(parsedReportDate);

        List<StockPerformanceReport> saveList = new ArrayList<>();
        for (StockYjbbEm stockYjbbEm : stockYjbbEms) {
            if (stockYjbbEm == null || StringUtils.isBlank(stockYjbbEm.getStockCode())) {
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
    }

}
