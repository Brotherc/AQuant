package com.brotherc.aquant.service.indicator;

import com.brotherc.aquant.entity.indicator.StockPerformanceReport;
import com.brotherc.aquant.entity.stock.StockQuote;
import com.brotherc.aquant.entity.stock.StockShareChange;
import com.brotherc.aquant.entity.indicator.StockValuationMetrics;
import com.brotherc.aquant.model.dto.akshare.StockZhValuationComparisonEm;
import com.brotherc.aquant.model.vo.stockindicator.CalculatedValuationMetricsPageVO;
import com.brotherc.aquant.model.vo.stockindicator.CalculatedValuationMetricsVO;
import com.brotherc.aquant.model.vo.stockindicator.ValuationMetricsPageReqVO;
import com.brotherc.aquant.repository.indicator.StockPerformanceReportRepository;
import com.brotherc.aquant.repository.stock.StockQuoteRepository;
import com.brotherc.aquant.repository.stock.StockShareChangeRepository;
import com.brotherc.aquant.repository.indicator.StockValuationMetricsRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockValuationMetricsService {

    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");
    private static final int SCALE = 6;

    private final StockValuationMetricsRepository stockValuationMetricsRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockPerformanceReportRepository stockPerformanceReportRepository;
    private final StockShareChangeRepository stockShareChangeRepository;

    public Page<CalculatedValuationMetricsPageVO> pageQuery(ValuationMetricsPageReqVO reqVO, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "peg"));
        }

        Specification<StockValuationMetrics> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (reqVO != null) {
                if (StringUtils.isNotBlank(reqVO.getStockCode())) {
                    String keyword = "%" + reqVO.getStockCode().trim() + "%";
                    predicates.add(cb.or(
                            cb.like(root.get("stockCode"), keyword),
                            cb.like(root.get("stockName"), keyword)
                    ));
                }

                if (reqVO.getPegMin() != null) {
                    predicates.add(cb.ge(root.get("peg"), reqVO.getPegMin()));
                }
                if (reqVO.getPegMax() != null) {
                    predicates.add(cb.le(root.get("peg"), reqVO.getPegMax()));
                }

                if (reqVO.getPeTtmMin() != null) {
                    predicates.add(cb.ge(root.get("peTtm"), reqVO.getPeTtmMin()));
                }
                if (reqVO.getPeTtmMax() != null) {
                    predicates.add(cb.le(root.get("peTtm"), reqVO.getPeTtmMax()));
                }

                if (reqVO.getPsTtmMin() != null) {
                    predicates.add(cb.ge(root.get("psTtm"), reqVO.getPsTtmMin()));
                }
                if (reqVO.getPsTtmMax() != null) {
                    predicates.add(cb.le(root.get("psTtm"), reqVO.getPsTtmMax()));
                }

                if (reqVO.getPbMrqMin() != null) {
                    predicates.add(cb.ge(root.get("pbMrq"), reqVO.getPbMrqMin()));
                }
                if (reqVO.getPbMrqMax() != null) {
                    predicates.add(cb.le(root.get("pbMrq"), reqVO.getPbMrqMax()));
                }

                if (reqVO.getPcfTtmMin() != null) {
                    predicates.add(cb.ge(root.get("pcfTtm"), reqVO.getPcfTtmMin()));
                }
                if (reqVO.getPcfTtmMax() != null) {
                    predicates.add(cb.le(root.get("pcfTtm"), reqVO.getPcfTtmMax()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<StockValuationMetrics> pageResult = stockValuationMetricsRepository.findAll(specification, pageable);
        return pageResult.map(this::toPageVO);
    }

    public CalculatedValuationMetricsVO detail(String stockCode) {
        if (StringUtils.isBlank(stockCode)) {
            return null;
        }
        StockValuationMetrics entity = stockValuationMetricsRepository.findByStockCode(stockCode);
        if (entity == null && stockCode.length() > 2) {
            entity = stockValuationMetricsRepository.findByStockCode(stockCode.substring(2));
        }
        if (entity == null) {
            return null;
        }
        return toDetailVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshValuationMetrics() {
        log.info("开始计算并离线落库股票估值指标数据...");
        List<CalculatedValuationMetricsVO> metricsList = calculateValuationMetrics();
        if (CollectionUtils.isEmpty(metricsList)) {
            log.info("计算得到的估值指标列表为空，跳过落库。");
            return;
        }
        fillIndustryMetrics(metricsList);

        Map<String, StockValuationMetrics> existingMap = stockValuationMetricsRepository.findAll().stream()
                .filter(item -> StringUtils.isNotBlank(item.getStockCode()))
                .collect(Collectors.toMap(StockValuationMetrics::getStockCode, Function.identity(), (a, b) -> a));

        List<StockValuationMetrics> toSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (CalculatedValuationMetricsVO vo : metricsList) {
            StockValuationMetrics entity = existingMap.getOrDefault(vo.getStockCode(), new StockValuationMetrics());
            entity.setStockCode(vo.getStockCode());
            entity.setStockName(vo.getStockName());
            entity.setPeg(vo.getPeg());
            entity.setPegIndustryMed(vo.getPegIndustryMedian());
            entity.setPegIndustryAvg(vo.getPegIndustryAverage());

            entity.setPeTtm(vo.getPeTtm());
            entity.setPeTtmIndustryMed(vo.getPeTtmIndustryMedian());
            entity.setPeTtmIndustryAvg(vo.getPeTtmIndustryAverage());

            entity.setPeLastYearA(vo.getPeAnnual());
            entity.setPeLastYearIndustryMed(vo.getPeAnnualIndustryMedian());
            entity.setPeLastYearIndustryAvg(vo.getPeAnnualIndustryAverage());

            entity.setPsTtm(vo.getPsTtm());
            entity.setPsTtmIndustryMed(vo.getPsTtmIndustryMedian());
            entity.setPsTtmIndustryAvg(vo.getPsTtmIndustryAverage());

            entity.setPsLastYA(vo.getPsAnnual());
            entity.setPsLastYAIndustryMed(vo.getPsAnnualIndustryMedian());
            entity.setPsLastYAIndustryAvg(vo.getPsAnnualIndustryAverage());

            entity.setPbMrq(vo.getPbMrq());
            entity.setPbMrqIndustryMed(vo.getPbMrqIndustryMedian());
            entity.setPbMrqIndustryAvg(vo.getPbMrqIndustryAverage());

            entity.setPbLastYA(vo.getPbAnnual());
            entity.setPbLastYAIndustryMed(vo.getPbAnnualIndustryMedian());
            entity.setPbLastYAIndustryAvg(vo.getPbAnnualIndustryAverage());

            entity.setPcfTtm(vo.getPcfTtm());
            entity.setPcfTtmIndustryMed(vo.getPcfTtmIndustryMedian());
            entity.setPcfTtmIndustryAvg(vo.getPcfTtmIndustryAverage());

            entity.setPcfLastYA(vo.getPcfAnnual());
            entity.setPcfLastYAIndustryMed(vo.getPcfAnnualIndustryMedian());
            entity.setPcfLastYAIndustryAvg(vo.getPcfAnnualIndustryAverage());

            entity.setCreatedAt(now);
            toSave.add(entity);
        }

        stockValuationMetricsRepository.saveAll(toSave);
        log.info("股票估值指标数据离线落库完成，共更新 {} 条数据。", toSave.size());
    }

    private CalculatedValuationMetricsPageVO toPageVO(StockValuationMetrics entity) {
        CalculatedValuationMetricsPageVO pageVO = new CalculatedValuationMetricsPageVO();
        pageVO.setId(entity.getId());
        pageVO.setStockCode(entity.getStockCode());
        pageVO.setStockName(entity.getStockName());
        pageVO.setPeg(entity.getPeg());
        pageVO.setPeTtm(entity.getPeTtm());
        pageVO.setPeAnnual(entity.getPeLastYearA());
        pageVO.setPsTtm(entity.getPsTtm());
        pageVO.setPsAnnual(entity.getPsLastYA());
        pageVO.setPbMrq(entity.getPbMrq());
        pageVO.setPbAnnual(entity.getPbLastYA());
        pageVO.setPcfTtm(entity.getPcfTtm());
        pageVO.setPcfAnnual(entity.getPcfLastYA());
        pageVO.setCalculatedAt(entity.getCreatedAt());
        return pageVO;
    }

    private CalculatedValuationMetricsVO toDetailVO(StockValuationMetrics entity) {
        CalculatedValuationMetricsVO vo = new CalculatedValuationMetricsVO();
        vo.setId(entity.getId());
        vo.setStockCode(entity.getStockCode());
        vo.setStockName(entity.getStockName());

        vo.setPeg(entity.getPeg());
        vo.setPegIndustryMedian(entity.getPegIndustryMed());
        vo.setPegIndustryAverage(entity.getPegIndustryAvg());

        vo.setPeTtm(entity.getPeTtm());
        vo.setPeTtmIndustryMedian(entity.getPeTtmIndustryMed());
        vo.setPeTtmIndustryAverage(entity.getPeTtmIndustryAvg());

        vo.setPeAnnual(entity.getPeLastYearA());
        vo.setPeAnnualIndustryMedian(entity.getPeLastYearIndustryMed());
        vo.setPeAnnualIndustryAverage(entity.getPeLastYearIndustryAvg());

        vo.setPsTtm(entity.getPsTtm());
        vo.setPsTtmIndustryMedian(entity.getPsTtmIndustryMed());
        vo.setPsTtmIndustryAverage(entity.getPsTtmIndustryAvg());

        vo.setPsAnnual(entity.getPsLastYA());
        vo.setPsAnnualIndustryMedian(entity.getPsLastYAIndustryMed());
        vo.setPsAnnualIndustryAverage(entity.getPsLastYAIndustryAvg());

        vo.setPbMrq(entity.getPbMrq());
        vo.setPbMrqIndustryMedian(entity.getPbMrqIndustryMed());
        vo.setPbMrqIndustryAverage(entity.getPbMrqIndustryAvg());

        vo.setPbAnnual(entity.getPbLastYA());
        vo.setPbAnnualIndustryMedian(entity.getPbLastYAIndustryMed());
        vo.setPbAnnualIndustryAverage(entity.getPbLastYAIndustryAvg());

        vo.setPcfTtm(entity.getPcfTtm());
        vo.setPcfTtmIndustryMedian(entity.getPcfTtmIndustryMed());
        vo.setPcfTtmIndustryAverage(entity.getPcfTtmIndustryAvg());

        vo.setPcfAnnual(entity.getPcfLastYA());
        vo.setPcfAnnualIndustryMedian(entity.getPcfLastYAIndustryMed());
        vo.setPcfAnnualIndustryAverage(entity.getPcfLastYAIndustryAvg());

        vo.setCalculatedAt(entity.getCreatedAt());
        return vo;
    }

    private List<CalculatedValuationMetricsVO> calculateValuationMetrics() {
        Map<String, List<StockPerformanceReport>> performanceReportMap = stockPerformanceReportRepository.findAll().stream()
                .filter(report -> report != null && StringUtils.isNotBlank(report.getStockCode()) && report.getReportDate() != null)
                .collect(Collectors.groupingBy(StockPerformanceReport::getStockCode));
        if (CollectionUtils.isEmpty(performanceReportMap)) {
            return List.of();
        }

        Map<String, StockShareChange> latestShareChangeMap = stockShareChangeRepository.findAll().stream()
                .filter(shareChange -> shareChange != null && StringUtils.isNotBlank(shareChange.getStockCode()))
                .filter(shareChange -> shareChange.getChangeDate() != null && shareChange.getTotalShares10k() != null)
                .filter(shareChange -> !shareChange.getChangeDate().isAfter(LocalDate.now()))
                .collect(Collectors.toMap(
                        StockShareChange::getStockCode,
                        Function.identity(),
                        (left, right) -> left.getChangeDate().isBefore(right.getChangeDate()) ? right : left
                ));

        List<CalculatedValuationMetricsVO> result = new ArrayList<>();
        for (StockQuote stockQuote : stockQuoteRepository.findAll()) {
            if (stockQuote != null && StringUtils.isNotBlank(stockQuote.getCode()) && stockQuote.getLatestPrice() != null) {
                String plainCode = stockQuote.getCode().length() > 2 ? stockQuote.getCode().substring(2) : stockQuote.getCode();
                List<StockPerformanceReport> reports = performanceReportMap.get(plainCode);
                StockPerformanceReport latestReport = CollectionUtils.isEmpty(reports) ? null : findLatestReport(reports);

                if (latestReport != null) {
                    StockPerformanceReport annualReport = findLatestAnnualReport(reports);
                    BigDecimal epsTtm = calculateTtmValue(reports, latestReport, StockPerformanceReport::getEarningsPerShare);
                    BigDecimal revenueTtm = calculateTtmValue(reports, latestReport, StockPerformanceReport::getTotalRevenue);
                    BigDecimal netProfitTtm = calculateTtmValue(reports, latestReport, StockPerformanceReport::getNetProfit);
                    BigDecimal previousNetProfitTtm = calculatePreviousTtmValue(reports, latestReport, StockPerformanceReport::getNetProfit);
                    BigDecimal ocfTtm = calculateTtmValue(reports, latestReport, StockPerformanceReport::getOperatingCashFlowPerShare);
                    StockShareChange latestShareChange = latestShareChangeMap.get(stockQuote.getCode());

                    CalculatedValuationMetricsVO metrics = new CalculatedValuationMetricsVO();
                    metrics.setId(stockQuote.getId());
                    metrics.setStockCode(stockQuote.getCode());
                    metrics.setStockName(stockQuote.getName());
                    metrics.setCalculatedAt(LocalDateTime.now());

                    metrics.setPeTtm(divide(stockQuote.getLatestPrice(), epsTtm));
                    metrics.setPeAnnual(annualReport == null ? null : divide(stockQuote.getLatestPrice(), annualReport.getEarningsPerShare()));
                    metrics.setPbMrq(divide(stockQuote.getLatestPrice(), latestReport.getNetAssetsPerShare()));
                    metrics.setPbAnnual(annualReport == null ? null : divide(stockQuote.getLatestPrice(), annualReport.getNetAssetsPerShare()));

                    BigDecimal marketCap = latestShareChange == null ? null :
                            multiply(stockQuote.getLatestPrice(), multiply(latestShareChange.getTotalShares10k(), TEN_THOUSAND));
                    metrics.setPsTtm(divide(marketCap, revenueTtm));
                    metrics.setPsAnnual(annualReport == null ? null : divide(marketCap, annualReport.getTotalRevenue()));

                    metrics.setPcfTtm(divide(stockQuote.getLatestPrice(), ocfTtm));
                    metrics.setPcfAnnual(annualReport == null ? null : divide(stockQuote.getLatestPrice(), annualReport.getOperatingCashFlowPerShare()));

                    metrics.setPeg(calculatePeg(metrics.getPeTtm(), netProfitTtm, previousNetProfitTtm));
                    result.add(metrics);
                }
            }
        }
        return result;
    }

    private StockPerformanceReport findLatestReport(List<StockPerformanceReport> reports) {
        return reports.stream()
                .max(Comparator.comparing(StockPerformanceReport::getReportDate))
                .orElse(null);
    }

    private StockPerformanceReport findLatestAnnualReport(List<StockPerformanceReport> reports) {
        return reports.stream()
                .filter(report -> report.getReportDate().getMonth() == Month.DECEMBER)
                .max(Comparator.comparing(StockPerformanceReport::getReportDate))
                .orElse(null);
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

    private BigDecimal calculatePeg(BigDecimal peTtm, BigDecimal netProfitTtm, BigDecimal previousNetProfitTtm) {
        if (peTtm == null || netProfitTtm == null || previousNetProfitTtm == null || previousNetProfitTtm.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal growthRate = netProfitTtm.subtract(previousNetProfitTtm)
                .divide(previousNetProfitTtm.abs(), SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        if (growthRate.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return divide(peTtm, growthRate);
    }

    private void fillIndustryMetrics(List<CalculatedValuationMetricsVO> metricsList) {
        Map<String, String> industryMap = stockPerformanceReportRepository.findAll().stream()
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
        Map<String, List<CalculatedValuationMetricsVO>> industryMetricsMap = metricsList.stream()
                .filter(metrics -> StringUtils.isNotBlank(getIndustry(metrics.getStockCode(), industryMap)))
                .collect(Collectors.groupingBy(metrics -> getIndustry(metrics.getStockCode(), industryMap)));
        Map<String, IndustryMetrics> industryMetricsCache = new HashMap<>();
        for (CalculatedValuationMetricsVO metrics : metricsList) {
            String industry = getIndustry(metrics.getStockCode(), industryMap);
            if (StringUtils.isBlank(industry)) {
                continue;
            }
            IndustryMetrics industryMetrics = industryMetricsCache.computeIfAbsent(industry,
                    key -> buildIndustryMetrics(industryMetricsMap.getOrDefault(key, List.of())));
            metrics.setPegIndustryAverage(industryMetrics.pegAvg);
            metrics.setPegIndustryMedian(industryMetrics.pegMed);
            metrics.setPeTtmIndustryAverage(industryMetrics.peTtmAvg);
            metrics.setPeTtmIndustryMedian(industryMetrics.peTtmMed);
            metrics.setPeAnnualIndustryAverage(industryMetrics.peAnnualAvg);
            metrics.setPeAnnualIndustryMedian(industryMetrics.peAnnualMed);
            metrics.setPsTtmIndustryAverage(industryMetrics.psTtmAvg);
            metrics.setPsTtmIndustryMedian(industryMetrics.psTtmMed);
            metrics.setPsAnnualIndustryAverage(industryMetrics.psAnnualAvg);
            metrics.setPsAnnualIndustryMedian(industryMetrics.psAnnualMed);
            metrics.setPbMrqIndustryAverage(industryMetrics.pbMrqAvg);
            metrics.setPbMrqIndustryMedian(industryMetrics.pbMrqMed);
            metrics.setPbAnnualIndustryAverage(industryMetrics.pbAnnualAvg);
            metrics.setPbAnnualIndustryMedian(industryMetrics.pbAnnualMed);
            metrics.setPcfTtmIndustryAverage(industryMetrics.pcfTtmAvg);
            metrics.setPcfTtmIndustryMedian(industryMetrics.pcfTtmMed);
            metrics.setPcfAnnualIndustryAverage(industryMetrics.pcfAnnualAvg);
            metrics.setPcfAnnualIndustryMedian(industryMetrics.pcfAnnualMed);
        }
    }

    private String getIndustry(String stockCode, Map<String, String> industryMap) {
        if (StringUtils.isBlank(stockCode)) {
            return null;
        }
        String plainCode = stockCode.length() > 2 ? stockCode.substring(2) : stockCode;
        return industryMap.get(plainCode);
    }

    private IndustryMetrics buildIndustryMetrics(List<CalculatedValuationMetricsVO> metricsList) {
        IndustryMetrics industryMetrics = new IndustryMetrics();
        industryMetrics.pegAvg = average(metricsList, CalculatedValuationMetricsVO::getPeg);
        industryMetrics.pegMed = median(metricsList, CalculatedValuationMetricsVO::getPeg);
        industryMetrics.peTtmAvg = average(metricsList, CalculatedValuationMetricsVO::getPeTtm);
        industryMetrics.peTtmMed = median(metricsList, CalculatedValuationMetricsVO::getPeTtm);
        industryMetrics.peAnnualAvg = average(metricsList, CalculatedValuationMetricsVO::getPeAnnual);
        industryMetrics.peAnnualMed = median(metricsList, CalculatedValuationMetricsVO::getPeAnnual);
        industryMetrics.psTtmAvg = average(metricsList, CalculatedValuationMetricsVO::getPsTtm);
        industryMetrics.psTtmMed = median(metricsList, CalculatedValuationMetricsVO::getPsTtm);
        industryMetrics.psAnnualAvg = average(metricsList, CalculatedValuationMetricsVO::getPsAnnual);
        industryMetrics.psAnnualMed = median(metricsList, CalculatedValuationMetricsVO::getPsAnnual);
        industryMetrics.pbMrqAvg = average(metricsList, CalculatedValuationMetricsVO::getPbMrq);
        industryMetrics.pbMrqMed = median(metricsList, CalculatedValuationMetricsVO::getPbMrq);
        industryMetrics.pbAnnualAvg = average(metricsList, CalculatedValuationMetricsVO::getPbAnnual);
        industryMetrics.pbAnnualMed = median(metricsList, CalculatedValuationMetricsVO::getPbAnnual);
        industryMetrics.pcfTtmAvg = average(metricsList, CalculatedValuationMetricsVO::getPcfTtm);
        industryMetrics.pcfTtmMed = median(metricsList, CalculatedValuationMetricsVO::getPcfTtm);
        industryMetrics.pcfAnnualAvg = average(metricsList, CalculatedValuationMetricsVO::getPcfAnnual);
        industryMetrics.pcfAnnualMed = median(metricsList, CalculatedValuationMetricsVO::getPcfAnnual);
        return industryMetrics;
    }

    private BigDecimal average(List<CalculatedValuationMetricsVO> metricsList, Function<CalculatedValuationMetricsVO, BigDecimal> valueGetter) {
        List<BigDecimal> values = values(metricsList, valueGetter);
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal median(List<CalculatedValuationMetricsVO> metricsList, Function<CalculatedValuationMetricsVO, BigDecimal> valueGetter) {
        List<BigDecimal> values = values(metricsList, valueGetter).stream().sorted().toList();
        if (values.isEmpty()) {
            return null;
        }
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) {
            return values.get(middle);
        }
        return values.get(middle - 1).add(values.get(middle)).divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> values(List<CalculatedValuationMetricsVO> metricsList, Function<CalculatedValuationMetricsVO, BigDecimal> valueGetter) {
        return metricsList.stream()
                .map(valueGetter)
                .filter(Objects::nonNull)
                .toList();
    }

    private BigDecimal multiply(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return null;
        }
        return left.multiply(right);
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private static final class IndustryMetrics {
        private BigDecimal pegAvg;
        private BigDecimal pegMed;
        private BigDecimal peTtmAvg;
        private BigDecimal peTtmMed;
        private BigDecimal peAnnualAvg;
        private BigDecimal peAnnualMed;
        private BigDecimal psTtmAvg;
        private BigDecimal psTtmMed;
        private BigDecimal psAnnualAvg;
        private BigDecimal psAnnualMed;
        private BigDecimal pbMrqAvg;
        private BigDecimal pbMrqMed;
        private BigDecimal pbAnnualAvg;
        private BigDecimal pbAnnualMed;
        private BigDecimal pcfTtmAvg;
        private BigDecimal pcfTtmMed;
        private BigDecimal pcfAnnualAvg;
        private BigDecimal pcfAnnualMed;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(String code, String name, List<StockZhValuationComparisonEm> list) {
        StockValuationMetrics stockValuationMetrics = stockValuationMetricsRepository.findByStockCode(code);

        if (stockValuationMetrics == null) {
            stockValuationMetrics = new StockValuationMetrics();
        }
        stockValuationMetrics.setStockCode(code);
        stockValuationMetrics.setStockName(name);

        code = code.substring(2);

        for (StockZhValuationComparisonEm data : list) {
            String c = data.getCode();
            if (code.equals(c)) {
                stockValuationMetrics.setPeg(data.getPeg());
                stockValuationMetrics.setPegRank(data.getPegRank());

                stockValuationMetrics.setPeLastYearA(data.getPe24a());
                stockValuationMetrics.setPeTtm(data.getPeTtm());
                stockValuationMetrics.setPeThisYE(data.getPe25e());
                stockValuationMetrics.setPeNextYE(data.getPe26e());
                stockValuationMetrics.setPeNext2YE(data.getPe27e());

                stockValuationMetrics.setPsLastYA(data.getPs24a());
                stockValuationMetrics.setPsTtm(data.getPsTtm());
                stockValuationMetrics.setPsThisYE(data.getPs25e());
                stockValuationMetrics.setPsNextYE(data.getPs26e());
                stockValuationMetrics.setPsNext2YE(data.getPs27e());

                stockValuationMetrics.setPbLastYA(data.getPb24a());
                stockValuationMetrics.setPbMrq(data.getPbMrq());

                stockValuationMetrics.setPceLastYA(data.getPce24a());
                stockValuationMetrics.setPceTtm(data.getPceTtm());

                stockValuationMetrics.setPcfLastYA(data.getPcf24a());
                stockValuationMetrics.setPcfTtm(data.getPcfTtm());

                stockValuationMetrics.setEvEbitdaLastYA(data.getEvEbitda24a());

                stockValuationMetrics.setCreatedAt(LocalDateTime.now());
            } else if ("行业中值".equals(c)) {
                stockValuationMetrics.setPegIndustryMed(data.getPeg());

                stockValuationMetrics.setPeLastYearIndustryMed(data.getPe24a());
                stockValuationMetrics.setPeTtmIndustryMed(data.getPeTtm());
                stockValuationMetrics.setPeThisYEIndustryMed(data.getPe25e());
                stockValuationMetrics.setPeNextYEIndustryMed(data.getPe26e());
                stockValuationMetrics.setPeNext2YEIndustryMed(data.getPe27e());

                stockValuationMetrics.setPsLastYAIndustryMed(data.getPs24a());
                stockValuationMetrics.setPsTtmIndustryMed(data.getPsTtm());
                stockValuationMetrics.setPsThisYEIndustryMed(data.getPs25e());
                stockValuationMetrics.setPsNextYEIndustryMed(data.getPs26e());
                stockValuationMetrics.setPsNext2YEIndustryMed(data.getPs27e());

                stockValuationMetrics.setPbLastYAIndustryMed(data.getPb24a());
                stockValuationMetrics.setPbMrqIndustryMed(data.getPbMrq());

                stockValuationMetrics.setPceLastYAIndustryMed(data.getPce24a());
                stockValuationMetrics.setPceTtmIndustryMed(data.getPceTtm());

                stockValuationMetrics.setPcfLastYAIndustryMed(data.getPcf24a());
                stockValuationMetrics.setPcfTtmIndustryMed(data.getPcfTtm());

                stockValuationMetrics.setEvEbitdaLastYAIndustryMed(data.getEvEbitda24a());

                stockValuationMetrics.setCreatedAt(LocalDateTime.now());
            } else if ("行业平均".equals(c)) {
                stockValuationMetrics.setPegIndustryAvg(data.getPeg());

                stockValuationMetrics.setPeLastYearIndustryAvg(data.getPe24a());
                stockValuationMetrics.setPeTtmIndustryAvg(data.getPeTtm());
                stockValuationMetrics.setPeThisYEIndustryAvg(data.getPe25e());
                stockValuationMetrics.setPeNextYEIndustryAvg(data.getPe26e());
                stockValuationMetrics.setPeNext2YEIndustryAvg(data.getPe27e());

                stockValuationMetrics.setPsLastYAIndustryAvg(data.getPs24a());
                stockValuationMetrics.setPsTtmIndustryAvg(data.getPsTtm());
                stockValuationMetrics.setPsThisYEIndustryAvg(data.getPs25e());
                stockValuationMetrics.setPsNextYEIndustryAvg(data.getPs26e());
                stockValuationMetrics.setPsNext2YEIndustryAvg(data.getPs27e());

                stockValuationMetrics.setPbLastYAIndustryAvg(data.getPb24a());
                stockValuationMetrics.setPbMrqIndustryAvg(data.getPbMrq());

                stockValuationMetrics.setPceLastYAIndustryAvg(data.getPce24a());
                stockValuationMetrics.setPceTtmIndustryAvg(data.getPceTtm());

                stockValuationMetrics.setPcfLastYAIndustryAvg(data.getPcf24a());
                stockValuationMetrics.setPcfTtmIndustryAvg(data.getPcfTtm());

                stockValuationMetrics.setEvEbitdaLastYAIndustryAvg(data.getEvEbitda24a());

                stockValuationMetrics.setCreatedAt(LocalDateTime.now());
            }
        }
        stockValuationMetricsRepository.save(stockValuationMetrics);
    }

}
