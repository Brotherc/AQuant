package com.brotherc.aquant.service.indicator;

import com.brotherc.aquant.entity.indicator.StockGrowthMetrics;
import com.brotherc.aquant.entity.indicator.StockPerformanceReport;
import com.brotherc.aquant.entity.stock.StockQuote;
import com.brotherc.aquant.model.dto.akshare.StockZhGrowthComparisonEm;
import com.brotherc.aquant.model.dto.indicator.GrowthIndustryMetrics;
import com.brotherc.aquant.model.vo.stockindicator.GrowthMetricsPageReqVO;
import com.brotherc.aquant.repository.indicator.StockGrowthMetricsRepository;
import com.brotherc.aquant.repository.indicator.StockPerformanceReportRepository;
import com.brotherc.aquant.repository.stock.StockQuoteRepository;
import com.brotherc.aquant.utils.StockUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockGrowthMetricsService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int SCALE = 4;

    private final StockGrowthMetricsRepository stockGrowthMetricsRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockPerformanceReportRepository stockPerformanceReportRepository;

    public Page<StockGrowthMetrics> pageQuery(GrowthMetricsPageReqVO reqVO, Pageable pageable) {
        Specification<StockGrowthMetrics> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 股票代码等值查询
            if (reqVO != null) {
                if (StringUtils.isNotBlank(reqVO.getStockCode())) {
                    predicates.add(cb.equal(root.get("stockCode"), reqVO.getStockCode().trim()));
                }

                // EPS 3年复合增长率范围
                if (reqVO.getEpsGrowth3yCagrMin() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("epsGrowth3yCagr"), reqVO.getEpsGrowth3yCagrMin()));
                }
                if (reqVO.getEpsGrowth3yCagrMax() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("epsGrowth3yCagr"), reqVO.getEpsGrowth3yCagrMax()));
                }

                // 营收增长率(TTM)范围
                if (reqVO.getRevenueGrowthTtmMin() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("revenueGrowthTtm"), reqVO.getRevenueGrowthTtmMin()));
                }
                if (reqVO.getRevenueGrowthTtmMax() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("revenueGrowthTtm"), reqVO.getRevenueGrowthTtmMax()));
                }

                // 净利润增长率(TTM)范围
                if (reqVO.getNetProfitGrowthTtmMin() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("netProfitGrowthTtm"), reqVO.getNetProfitGrowthTtmMin()));
                }
                if (reqVO.getNetProfitGrowthTtmMax() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("netProfitGrowthTtm"), reqVO.getNetProfitGrowthTtmMax()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        if (pageable.getSort().isUnsorted()) {
            // 默认按 EPS 3年复合增长率排名升序排序
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "epsGrowth3yCagrRank"));
        }

        return stockGrowthMetricsRepository.findAll(spec, pageable);
    }

    /**
     * 刷新全量股票成长性指标数据并离线落库
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshGrowthMetrics() {
        log.info("开始计算并离线落库股票成长性指标数据...");
        List<StockGrowthMetrics> calculatedList = calculateGrowthMetrics();
        if (CollectionUtils.isEmpty(calculatedList)) {
            log.info("计算得到的成长性指标列表为空，跳过落库。");
            return;
        }

        Map<String, StockGrowthMetrics> existingMap = stockGrowthMetricsRepository.findAll().stream()
                .filter(item -> StringUtils.isNotBlank(item.getStockCode()))
                .collect(Collectors.toMap(StockGrowthMetrics::getStockCode, Function.identity(), (a, b) -> a));

        List<StockGrowthMetrics> toSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (StockGrowthMetrics calculated : calculatedList) {
            StockGrowthMetrics entity = existingMap.getOrDefault(calculated.getStockCode(), new StockGrowthMetrics());
            copyProperties(calculated, entity);
            entity.setCreatedAt(now);
            toSave.add(entity);
        }

        stockGrowthMetricsRepository.saveAll(toSave);
        log.info("股票成长性指标数据离线落库完成，共更新 {} 条数据。", toSave.size());
    }

    /**
     * 核心计算：计算每股收益、营收、净利润的 TTM、去年实际及 3年复合增长率
     */
    public List<StockGrowthMetrics> calculateGrowthMetrics() {
        List<StockPerformanceReport> performanceReports = stockPerformanceReportRepository.findAll();
        List<StockQuote> stockQuotes = stockQuoteRepository.findAll();

        if (CollectionUtils.isEmpty(stockQuotes) || CollectionUtils.isEmpty(performanceReports)) {
            log.warn("股票行情或业绩报表为空，跳过成长性指标计算。");
            return List.of();
        }

        Map<String, List<StockPerformanceReport>> performanceReportMap = performanceReports.stream()
                .filter(report -> report != null && StringUtils.isNotBlank(report.getStockCode()) && report.getReportDate() != null)
                .collect(Collectors.groupingBy(StockPerformanceReport::getStockCode));

        List<StockGrowthMetrics> resultList = new ArrayList<>();

        for (StockQuote quote : stockQuotes) {
            if (quote == null || StringUtils.isBlank(quote.getCode())) {
                continue;
            }
            String plainCode = StockUtils.getPlainCode(quote.getCode());
            List<StockPerformanceReport> reports = performanceReportMap.get(plainCode);
            if (CollectionUtils.isEmpty(reports)) {
                continue;
            }

            StockPerformanceReport latestReport = findLatestReport(reports);
            if (latestReport == null) {
                continue;
            }

            List<StockPerformanceReport> annualReports = findAnnualReports(reports);

            StockGrowthMetrics item = new StockGrowthMetrics();
            item.setStockCode(quote.getCode());
            item.setStockName(quote.getName());

            // 1. 去年实际增长率 (LastYA: 最新完整年报 vs 前一年年报)
            StockPerformanceReport latestAnnual = !annualReports.isEmpty() ? annualReports.get(0) : null;
            StockPerformanceReport prevAnnual = annualReports.size() > 1 ? annualReports.get(1) : null;

            if (latestAnnual != null && prevAnnual != null) {
                item.setRevenueGrowthLastYA(calculateGrowthRate(latestAnnual.getTotalRevenue(), prevAnnual.getTotalRevenue()));
                item.setNetProfitGrowthLastYA(calculateGrowthRate(latestAnnual.getNetProfit(), prevAnnual.getNetProfit()));
                item.setEpsGrowthLastYA(calculateGrowthRate(latestAnnual.getEarningsPerShare(), prevAnnual.getEarningsPerShare()));
            }

            // 2. TTM 滚动增长率 (TTM: 最新滚动4季度 vs 去年同期滚动4季度)
            BigDecimal revTtm = calculateTtmValue(reports, latestReport, StockPerformanceReport::getTotalRevenue);
            BigDecimal prevRevTtm = calculatePreviousTtmValue(reports, latestReport, StockPerformanceReport::getTotalRevenue);
            item.setRevenueGrowthTtm(calculateGrowthRate(revTtm, prevRevTtm));

            BigDecimal netProfitTtm = calculateTtmValue(reports, latestReport, StockPerformanceReport::getNetProfit);
            BigDecimal prevNetProfitTtm = calculatePreviousTtmValue(reports, latestReport, StockPerformanceReport::getNetProfit);
            item.setNetProfitGrowthTtm(calculateGrowthRate(netProfitTtm, prevNetProfitTtm));

            BigDecimal epsTtm = calculateTtmValue(reports, latestReport, StockPerformanceReport::getEarningsPerShare);
            BigDecimal prevEpsTtm = calculatePreviousTtmValue(reports, latestReport, StockPerformanceReport::getEarningsPerShare);
            item.setEpsGrowthTtm(calculateGrowthRate(epsTtm, prevEpsTtm));

            // 3. 3年复合增长率 (3yCAGR: 终期年报 vs 3年前基期年报)
            StockPerformanceReport baseAnnual = findBaseAnnualReport(annualReports, latestAnnual, 3);
            if (latestAnnual != null && baseAnnual != null) {
                item.setRevenueGrowth3yCagr(calculateCagr(latestAnnual.getTotalRevenue(), baseAnnual.getTotalRevenue(), 3));
                item.setNetProfitGrowth3yCagr(calculateCagr(latestAnnual.getNetProfit(), baseAnnual.getNetProfit(), 3));
                item.setEpsGrowth3yCagr(calculateCagr(latestAnnual.getEarningsPerShare(), baseAnnual.getEarningsPerShare(), 3));
            }

            resultList.add(item);
        }

        // 4. 行业中位数与均值聚合
        Map<String, String> industryMap = buildIndustryMap(performanceReports);
        fillIndustryMetrics(resultList, industryMap);

        // 5. 行业排名 (按 epsGrowth3yCagr 降序)
        fillIndustryRanks(resultList, industryMap);

        return resultList;
    }

    private void fillIndustryMetrics(List<StockGrowthMetrics> list, Map<String, String> industryMap) {
        Map<String, List<StockGrowthMetrics>> industryGroups = list.stream()
                .filter(item -> StringUtils.isNotBlank(getIndustry(item.getStockCode(), industryMap)))
                .collect(Collectors.groupingBy(item -> getIndustry(item.getStockCode(), industryMap)));

        Map<String, GrowthIndustryMetrics> cache = new HashMap<>();

        for (StockGrowthMetrics item : list) {
            String industry = getIndustry(item.getStockCode(), industryMap);
            if (StringUtils.isBlank(industry)) {
                continue;
            }

            GrowthIndustryMetrics metrics = cache.computeIfAbsent(industry, key -> buildIndustryMetrics(industryGroups.getOrDefault(key, List.of())));

            item.setEpsGrowth3yCagrIndustryAvg(metrics.getEpsGrowth3yCagrAvg());
            item.setEpsGrowth3yCagrIndustryMed(metrics.getEpsGrowth3yCagrMed());
            item.setEpsGrowthLastYAIndustryAvg(metrics.getEpsGrowthLastYAAvg());
            item.setEpsGrowthLastYAIndustryMed(metrics.getEpsGrowthLastYAMed());
            item.setEpsGrowthTtmIndustryAvg(metrics.getEpsGrowthTtmAvg());
            item.setEpsGrowthTtmIndustryMed(metrics.getEpsGrowthTtmMed());

            item.setRevenueGrowth3yCagrIndustryAvg(metrics.getRevenueGrowth3yCagrAvg());
            item.setRevenueGrowth3yCagrIndustryMed(metrics.getRevenueGrowth3yCagrMed());
            item.setRevenueGrowthLastYAIndustryAvg(metrics.getRevenueGrowthLastYAAvg());
            item.setRevenueGrowthLastYAIndustryMed(metrics.getRevenueGrowthLastYAMed());
            item.setRevenueGrowthTtmIndustryAvg(metrics.getRevenueGrowthTtmAvg());
            item.setRevenueGrowthTtmIndustryMed(metrics.getRevenueGrowthTtmMed());

            item.setNetProfitGrowth3yCagrIndustryAvg(metrics.getNetProfitGrowth3yCagrAvg());
            item.setNetProfitGrowth3yCagrIndustryMed(metrics.getNetProfitGrowth3yCagrMed());
            item.setNetProfitGrowthLastYAIndustryAvg(metrics.getNetProfitGrowthLastYAAvg());
            item.setNetProfitGrowthLastYAIndustryMed(metrics.getNetProfitGrowthLastYAMed());
            item.setNetProfitGrowthTtmIndustryAvg(metrics.getNetProfitGrowthTtmAvg());
            item.setNetProfitGrowthTtmIndustryMed(metrics.getNetProfitGrowthTtmMed());
        }
    }

    private void fillIndustryRanks(List<StockGrowthMetrics> list, Map<String, String> industryMap) {
        Map<String, List<StockGrowthMetrics>> industryGroups = list.stream()
                .filter(item -> StringUtils.isNotBlank(getIndustry(item.getStockCode(), industryMap)))
                .collect(Collectors.groupingBy(item -> getIndustry(item.getStockCode(), industryMap)));

        for (List<StockGrowthMetrics> group : industryGroups.values()) {
            List<StockGrowthMetrics> rankedList = group.stream()
                    .filter(item -> item.getEpsGrowth3yCagr() != null)
                    .sorted(Comparator.comparing(StockGrowthMetrics::getEpsGrowth3yCagr).reversed())
                    .toList();

            for (int i = 0; i < rankedList.size(); i++) {
                rankedList.get(i).setEpsGrowth3yCagrRank(BigDecimal.valueOf(i + 1L));
            }
        }
    }

    private GrowthIndustryMetrics buildIndustryMetrics(List<StockGrowthMetrics> group) {
        GrowthIndustryMetrics metrics = new GrowthIndustryMetrics();
        metrics.setEpsGrowth3yCagrAvg(average(group, StockGrowthMetrics::getEpsGrowth3yCagr));
        metrics.setEpsGrowth3yCagrMed(median(group, StockGrowthMetrics::getEpsGrowth3yCagr));
        metrics.setEpsGrowthLastYAAvg(average(group, StockGrowthMetrics::getEpsGrowthLastYA));
        metrics.setEpsGrowthLastYAMed(median(group, StockGrowthMetrics::getEpsGrowthLastYA));
        metrics.setEpsGrowthTtmAvg(average(group, StockGrowthMetrics::getEpsGrowthTtm));
        metrics.setEpsGrowthTtmMed(median(group, StockGrowthMetrics::getEpsGrowthTtm));

        metrics.setRevenueGrowth3yCagrAvg(average(group, StockGrowthMetrics::getRevenueGrowth3yCagr));
        metrics.setRevenueGrowth3yCagrMed(median(group, StockGrowthMetrics::getRevenueGrowth3yCagr));
        metrics.setRevenueGrowthLastYAAvg(average(group, StockGrowthMetrics::getRevenueGrowthLastYA));
        metrics.setRevenueGrowthLastYAMed(median(group, StockGrowthMetrics::getRevenueGrowthLastYA));
        metrics.setRevenueGrowthTtmAvg(average(group, StockGrowthMetrics::getRevenueGrowthTtm));
        metrics.setRevenueGrowthTtmMed(median(group, StockGrowthMetrics::getRevenueGrowthTtm));

        metrics.setNetProfitGrowth3yCagrAvg(average(group, StockGrowthMetrics::getNetProfitGrowth3yCagr));
        metrics.setNetProfitGrowth3yCagrMed(median(group, StockGrowthMetrics::getNetProfitGrowth3yCagr));
        metrics.setNetProfitGrowthLastYAAvg(average(group, StockGrowthMetrics::getNetProfitGrowthLastYA));
        metrics.setNetProfitGrowthLastYAMed(median(group, StockGrowthMetrics::getNetProfitGrowthLastYA));
        metrics.setNetProfitGrowthTtmAvg(average(group, StockGrowthMetrics::getNetProfitGrowthTtm));
        metrics.setNetProfitGrowthTtmMed(median(group, StockGrowthMetrics::getNetProfitGrowthTtm));

        return metrics;
    }

    private BigDecimal average(List<StockGrowthMetrics> list, Function<StockGrowthMetrics, BigDecimal> getter) {
        List<BigDecimal> values = list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal median(List<StockGrowthMetrics> list, Function<StockGrowthMetrics, BigDecimal> getter) {
        List<BigDecimal> values = list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        int size = values.size();
        if (size % 2 == 1) {
            return values.get(size / 2);
        }
        BigDecimal mid1 = values.get(size / 2 - 1);
        BigDecimal mid2 = values.get(size / 2);
        return mid1.add(mid2).divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(ONE_HUNDRED)
                .divide(previous.abs(), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCagr(BigDecimal finalValue, BigDecimal baseValue, int years) {
        if (finalValue == null || baseValue == null || years <= 0) {
            return null;
        }
        // 金融标准：基期或终期为非正数（亏损）时，无法计算有意义的复合增长率
        if (finalValue.compareTo(BigDecimal.ZERO) <= 0 || baseValue.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        double ratio = finalValue.doubleValue() / baseValue.doubleValue();
        double cagr = Math.pow(ratio, 1.0 / years) - 1.0;
        return BigDecimal.valueOf(cagr * 100).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private StockPerformanceReport findLatestReport(List<StockPerformanceReport> reports) {
        return reports.stream()
                .max(Comparator.comparing(StockPerformanceReport::getReportDate))
                .orElse(null);
    }

    private List<StockPerformanceReport> findAnnualReports(List<StockPerformanceReport> reports) {
        return reports.stream()
                .filter(r -> r.getReportDate() != null && StockUtils.isAnnualReport(r.getReportDate()))
                .sorted(Comparator.comparing(StockPerformanceReport::getReportDate).reversed())
                .toList();
    }

    private StockPerformanceReport findBaseAnnualReport(List<StockPerformanceReport> annualReports, StockPerformanceReport latestAnnual, int yearDiff) {
        if (latestAnnual == null || CollectionUtils.isEmpty(annualReports)) {
            return null;
        }
        int targetYear = latestAnnual.getReportDate().getYear() - yearDiff;
        for (StockPerformanceReport r : annualReports) {
            if (r.getReportDate() != null && r.getReportDate().getYear() == targetYear) {
                return r;
            }
        }
        return null;
    }

    private BigDecimal calculateTtmValue(
            List<StockPerformanceReport> reports, StockPerformanceReport latestReport,
            Function<StockPerformanceReport, BigDecimal> valueGetter
    ) {
        LocalDate latestReportDate = latestReport.getReportDate();
        if (latestReportDate.getMonth() == Month.DECEMBER) {
            return valueGetter.apply(latestReport);
        }

        StockPerformanceReport previousAnnualReport = findByReportDate(reports, LocalDate.of(latestReportDate.getYear() - 1, Month.DECEMBER, 31));
        StockPerformanceReport samePeriodLastYearReport = findByReportDate(reports, latestReportDate.minusYears(1));
        if (previousAnnualReport == null || samePeriodLastYearReport == null) {
            return null;
        }

        BigDecimal latestValue = valueGetter.apply(latestReport);
        BigDecimal previousAnnualValue = valueGetter.apply(previousAnnualReport);
        BigDecimal samePeriodLastYearValue = valueGetter.apply(samePeriodLastYearReport);
        if (latestValue == null || previousAnnualValue == null || samePeriodLastYearValue == null) {
            return null;
        }
        return latestValue.add(previousAnnualValue).subtract(samePeriodLastYearValue);
    }

    private BigDecimal calculatePreviousTtmValue(
            List<StockPerformanceReport> reports, StockPerformanceReport latestReport,
            Function<StockPerformanceReport, BigDecimal> valueGetter
    ) {
        StockPerformanceReport samePeriodLastYearReport = findByReportDate(reports, latestReport.getReportDate().minusYears(1));
        if (samePeriodLastYearReport == null) {
            return null;
        }
        return calculateTtmValue(reports, samePeriodLastYearReport, valueGetter);
    }

    private StockPerformanceReport findByReportDate(List<StockPerformanceReport> reports, LocalDate reportDate) {
        for (StockPerformanceReport report : reports) {
            if (reportDate.equals(report.getReportDate())) {
                return report;
            }
        }
        return null;
    }

    private Map<String, String> buildIndustryMap(List<StockPerformanceReport> performanceReports) {
        return performanceReports.stream()
                .filter(report -> report != null && StringUtils.isNotBlank(report.getStockCode()))
                .filter(report -> StringUtils.isNotBlank(report.getIndustry()) && report.getReportDate() != null)
                .collect(Collectors.toMap(
                        StockPerformanceReport::getStockCode,
                        Function.identity(),
                        (left, right) -> left.getReportDate().isBefore(right.getReportDate()) ? right : left
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getIndustry()
                ));
    }

    private String getIndustry(String stockCode, Map<String, String> industryMap) {
        if (StringUtils.isBlank(stockCode)) {
            return null;
        }
        String plainCode = StockUtils.getPlainCode(stockCode);
        return industryMap.get(plainCode);
    }

    private void copyProperties(StockGrowthMetrics source, StockGrowthMetrics target) {
        target.setStockCode(source.getStockCode());
        target.setStockName(source.getStockName());

        target.setEpsGrowth3yCagr(source.getEpsGrowth3yCagr());
        target.setEpsGrowth3yCagrIndustryMed(source.getEpsGrowth3yCagrIndustryMed());
        target.setEpsGrowth3yCagrIndustryAvg(source.getEpsGrowth3yCagrIndustryAvg());
        target.setEpsGrowth3yCagrRank(source.getEpsGrowth3yCagrRank());

        target.setEpsGrowthLastYA(source.getEpsGrowthLastYA());
        target.setEpsGrowthLastYAIndustryMed(source.getEpsGrowthLastYAIndustryMed());
        target.setEpsGrowthLastYAIndustryAvg(source.getEpsGrowthLastYAIndustryAvg());

        target.setEpsGrowthTtm(source.getEpsGrowthTtm());
        target.setEpsGrowthTtmIndustryMed(source.getEpsGrowthTtmIndustryMed());
        target.setEpsGrowthTtmIndustryAvg(source.getEpsGrowthTtmIndustryAvg());

        target.setRevenueGrowth3yCagr(source.getRevenueGrowth3yCagr());
        target.setRevenueGrowth3yCagrIndustryMed(source.getRevenueGrowth3yCagrIndustryMed());
        target.setRevenueGrowth3yCagrIndustryAvg(source.getRevenueGrowth3yCagrIndustryAvg());

        target.setRevenueGrowthLastYA(source.getRevenueGrowthLastYA());
        target.setRevenueGrowthLastYAIndustryMed(source.getRevenueGrowthLastYAIndustryMed());
        target.setRevenueGrowthLastYAIndustryAvg(source.getRevenueGrowthLastYAIndustryAvg());

        target.setRevenueGrowthTtm(source.getRevenueGrowthTtm());
        target.setRevenueGrowthTtmIndustryMed(source.getRevenueGrowthTtmIndustryMed());
        target.setRevenueGrowthTtmIndustryAvg(source.getRevenueGrowthTtmIndustryAvg());

        target.setNetProfitGrowth3yCagr(source.getNetProfitGrowth3yCagr());
        target.setNetProfitGrowth3yCagrIndustryMed(source.getNetProfitGrowth3yCagrIndustryMed());
        target.setNetProfitGrowth3yCagrIndustryAvg(source.getNetProfitGrowth3yCagrIndustryAvg());

        target.setNetProfitGrowthLastYA(source.getNetProfitGrowthLastYA());
        target.setNetProfitGrowthLastYAIndustryMed(source.getNetProfitGrowthLastYAIndustryMed());
        target.setNetProfitGrowthLastYAIndustryAvg(source.getNetProfitGrowthLastYAIndustryAvg());

        target.setNetProfitGrowthTtm(source.getNetProfitGrowthTtm());
        target.setNetProfitGrowthTtmIndustryMed(source.getNetProfitGrowthTtmIndustryMed());
        target.setNetProfitGrowthTtmIndustryAvg(source.getNetProfitGrowthTtmIndustryAvg());
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(String code, String name, List<StockZhGrowthComparisonEm> list) {
        StockGrowthMetrics stockGrowthMetrics = stockGrowthMetricsRepository.findByStockCode(code);

        if (stockGrowthMetrics == null) {
            stockGrowthMetrics = new StockGrowthMetrics();
        }
        stockGrowthMetrics.setStockCode(code);
        stockGrowthMetrics.setStockName(name);

        String plainCode = StockUtils.getPlainCode(code);

        for (StockZhGrowthComparisonEm data : list) {
            String c = data.getCode();
            if (plainCode.equals(c)) {
                stockGrowthMetrics.setEpsGrowth3yCagr(data.getEpsGrowth3yCagr());
                stockGrowthMetrics.setEpsGrowthLastYA(data.getEpsGrowth24a());
                stockGrowthMetrics.setEpsGrowthTtm(data.getEpsGrowthTtm());
                stockGrowthMetrics.setEpsGrowthThisYE(data.getEpsGrowth25e());
                stockGrowthMetrics.setEpsGrowthNextYE(data.getEpsGrowth26e());
                stockGrowthMetrics.setEpsGrowthNext2YE(data.getEpsGrowth27e());
                stockGrowthMetrics.setEpsGrowth3yCagrRank(data.getEpsGrowth3yCagrRank());

                stockGrowthMetrics.setRevenueGrowth3yCagr(data.getRevenueGrowth3yCagr());
                stockGrowthMetrics.setRevenueGrowthLastYA(data.getRevenueGrowth24a());
                stockGrowthMetrics.setRevenueGrowthTtm(data.getRevenueGrowthTtm());
                stockGrowthMetrics.setRevenueGrowthThisYE(data.getRevenueGrowth25e());
                stockGrowthMetrics.setRevenueGrowthNextYE(data.getRevenueGrowth26e());
                stockGrowthMetrics.setRevenueGrowthNext2YE(data.getRevenueGrowth27e());

                stockGrowthMetrics.setNetProfitGrowth3yCagr(data.getNetProfitGrowth3yCagr());
                stockGrowthMetrics.setNetProfitGrowthLastYA(data.getNetProfitGrowth24a());
                stockGrowthMetrics.setNetProfitGrowthTtm(data.getNetProfitGrowthTtm());
                stockGrowthMetrics.setNetProfitGrowthThisYE(data.getNetProfitGrowth25e());
                stockGrowthMetrics.setNetProfitGrowthNextYE(data.getNetProfitGrowth26e());
                stockGrowthMetrics.setNetProfitGrowthNext2YE(data.getNetProfitGrowth27e());
                stockGrowthMetrics.setCreatedAt(LocalDateTime.now());
            } else if ("行业中值".equals(c)) {
                stockGrowthMetrics.setEpsGrowth3yCagrIndustryMed(data.getEpsGrowth3yCagr());
                stockGrowthMetrics.setEpsGrowthLastYAIndustryMed(data.getEpsGrowth24a());
                stockGrowthMetrics.setEpsGrowthTtmIndustryMed(data.getEpsGrowthTtm());
                stockGrowthMetrics.setEpsGrowthThisYEIndustryMed(data.getEpsGrowth25e());
                stockGrowthMetrics.setEpsGrowthNextYEIndustryMed(data.getEpsGrowth26e());
                stockGrowthMetrics.setEpsGrowthNext2YEIndustryMed(data.getEpsGrowth27e());
                stockGrowthMetrics.setEpsGrowth3yCagrRankIndustryMed(data.getEpsGrowth3yCagrRank());

                stockGrowthMetrics.setRevenueGrowth3yCagrIndustryMed(data.getRevenueGrowth3yCagr());
                stockGrowthMetrics.setRevenueGrowthLastYAIndustryMed(data.getRevenueGrowth24a());
                stockGrowthMetrics.setRevenueGrowthTtmIndustryMed(data.getRevenueGrowthTtm());
                stockGrowthMetrics.setRevenueGrowthThisYEIndustryMed(data.getRevenueGrowth25e());
                stockGrowthMetrics.setRevenueGrowthNextYEIndustryMed(data.getRevenueGrowth26e());
                stockGrowthMetrics.setRevenueGrowthNext2YEIndustryMed(data.getRevenueGrowth27e());

                stockGrowthMetrics.setNetProfitGrowth3yCagrIndustryMed(data.getNetProfitGrowth3yCagr());
                stockGrowthMetrics.setNetProfitGrowthLastYAIndustryMed(data.getNetProfitGrowth24a());
                stockGrowthMetrics.setNetProfitGrowthTtmIndustryMed(data.getNetProfitGrowthTtm());
                stockGrowthMetrics.setNetProfitGrowthThisYEIndustryMed(data.getNetProfitGrowth25e());
                stockGrowthMetrics.setNetProfitGrowthNextYEIndustryMed(data.getNetProfitGrowth26e());
                stockGrowthMetrics.setNetProfitGrowthNext2YEIndustryMed(data.getNetProfitGrowth27e());
                stockGrowthMetrics.setCreatedAt(LocalDateTime.now());
            } else if ("行业平均".equals(c)) {
                stockGrowthMetrics.setEpsGrowth3yCagrIndustryAvg(data.getEpsGrowth3yCagr());
                stockGrowthMetrics.setEpsGrowthLastYAIndustryAvg(data.getEpsGrowth24a());
                stockGrowthMetrics.setEpsGrowthTtmIndustryAvg(data.getEpsGrowthTtm());
                stockGrowthMetrics.setEpsGrowthThisYEIndustryAvg(data.getEpsGrowth25e());
                stockGrowthMetrics.setEpsGrowthNextYEIndustryAvg(data.getEpsGrowth26e());
                stockGrowthMetrics.setEpsGrowthNext2YEIndustryAvg(data.getEpsGrowth27e());
                stockGrowthMetrics.setEpsGrowth3yCagrRankIndustryAvg(data.getEpsGrowth3yCagrRank());

                stockGrowthMetrics.setRevenueGrowth3yCagrIndustryAvg(data.getRevenueGrowth3yCagr());
                stockGrowthMetrics.setRevenueGrowthLastYAIndustryAvg(data.getRevenueGrowth24a());
                stockGrowthMetrics.setRevenueGrowthTtmIndustryAvg(data.getRevenueGrowthTtm());
                stockGrowthMetrics.setRevenueGrowthThisYEIndustryAvg(data.getRevenueGrowth25e());
                stockGrowthMetrics.setRevenueGrowthNextYEIndustryAvg(data.getRevenueGrowth26e());
                stockGrowthMetrics.setRevenueGrowthNext2YEIndustryAvg(data.getRevenueGrowth27e());

                stockGrowthMetrics.setNetProfitGrowth3yCagrIndustryAvg(data.getNetProfitGrowth3yCagr());
                stockGrowthMetrics.setNetProfitGrowthLastYAIndustryAvg(data.getNetProfitGrowth24a());
                stockGrowthMetrics.setNetProfitGrowthTtmIndustryAvg(data.getNetProfitGrowthTtm());
                stockGrowthMetrics.setNetProfitGrowthThisYEIndustryAvg(data.getNetProfitGrowth25e());
                stockGrowthMetrics.setNetProfitGrowthNextYEIndustryAvg(data.getNetProfitGrowth26e());
                stockGrowthMetrics.setNetProfitGrowthNext2YEIndustryAvg(data.getNetProfitGrowth27e());
                stockGrowthMetrics.setCreatedAt(LocalDateTime.now());
            }
        }
        stockGrowthMetricsRepository.save(stockGrowthMetrics);
    }

}
