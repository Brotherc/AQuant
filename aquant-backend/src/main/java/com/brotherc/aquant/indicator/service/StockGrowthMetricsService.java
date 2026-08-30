package com.brotherc.aquant.indicator.service;

import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.indicator.entity.StockGrowthMetrics;
import com.brotherc.aquant.indicator.entity.StockPerformanceReport;
import com.brotherc.aquant.indicator.model.dto.GrowthIndustryMetrics;
import com.brotherc.aquant.indicator.model.vo.GrowthMetricsPageReqVO;
import com.brotherc.aquant.indicator.model.vo.GrowthOverviewVO;
import com.brotherc.aquant.indicator.repository.StockGrowthMetricsRepository;
import com.brotherc.aquant.indicator.repository.StockPerformanceReportRepository;
import com.brotherc.aquant.integration.akshare.model.StockZhGrowthComparisonEm;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.watchlist.entity.StockWatchlistGroup;
import com.brotherc.aquant.watchlist.entity.StockWatchlistStock;
import com.brotherc.aquant.watchlist.repository.StockWatchlistGroupRepository;
import com.brotherc.aquant.watchlist.repository.StockWatchlistStockRepository;
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
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockGrowthMetricsService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int SCALE = 4;
    private static final double MIN_REPORT_COVERAGE = 0.8;

    private final StockGrowthMetricsRepository stockGrowthMetricsRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockPerformanceReportRepository stockPerformanceReportRepository;
    private final StockWatchlistGroupRepository watchlistGroupRepository;
    private final StockWatchlistStockRepository watchlistStockRepository;

    public Page<StockGrowthMetrics> pageQuery(GrowthMetricsPageReqVO reqVO, Pageable pageable, Long userId) {
        List<Sort.Order> orders = pageable.getSort().stream().collect(Collectors.toCollection(ArrayList::new));
        if (orders.isEmpty()) {
            orders.add(Sort.Order.desc("growthScore"));
        }
        Set<String> sortedProperties = orders.stream()
                .map(Sort.Order::getProperty)
                .collect(Collectors.toSet());
        List<Sort.Order> fallbackOrders = List.of(
                Sort.Order.desc("growthScore"),
                Sort.Order.desc("revenueGrowthTtm"),
                Sort.Order.desc("netProfitGrowthTtm"),
                Sort.Order.desc("epsGrowthTtm"),
                Sort.Order.asc("stockCode")
        );
        fallbackOrders.stream()
                .filter(order -> !sortedProperties.contains(order.getProperty()))
                .forEach(orders::add);
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));

        Specification<StockGrowthMetrics> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (reqVO != null) {
                // 关键字搜索 (代码或名称)
                String keyword = StringUtils.isNotBlank(reqVO.getKeyword()) ? reqVO.getKeyword().trim() : reqVO.getStockCode();
                if (StringUtils.isNotBlank(keyword)) {
                    String kw = "%" + keyword + "%";
                    predicates.add(cb.or(
                            cb.like(root.get("stockCode"), kw),
                            cb.like(root.get("stockName"), kw)
                    ));
                }

                // 行业筛选
                if (StringUtils.isNotBlank(reqVO.getIndustry())) {
                    predicates.add(cb.equal(root.get("industry"), reqVO.getIndustry().trim()));
                }

                // 成长等级筛选
                if (StringUtils.isNotBlank(reqVO.getGrowthLevel())) {
                    predicates.add(cb.equal(root.get("growthLevel"), reqVO.getGrowthLevel().trim()));
                }

                // 快捷标签筛选
                if (StringUtils.isNotBlank(reqVO.getTabFilter()) && !"ALL".equalsIgnoreCase(reqVO.getTabFilter())) {
                    switch (reqVO.getTabFilter().toUpperCase()) {
                        case "HIGH_GROWTH" -> {
                            // 高成长榜：成长评分 >= 80 (优秀)
                            predicates.add(cb.ge(root.get("growthScore"), new BigDecimal("80.0")));
                        }
                        case "STABLE_GROWTH" -> {
                            // 稳健成长：成长评分 65~80 (良好)
                            predicates.add(cb.ge(root.get("growthScore"), new BigDecimal("65.0")));
                            predicates.add(cb.lt(root.get("growthScore"), new BigDecimal("80.0")));
                        }
                        case "PROFIT_RECOVERY" -> {
                            // 盈利修复：成长评分 50~65 (中等) 或 TTM净利增速 > 0 且 去年实际增速 < 0
                            predicates.add(cb.or(
                                    cb.and(cb.ge(root.get("growthScore"), new BigDecimal("50.0")), cb.lt(root.get("growthScore"), new BigDecimal("65.0"))),
                                    cb.and(cb.gt(root.get("netProfitGrowthTtm"), BigDecimal.ZERO), cb.lt(root.get("netProfitGrowthLastYA"), BigDecimal.ZERO))
                            ));
                        }
                        case "WATCHLIST" -> {
                            // 我的自选
                            Set<String> watchlistCodes = getUserWatchlistStockCodes(userId);
                            if (watchlistCodes.isEmpty()) {
                                predicates.add(cb.disjunction());
                            } else {
                                List<String> allVariants = new ArrayList<>(watchlistCodes);
                                for (String c : watchlistCodes) {
                                    if (c.length() == 6) {
                                        allVariants.add("sh" + c);
                                        allVariants.add("sz" + c);
                                        allVariants.add("bj" + c);
                                    } else if (c.length() > 6) {
                                        allVariants.add(c.substring(2));
                                    }
                                }
                                predicates.add(root.get("stockCode").in(allVariants));
                            }
                        }
                        default -> {}
                    }
                }

                // 成长评分范围
                if (reqVO.getGrowthScoreMin() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("growthScore"), reqVO.getGrowthScoreMin()));
                }
                if (reqVO.getGrowthScoreMax() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("growthScore"), reqVO.getGrowthScoreMax()));
                }

                // EPS 3年复合增长率范围
                if (reqVO.getEpsGrowth3yCagrMin() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("epsGrowth3yCagr"), reqVO.getEpsGrowth3yCagrMin()));
                }
                if (reqVO.getEpsGrowth3yCagrMax() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("epsGrowth3yCagr"), reqVO.getEpsGrowth3yCagrMax()));
                }

                // EPS TTM
                if (reqVO.getEpsGrowthTtmMin() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("epsGrowthTtm"), reqVO.getEpsGrowthTtmMin()));
                }
                if (reqVO.getEpsGrowthTtmMax() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("epsGrowthTtm"), reqVO.getEpsGrowthTtmMax()));
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

        return stockGrowthMetricsRepository.findAll(spec, pageable);
    }

    /**
     * 获取全市场可选行业列表
     */
    public List<String> getIndustries() {
        List<String> list = stockGrowthMetricsRepository.findDistinctIndustries();
        if (!CollectionUtils.isEmpty(list)) {
            return list;
        }
        return stockPerformanceReportRepository.findDistinctIndustries();
    }

    /**
     * 顶部 4 维指标统计概览
     */
    public GrowthOverviewVO getOverview(Long userId) {
        List<StockGrowthMetrics> list = stockGrowthMetricsRepository.findAll();
        if (CollectionUtils.isEmpty(list)) {
            return new GrowthOverviewVO(0L, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
        }

        // 1. 高成长机会 (评分>=80)
        long highGrowthCount = list.stream()
                .filter(item -> item.getGrowthScore() != null && item.getGrowthScore().compareTo(new BigDecimal("80.0")) >= 0)
                .count();

        // 2. 市场营收增长中位数 (全市场 TTM 营收增速)
        List<BigDecimal> revGrowthList = list.stream()
                .map(StockGrowthMetrics::getRevenueGrowthTtm)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        BigDecimal marketRevMedian = BigDecimal.ZERO;
        if (!revGrowthList.isEmpty()) {
            int size = revGrowthList.size();
            if (size % 2 == 0) {
                marketRevMedian = revGrowthList.get(size / 2 - 1).add(revGrowthList.get(size / 2))
                        .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            } else {
                marketRevMedian = revGrowthList.get(size / 2).setScale(2, RoundingMode.HALF_UP);
            }
        }

        // 3. 市场净利润增长中位数 (全市场 TTM 净利增速)
        List<BigDecimal> netProfitGrowthList = list.stream()
                .map(StockGrowthMetrics::getNetProfitGrowthTtm)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        BigDecimal marketNetProfitMedian = BigDecimal.ZERO;
        if (!netProfitGrowthList.isEmpty()) {
            int size = netProfitGrowthList.size();
            if (size % 2 == 0) {
                marketNetProfitMedian = netProfitGrowthList.get(size / 2 - 1).add(netProfitGrowthList.get(size / 2))
                        .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            } else {
                marketNetProfitMedian = netProfitGrowthList.get(size / 2).setScale(2, RoundingMode.HALF_UP);
            }
        }

        // 4. 我的自选高成长
        Set<String> watchlistStockCodes = getUserWatchlistStockCodes(userId);
        long watchlistHighGrowthCount = 0;
        if (!watchlistStockCodes.isEmpty()) {
            watchlistHighGrowthCount = list.stream()
                    .filter(item -> watchlistStockCodes.contains(item.getStockCode()) || (item.getStockCode().length() > 2 && watchlistStockCodes.contains(item.getStockCode().substring(2))))
                    .filter(item -> item.getGrowthScore() != null && item.getGrowthScore().compareTo(new BigDecimal("80.0")) >= 0)
                    .count();
        }

        return GrowthOverviewVO.builder()
                .highGrowthOpportunityCount(highGrowthCount)
                .marketRevenueGrowthMedian(marketRevMedian)
                .marketNetProfitGrowthMedian(marketNetProfitMedian)
                .watchlistHighGrowthCount(watchlistHighGrowthCount)
                .build();
    }

    /**
     * 获取用户所有自选股票代码集合
     */
    private Set<String> getUserWatchlistStockCodes(Long userId) {
        if (userId == null) return Collections.emptySet();
        List<StockWatchlistGroup> groups = watchlistGroupRepository.findAllByUserIdOrderBySortNoAsc(userId);
        if (groups.isEmpty()) return Collections.emptySet();
        List<Long> groupIds = groups.stream().map(StockWatchlistGroup::getId).toList();
        List<StockWatchlistStock> watchlistStocks = watchlistStockRepository.findByGroupIdIn(groupIds);
        return watchlistStocks.stream()
                .map(StockWatchlistStock::getStockCode)
                .filter(StringUtils::isNotBlank)
                .flatMap(code -> java.util.stream.Stream.of(code, StockUtils.wrapExchangePrefix(code), StockUtils.getPlainCode(code)))
                .collect(Collectors.toSet());
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
     * 核心计算：计算每股收益、营收、净利润的 TTM、去年实际、近3年历史年报及 3年复合增长率
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
                .collect(Collectors.groupingBy(report -> StockUtils.getPlainCode(report.getStockCode())));

        Set<String> activeCodes = stockQuotes.stream()
                .filter(Objects::nonNull)
                .map(StockQuote::getCode)
                .filter(StringUtils::isNotBlank)
                .map(StockUtils::getPlainCode)
                .collect(Collectors.toSet());
        LocalDate latestCoveredReportDate = findLatestCoveredReportDate(performanceReports, activeCodes, false);
        LocalDate latestCoveredAnnualDate = findLatestCoveredReportDate(performanceReports, activeCodes, true);
        if (latestCoveredReportDate == null || latestCoveredAnnualDate == null) {
            log.warn("没有找到披露覆盖率达到 {}% 的业绩报告期，跳过成长性指标计算。", MIN_REPORT_COVERAGE * 100);
            return List.of();
        }
        log.info("成长性指标统一计算口径，latestReportDate={}, latestAnnualDate={}",
                latestCoveredReportDate, latestCoveredAnnualDate);

        Map<String, String> industryMap = buildIndustryMap(performanceReports);
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

            StockPerformanceReport latestReport = findByReportDate(reports, latestCoveredReportDate);
            StockPerformanceReport latestAnnual = findByReportDate(reports, latestCoveredAnnualDate);
            StockPerformanceReport previousAnnual = findByReportDate(reports, latestCoveredAnnualDate.minusYears(1));
            StockPerformanceReport previous2Annual = findByReportDate(reports, latestCoveredAnnualDate.minusYears(2));
            StockPerformanceReport previous3Annual = findByReportDate(reports, latestCoveredAnnualDate.minusYears(3));

            StockGrowthMetrics item = new StockGrowthMetrics();
            item.setStockCode(quote.getCode());
            item.setStockName(quote.getName());
            item.setIndustry(getIndustry(quote.getCode(), industryMap));

            // 1. 近 3 年历史年报增长率
            if (latestAnnual != null && previousAnnual != null) {
                item.setRevenueGrowthLastYA(calculateGrowthRate(latestAnnual.getTotalRevenue(), previousAnnual.getTotalRevenue()));
                item.setNetProfitGrowthLastYA(calculateGrowthRate(latestAnnual.getNetProfit(), previousAnnual.getNetProfit()));
                item.setEpsGrowthLastYA(calculateGrowthRate(latestAnnual.getEarningsPerShare(), previousAnnual.getEarningsPerShare()));
            }
            if (previousAnnual != null && previous2Annual != null) {
                item.setRevenueGrowthLast2yA(calculateGrowthRate(previousAnnual.getTotalRevenue(), previous2Annual.getTotalRevenue()));
                item.setNetProfitGrowthLast2yA(calculateGrowthRate(previousAnnual.getNetProfit(), previous2Annual.getNetProfit()));
                item.setEpsGrowthLast2yA(calculateGrowthRate(previousAnnual.getEarningsPerShare(), previous2Annual.getEarningsPerShare()));
            }
            if (previous2Annual != null && previous3Annual != null) {
                item.setRevenueGrowthLast3yA(calculateGrowthRate(previous2Annual.getTotalRevenue(), previous3Annual.getTotalRevenue()));
                item.setNetProfitGrowthLast3yA(calculateGrowthRate(previous2Annual.getNetProfit(), previous3Annual.getNetProfit()));
                item.setEpsGrowthLast3yA(calculateGrowthRate(previous2Annual.getEarningsPerShare(), previous3Annual.getEarningsPerShare()));
            }

            // 2. TTM 滚动增长率 (TTM: 最新滚动4季度 vs 去年同期滚动4季度)
            if (latestReport != null) {
                BigDecimal revTtm = calculateTtmValue(reports, latestReport, StockPerformanceReport::getTotalRevenue);
                BigDecimal prevRevTtm = calculatePreviousTtmValue(reports, latestReport, StockPerformanceReport::getTotalRevenue);
                item.setRevenueGrowthTtm(calculateGrowthRate(revTtm, prevRevTtm));

                BigDecimal netProfitTtm = calculateTtmValue(reports, latestReport, StockPerformanceReport::getNetProfit);
                BigDecimal prevNetProfitTtm = calculatePreviousTtmValue(reports, latestReport, StockPerformanceReport::getNetProfit);
                item.setNetProfitGrowthTtm(calculateGrowthRate(netProfitTtm, prevNetProfitTtm));

                BigDecimal epsTtm = calculateTtmValue(reports, latestReport, StockPerformanceReport::getEarningsPerShare);
                BigDecimal prevEpsTtm = calculatePreviousTtmValue(reports, latestReport, StockPerformanceReport::getEarningsPerShare);
                item.setEpsGrowthTtm(calculateGrowthRate(epsTtm, prevEpsTtm));
            }

            // 3. 3年复合增长率 (3yCAGR: 终期年报 vs 3年前基期年报)
            if (latestAnnual != null && previous3Annual != null) {
                item.setRevenueGrowth3yCagr(calculateCagr(latestAnnual.getTotalRevenue(), previous3Annual.getTotalRevenue(), 3));
                item.setNetProfitGrowth3yCagr(calculateCagr(latestAnnual.getNetProfit(), previous3Annual.getNetProfit(), 3));
                item.setEpsGrowth3yCagr(calculateCagr(latestAnnual.getEarningsPerShare(), previous3Annual.getEarningsPerShare(), 3));
            }

            resultList.add(item);
        }

        // 4. 行业中位数与均值聚合
        fillIndustryMetrics(resultList, industryMap);

        // 5. 行业排名 (按 epsGrowth3yCagr 降序)
        fillIndustryRanks(resultList, industryMap);

        // 6. 计算成长评分 (growthScore)、等级 (growthLevel) 与结论 (conclusion)
        for (StockGrowthMetrics item : resultList) {
            calculateScoreAndConclusion(item);
        }

        return resultList;
    }

    /**
     * 智能成长综合评分引擎
     */
    private void calculateScoreAndConclusion(StockGrowthMetrics item) {
        if (Stream.of(
                item.getEpsGrowthTtm(), item.getRevenueGrowthTtm(), item.getNetProfitGrowthTtm(),
                item.getRevenueGrowth3yCagr(), item.getRevenueGrowthLastYA(),
                item.getRevenueGrowthLast2yA(), item.getRevenueGrowthLast3yA()
        ).anyMatch(Objects::isNull)) {
            item.setGrowthScore(null);
            item.setGrowthLevel("数据不足");
            item.setConclusion("统一报告期或连续年度数据不足，暂不进行成长评分。");
            return;
        }

        // 短期增长55分：营收25分、净利润20分、EPS 10分；EPS降权以避免与净利润重复计权。
        double score = calculateGrowthRateScore(item.getRevenueGrowthTtm(), 15, 30, 25);
        score += calculateGrowthRateScore(item.getNetProfitGrowthTtm(), 20, 50, 20);
        score += calculateGrowthRateScore(item.getEpsGrowthTtm(), 20, 50, 10);

        // 长期增长30分：营收和净利润三年复合增速各15分；亏损导致净利润CAGR无意义时不计分。
        score += calculateGrowthRateScore(item.getRevenueGrowth3yCagr(), 10, 20, 15);
        if (item.getNetProfitGrowth3yCagr() != null) {
            score += calculateGrowthRateScore(item.getNetProfitGrowth3yCagr(), 10, 25, 15);
        }

        // 行业相对表现10分，仅正增长且超过行业中位数时加分，避免“负增长但跌得较少”获得奖励。
        if (isPositiveAndAboveIndustryMedian(item.getRevenueGrowthTtm(), item.getRevenueGrowthTtmIndustryMed())) {
            score += 5;
        }
        if (isPositiveAndAboveIndustryMedian(item.getNetProfitGrowthTtm(), item.getNetProfitGrowthTtmIndustryMed())) {
            score += 5;
        }

        // 连续性5分：近三年营收、净利润同比每保持一年正增长，获得对应比例分数。
        long positiveAnnualGrowthCount = Stream.of(
                        item.getRevenueGrowthLastYA(), item.getRevenueGrowthLast2yA(), item.getRevenueGrowthLast3yA(),
                        item.getNetProfitGrowthLastYA(), item.getNetProfitGrowthLast2yA(), item.getNetProfitGrowthLast3yA()
                )
                .filter(Objects::nonNull)
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .count();
        score += positiveAnnualGrowthCount * 5.0 / 6.0;

        int finalScore = Math.max(0, Math.min(100, (int) Math.round(score)));
        boolean revenueDeclining = item.getRevenueGrowthTtm().compareTo(BigDecimal.ZERO) < 0;
        boolean profitDeclining = item.getNetProfitGrowthTtm().compareTo(BigDecimal.ZERO) < 0;
        boolean epsDeclining = item.getEpsGrowthTtm().compareTo(BigDecimal.ZERO) < 0;
        if (revenueDeclining && profitDeclining) {
            finalScore = Math.min(finalScore, 49);
        } else if (revenueDeclining || profitDeclining || epsDeclining) {
            finalScore = Math.min(finalScore, 64);
        }
        if (item.getRevenueGrowth3yCagr().compareTo(BigDecimal.ZERO) < 0) {
            finalScore = Math.min(finalScore, 49);
        } else if (item.getNetProfitGrowth3yCagr() == null
                || item.getNetProfitGrowth3yCagr().compareTo(BigDecimal.ZERO) < 0) {
            finalScore = Math.min(finalScore, 64);
        }
        item.setGrowthScore(BigDecimal.valueOf(finalScore));

        // 等级与结论
        if (finalScore >= 80.0) {
            item.setGrowthLevel("优秀");
            item.setConclusion("短期与长期增长表现较强，且增长持续性较好。");
        } else if (finalScore >= 65.0) {
            item.setGrowthLevel("良好");
            item.setConclusion("成长表现良好，营收与盈利总体保持扩张。");
        } else if (finalScore >= 50.0) {
            item.setGrowthLevel("中等");
            item.setConclusion("成长性一般，仍需观察后续业绩的持续性。");
        } else {
            item.setGrowthLevel("较弱");
            item.setConclusion("多项增长指标偏弱或承压，需注意业绩波动风险。");
        }
    }

    private double calculateGrowthRateScore(BigDecimal growthRate, double target, double strong, double maxScore) {
        double value = growthRate.doubleValue();
        if (value <= -20) return 0;
        if (value < 0) return maxScore * 0.2 * (value + 20) / 20;
        if (value < target) return maxScore * (0.2 + 0.6 * value / target);
        if (value < strong) return maxScore * (0.8 + 0.2 * (value - target) / (strong - target));
        return maxScore;
    }

    private boolean isPositiveAndAboveIndustryMedian(BigDecimal value, BigDecimal industryMedian) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0
                && industryMedian != null && value.compareTo(industryMedian) > 0;
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

    private LocalDate findLatestCoveredReportDate(
            List<StockPerformanceReport> reports, Set<String> activeCodes, boolean annualOnly
    ) {
        if (activeCodes.isEmpty()) {
            return null;
        }
        Map<LocalDate, Set<String>> reportCodesByDate = new HashMap<>();
        for (StockPerformanceReport report : reports) {
            if (report == null || report.getReportDate() == null || StringUtils.isBlank(report.getStockCode())
                    || (annualOnly && !StockUtils.isAnnualReport(report.getReportDate()))) {
                continue;
            }
            String code = StockUtils.getPlainCode(report.getStockCode());
            if (activeCodes.contains(code)) {
                reportCodesByDate.computeIfAbsent(report.getReportDate(), key -> new HashSet<>()).add(code);
            }
        }
        for (LocalDate reportDate : reportCodesByDate.keySet().stream().sorted(Comparator.reverseOrder()).toList()) {
            double coverage = (double) reportCodesByDate.get(reportDate).size() / activeCodes.size();
            if (coverage >= MIN_REPORT_COVERAGE) {
                return reportDate;
            }
            log.info("成长性指标跳过披露覆盖率不足的报告期，reportDate={}, coverage={}",
                    reportDate, String.format(Locale.ROOT, "%.2f%%", coverage * 100));
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
                        report -> StockUtils.getPlainCode(report.getStockCode()),
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
        target.setIndustry(source.getIndustry());
        target.setGrowthScore(source.getGrowthScore());
        target.setGrowthLevel(source.getGrowthLevel());
        target.setConclusion(source.getConclusion());

        target.setEpsGrowth3yCagr(source.getEpsGrowth3yCagr());
        target.setEpsGrowth3yCagrIndustryMed(source.getEpsGrowth3yCagrIndustryMed());
        target.setEpsGrowth3yCagrIndustryAvg(source.getEpsGrowth3yCagrIndustryAvg());
        target.setEpsGrowth3yCagrRank(source.getEpsGrowth3yCagrRank());

        target.setEpsGrowthLastYA(source.getEpsGrowthLastYA());
        target.setEpsGrowthLast2yA(source.getEpsGrowthLast2yA());
        target.setEpsGrowthLast3yA(source.getEpsGrowthLast3yA());
        target.setEpsGrowthLastYAIndustryMed(source.getEpsGrowthLastYAIndustryMed());
        target.setEpsGrowthLastYAIndustryAvg(source.getEpsGrowthLastYAIndustryAvg());

        target.setEpsGrowthTtm(source.getEpsGrowthTtm());
        target.setEpsGrowthTtmIndustryMed(source.getEpsGrowthTtmIndustryMed());
        target.setEpsGrowthTtmIndustryAvg(source.getEpsGrowthTtmIndustryAvg());

        target.setRevenueGrowth3yCagr(source.getRevenueGrowth3yCagr());
        target.setRevenueGrowth3yCagrIndustryMed(source.getRevenueGrowth3yCagrIndustryMed());
        target.setRevenueGrowth3yCagrIndustryAvg(source.getRevenueGrowth3yCagrIndustryAvg());

        target.setRevenueGrowthLastYA(source.getRevenueGrowthLastYA());
        target.setRevenueGrowthLast2yA(source.getRevenueGrowthLast2yA());
        target.setRevenueGrowthLast3yA(source.getRevenueGrowthLast3yA());
        target.setRevenueGrowthLastYAIndustryMed(source.getRevenueGrowthLastYAIndustryMed());
        target.setRevenueGrowthLastYAIndustryAvg(source.getRevenueGrowthLastYAIndustryAvg());

        target.setRevenueGrowthTtm(source.getRevenueGrowthTtm());
        target.setRevenueGrowthTtmIndustryMed(source.getRevenueGrowthTtmIndustryMed());
        target.setRevenueGrowthTtmIndustryAvg(source.getRevenueGrowthTtmIndustryAvg());

        target.setNetProfitGrowth3yCagr(source.getNetProfitGrowth3yCagr());
        target.setNetProfitGrowth3yCagrIndustryMed(source.getNetProfitGrowth3yCagrIndustryMed());
        target.setNetProfitGrowth3yCagrIndustryAvg(source.getNetProfitGrowth3yCagrIndustryAvg());

        target.setNetProfitGrowthLastYA(source.getNetProfitGrowthLastYA());
        target.setNetProfitGrowthLast2yA(source.getNetProfitGrowthLast2yA());
        target.setNetProfitGrowthLast3yA(source.getNetProfitGrowthLast3yA());
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
        calculateScoreAndConclusion(stockGrowthMetrics);
        stockGrowthMetricsRepository.save(stockGrowthMetrics);
    }

}
