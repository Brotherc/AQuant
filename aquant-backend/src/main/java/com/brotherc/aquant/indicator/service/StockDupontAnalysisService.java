package com.brotherc.aquant.indicator.service;

import com.brotherc.aquant.indicator.entity.StockBalanceSheet;
import com.brotherc.aquant.indicator.entity.StockDupontAnalysis;
import com.brotherc.aquant.indicator.entity.StockPerformanceReport;
import com.brotherc.aquant.indicator.model.dto.DupontIndustryMetrics;
import com.brotherc.aquant.indicator.model.vo.DupontAnalysisPageReqVO;
import com.brotherc.aquant.indicator.model.vo.DupontOverviewVO;
import com.brotherc.aquant.indicator.repository.StockBalanceSheetRepository;
import com.brotherc.aquant.indicator.repository.StockDupontAnalysisRepository;
import com.brotherc.aquant.indicator.repository.StockPerformanceReportRepository;
import com.brotherc.aquant.integration.akshare.model.StockZhDupontComparisonEm;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.watchlist.entity.StockWatchlistGroup;
import com.brotherc.aquant.watchlist.entity.StockWatchlistStock;
import com.brotherc.aquant.watchlist.repository.StockWatchlistGroupRepository;
import com.brotherc.aquant.watchlist.repository.StockWatchlistStockRepository;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.common.utils.UserContext;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDupontAnalysisService {

    private static final String ROE_3Y_AVG_INDUSTRY_AVG = "roe3yAvgIndustryAvg";
    private static final String ROE_3Y_AVG = "roe3yAvg";
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final int SCALE = 4;

    private final StockDupontAnalysisRepository stockDupontAnalysisRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockPerformanceReportRepository stockPerformanceReportRepository;
    private final StockBalanceSheetRepository stockBalanceSheetRepository;
    private final StockWatchlistGroupRepository watchlistGroupRepository;
    private final StockWatchlistStockRepository watchlistStockRepository;

    public Page<StockDupontAnalysis> pageQuery(DupontAnalysisPageReqVO query, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "roe3yAvg"));
        }

        Specification<StockDupontAnalysis> specification = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null) {
                // 等值/模糊查询 keyword 或 stockCode
                String keyword = StringUtils.isNotBlank(query.getKeyword()) ? query.getKeyword().trim() : query.getStockCode();
                if (StringUtils.isNotBlank(keyword)) {
                    String kw = "%" + keyword.trim() + "%";
                    predicates.add(cb.or(
                            cb.like(root.get("stockCode"), kw),
                            cb.like(root.get("stockName"), kw)
                    ));
                }

                // 行业筛选
                if (StringUtils.isNotBlank(query.getIndustry())) {
                    predicates.add(cb.equal(root.get("industry"), query.getIndustry().trim()));
                }

                // 质量等级筛选
                if (StringUtils.isNotBlank(query.getQualityLevel())) {
                    predicates.add(cb.equal(root.get("qualityLevel"), query.getQualityLevel().trim()));
                }

                // 质量评分最小值
                if (query.getQualityScoreMin() != null) {
                    predicates.add(cb.ge(root.get("qualityScore"), query.getQualityScoreMin()));
                }

                // 快捷标签筛选
                if (StringUtils.isNotBlank(query.getTabFilter())) {
                    switch (query.getTabFilter()) {
                        case "HIGH_QUALITY" -> {
                            predicates.add(cb.ge(root.get(ROE_3Y_AVG), new BigDecimal("15")));
                            predicates.add(cb.ge(root.get("qualityScore"), new BigDecimal("75")));
                        }
                        case "HIGH_LEVERAGE" -> {
                            predicates.add(cb.ge(root.get("equityMultiplier3yAvg"), new BigDecimal("2.5")));
                        }
                        case "STABLE_PROFIT" -> {
                            predicates.add(cb.ge(root.get(ROE_3Y_AVG), new BigDecimal("10")));
                            predicates.add(cb.ge(root.get("netMargin3yAvg"), new BigDecimal("5")));
                            predicates.add(cb.le(root.get("equityMultiplier3yAvg"), new BigDecimal("2.0")));
                        }
                        case "WATCHLIST" -> {
                            Long userId = UserContext.getCurrentUserId();
                            Set<String> watchlistCodes = getUserWatchlistStockCodes(userId);
                            if (CollectionUtils.isEmpty(watchlistCodes)) {
                                predicates.add(cb.disjunction());
                            } else {
                                predicates.add(root.get("stockCode").in(watchlistCodes));
                            }
                        }
                        default -> {}
                    }
                }

                // ROE-3年平均 范围
                if (query.getRoe3yAvgMin() != null) {
                    predicates.add(cb.ge(root.get(ROE_3Y_AVG), query.getRoe3yAvgMin()));
                }
                if (query.getRoe3yAvgMax() != null) {
                    predicates.add(cb.le(root.get(ROE_3Y_AVG), query.getRoe3yAvgMax()));
                }

                // ROE-3年平均-行业中值 范围
                if (query.getRoe3yAvgIndustryMedMin() != null) {
                    predicates.add(cb.ge(root.get("roe3yAvgIndustryMed"), query.getRoe3yAvgIndustryMedMin()));
                }
                if (query.getRoe3yAvgIndustryMedMax() != null) {
                    predicates.add(cb.le(root.get("roe3yAvgIndustryMed"), query.getRoe3yAvgIndustryMedMax()));
                }

                // ROE-3年平均-行业平均 范围
                if (query.getRoe3yAvgIndustryAvgMin() != null) {
                    predicates.add(cb.ge(root.get(ROE_3Y_AVG_INDUSTRY_AVG), query.getRoe3yAvgIndustryAvgMin()));
                }
                if (query.getRoe3yAvgIndustryAvgMax() != null) {
                    predicates.add(cb.le(root.get(ROE_3Y_AVG_INDUSTRY_AVG), query.getRoe3yAvgIndustryAvgMax()));
                }

                // ROE-3年平均 > 行业平均
                if (Boolean.TRUE.equals(query.getRoeHigherThanIndustryAvg())) {
                    predicates.add(cb.gt(root.get(ROE_3Y_AVG), root.get(ROE_3Y_AVG_INDUSTRY_AVG)));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return stockDupontAnalysisRepository.findAll(specification, pageable);
    }

    /**
     * 获取杜邦分析顶部概览统计数据
     */
    public DupontOverviewVO getDupontOverview(Long userId) {
        List<StockDupontAnalysis> list = stockDupontAnalysisRepository.findAll();
        if (list.isEmpty()) {
            return new DupontOverviewVO(0L, BigDecimal.ZERO, 0L, 0L);
        }

        long highQualityCount = list.stream()
                .filter(item -> item.getRoe3yAvg() != null && item.getRoe3yAvg().compareTo(new BigDecimal("15")) >= 0
                        && item.getQualityScore() != null && item.getQualityScore().compareTo(new BigDecimal("75")) >= 0)
                .count();

        long leverageWarningCount = list.stream()
                .filter(item -> {
                    BigDecimal em = item.getEquityMultiplier3yAvg() != null ? item.getEquityMultiplier3yAvg() : item.getEquityMultiplierLastYA();
                    return em != null && em.compareTo(new BigDecimal("2.5")) > 0;
                })
                .count();

        // 行业/全市场加权中位数 (取全市场 roe3yAvg 的中位数)
        List<BigDecimal> roeList = list.stream()
                .map(StockDupontAnalysis::getRoe3yAvg)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        BigDecimal industryRoeMedian = BigDecimal.ZERO;
        if (!roeList.isEmpty()) {
            int size = roeList.size();
            if (size % 2 == 1) {
                industryRoeMedian = roeList.get(size / 2);
            } else {
                industryRoeMedian = roeList.get(size / 2 - 1).add(roeList.get(size / 2)).divide(TWO, 2, RoundingMode.HALF_UP);
            }
        }

        // 用户自选高质量
        long watchlistHighQualityCount = 0L;
        if (userId != null) {
            Set<String> userWatchlistCodes = getUserWatchlistStockCodes(userId);
            if (!userWatchlistCodes.isEmpty()) {
                watchlistHighQualityCount = list.stream()
                        .filter(item -> userWatchlistCodes.contains(item.getStockCode()) || userWatchlistCodes.contains(StockUtils.getPlainCode(item.getStockCode())))
                        .filter(item -> item.getQualityScore() != null && item.getQualityScore().compareTo(new BigDecimal("75")) >= 0)
                        .count();
            }
        }

        return DupontOverviewVO.builder()
                .highQualityCount(highQualityCount)
                .industryRoeMedian(industryRoeMedian)
                .watchlistHighQualityCount(watchlistHighQualityCount)
                .leverageWarningCount(leverageWarningCount)
                .build();
    }

    /**
     * 获取全市场可选行业列表（直接按SQL仅查询去重后的行业字段）
     */
    public List<String> getIndustries() {
        List<String> list = stockDupontAnalysisRepository.findDistinctIndustries();
        if (!CollectionUtils.isEmpty(list)) {
            return list;
        }
        return stockPerformanceReportRepository.findDistinctIndustries();
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
        return watchlistStocks.stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
    }

    /**
     * 刷新并离线落库全市场股票杜邦分析指标
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshDupontAnalysis() {
        log.info("开始计算并离线落库股票杜邦分析数据...");
        List<StockDupontAnalysis> calculatedList = calculateDupontAnalysis();
        if (CollectionUtils.isEmpty(calculatedList)) {
            log.info("计算得到的杜邦分析数据为空，跳过落库。");
            return;
        }

        Map<String, StockDupontAnalysis> existingMap = stockDupontAnalysisRepository.findAll().stream()
                .filter(item -> StringUtils.isNotBlank(item.getStockCode()))
                .collect(Collectors.toMap(StockDupontAnalysis::getStockCode, Function.identity(), (a, b) -> a));

        List<StockDupontAnalysis> toSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (StockDupontAnalysis calculated : calculatedList) {
            StockDupontAnalysis entity = existingMap.getOrDefault(calculated.getStockCode(), new StockDupontAnalysis());
            copyProperties(calculated, entity);
            entity.setCreatedAt(now);
            toSave.add(entity);
        }

        stockDupontAnalysisRepository.saveAll(toSave);
        log.info("股票杜邦分析数据离线落库完成，共更新 {} 条数据。", toSave.size());
    }

    /**
     * 执行全量杜邦分析指标计算
     */
    public List<StockDupontAnalysis> calculateDupontAnalysis() {
        List<StockPerformanceReport> performanceReports = stockPerformanceReportRepository.findAll();
        List<StockBalanceSheet> balanceSheets = stockBalanceSheetRepository.findAll();
        List<StockQuote> stockQuotes = stockQuoteRepository.findAll();

        if (CollectionUtils.isEmpty(stockQuotes)) {
            log.warn("股票行情清单为空，跳过杜邦分析计算。");
            return List.of();
        }

        // 获取最近 3 个年报年份（降序：yearLast1 > yearLast2 > yearLast3）
        List<Integer> years = getLatestThreeAnnualYears(performanceReports, balanceSheets);
        if (years.isEmpty()) {
            log.warn("未找到有效的历史年报报告期（12-31），跳过杜邦分析计算。");
            return List.of();
        }

        Integer yearLast1 = years.get(0);
        Integer yearLast2 = years.size() > 1 ? years.get(1) : null;
        Integer yearLast3 = years.size() > 2 ? years.get(2) : null;

        log.info("杜邦分析使用年报报告期年份：去年实际={}, 2年前实际={}, 3年前实际={}", yearLast1, yearLast2, yearLast3);

        // 按股票纯代码和年份聚合财报
        Map<String, Map<Integer, StockPerformanceReport>> prMap = performanceReports.stream()
                .filter(r -> r != null && StringUtils.isNotBlank(r.getStockCode()) && StockUtils.isAnnualReport(r.getReportDate()))
                .collect(Collectors.groupingBy(
                        StockPerformanceReport::getStockCode,
                        Collectors.toMap(r -> r.getReportDate().getYear(), Function.identity(), (a, b) -> a)
                ));

        Map<String, Map<Integer, StockBalanceSheet>> bsMap = balanceSheets.stream()
                .filter(b -> b != null && StringUtils.isNotBlank(b.getStockCode()) && StockUtils.isAnnualReport(b.getReportDate()))
                .collect(Collectors.groupingBy(
                        StockBalanceSheet::getStockCode,
                        Collectors.toMap(b -> b.getReportDate().getYear(), Function.identity(), (a, b) -> a)
                ));

        List<StockDupontAnalysis> resultList = new ArrayList<>();

        for (StockQuote quote : stockQuotes) {
            if (quote == null || StringUtils.isBlank(quote.getCode())) {
                continue;
            }
            String plainCode = StockUtils.getPlainCode(quote.getCode());
            Map<Integer, StockPerformanceReport> prYears = prMap.getOrDefault(plainCode, Map.of());
            Map<Integer, StockBalanceSheet> bsYears = bsMap.getOrDefault(plainCode, Map.of());

            StockDupontAnalysis item = new StockDupontAnalysis();
            item.setStockCode(quote.getCode());
            item.setStockName(quote.getName());

            // 计算去年 (yearLast1) 指标
            calculateYearMetrics(item, prYears.get(yearLast1), bsYears.get(yearLast1), 1);
            // 计算2年前 (yearLast2) 指标
            if (yearLast2 != null) {
                calculateYearMetrics(item, prYears.get(yearLast2), bsYears.get(yearLast2), 2);
            }
            // 计算3年前 (yearLast3) 指标
            if (yearLast3 != null) {
                calculateYearMetrics(item, prYears.get(yearLast3), bsYears.get(yearLast3), 3);
            }

            // 计算 3 年平均指标
            item.setRoe3yAvg(calculateAverage(item.getRoeLastYA(), item.getRoeLast2yA(), item.getRoeLast3yA()));
            item.setNetMargin3yAvg(calculateAverage(item.getNetMarginLastYA(), item.getNetMarginLast2yA(), item.getNetMarginLast3yA()));
            item.setAssetTurnover3yAvg(calculateAverage(item.getAssetTurnoverLastYA(), item.getAssetTurnoverLast2yA(), item.getAssetTurnoverLast3yA()));
            item.setEquityMultiplier3yAvg(calculateAverage(item.getEquityMultiplierLastYA(), item.getEquityMultiplierLast2yA(), item.getEquityMultiplierLast3yA()));

            resultList.add(item);
        }

        // 行业归属映射
        Map<String, String> industryMap = buildIndustryMap(performanceReports);

        // 填充行业中值与行业均值
        fillIndustryMetrics(resultList, industryMap);

        // 填充同行业内 ROE 3年平均排名
        fillIndustryRanks(resultList, industryMap);

        return resultList;
    }

    private void calculateYearMetrics(StockDupontAnalysis item, StockPerformanceReport pr, StockBalanceSheet bs, int yearIndex) {
        BigDecimal netMargin = null;
        BigDecimal assetTurnover = null;
        BigDecimal equityMultiplier = null;
        BigDecimal roe = null;

        if (pr != null && pr.getNetProfit() != null && pr.getTotalRevenue() != null && pr.getTotalRevenue().compareTo(BigDecimal.ZERO) != 0) {
            netMargin = pr.getNetProfit().multiply(ONE_HUNDRED).divide(pr.getTotalRevenue(), SCALE, RoundingMode.HALF_UP);
        }

        if (pr != null && pr.getTotalRevenue() != null && bs != null && bs.getTotalAssets() != null && bs.getTotalAssets().compareTo(BigDecimal.ZERO) != 0) {
            assetTurnover = pr.getTotalRevenue().divide(bs.getTotalAssets(), SCALE, RoundingMode.HALF_UP);
        }

        if (bs != null && bs.getTotalAssets() != null && bs.getTotalEquity() != null && bs.getTotalEquity().compareTo(BigDecimal.ZERO) != 0) {
            equityMultiplier = bs.getTotalAssets().divide(bs.getTotalEquity(), SCALE, RoundingMode.HALF_UP);
        }

        if (pr != null && pr.getRoe() != null) {
            roe = pr.getRoe();
        }

        if (yearIndex == 1) {
            item.setNetMarginLastYA(netMargin);
            item.setAssetTurnoverLastYA(assetTurnover);
            item.setEquityMultiplierLastYA(equityMultiplier);
            item.setRoeLastYA(roe);
        } else if (yearIndex == 2) {
            item.setNetMarginLast2yA(netMargin);
            item.setAssetTurnoverLast2yA(assetTurnover);
            item.setEquityMultiplierLast2yA(equityMultiplier);
            item.setRoeLast2yA(roe);
        } else if (yearIndex == 3) {
            item.setNetMarginLast3yA(netMargin);
            item.setAssetTurnoverLast3yA(assetTurnover);
            item.setEquityMultiplierLast3yA(equityMultiplier);
            item.setRoeLast3yA(roe);
        }
    }

    private void fillIndustryMetrics(List<StockDupontAnalysis> list, Map<String, String> industryMap) {
        Map<String, List<StockDupontAnalysis>> industryGroups = list.stream()
                .filter(item -> StringUtils.isNotBlank(getIndustry(item.getStockCode(), industryMap)))
                .collect(Collectors.groupingBy(item -> getIndustry(item.getStockCode(), industryMap)));

        Map<String, DupontIndustryMetrics> cache = new HashMap<>();

        for (StockDupontAnalysis item : list) {
            String industry = getIndustry(item.getStockCode(), industryMap);
            if (StringUtils.isBlank(industry)) {
                continue;
            }

            DupontIndustryMetrics metrics = cache.computeIfAbsent(industry, key -> buildIndustryMetrics(industryGroups.getOrDefault(key, List.of())));

            item.setRoe3yAvgIndustryAvg(metrics.getRoe3yAvgAvg());
            item.setRoe3yAvgIndustryMed(metrics.getRoe3yAvgMed());
            item.setRoeLastYAIndustryAvg(metrics.getRoeLastYAAvg());
            item.setRoeLastYAIndustryMed(metrics.getRoeLastYAMed());
            item.setRoeLast2yAIndustryAvg(metrics.getRoeLast2yAAvg());
            item.setRoeLast2yAIndustryMed(metrics.getRoeLast2yAMed());
            item.setRoeLast3yAIndustryAvg(metrics.getRoeLast3yAAvg());
            item.setRoeLast3yAIndustryMed(metrics.getRoeLast3yAMed());

            item.setNetMargin3yAvgIndustryAvg(metrics.getNetMargin3yAvgAvg());
            item.setNetMargin3yAvgIndustryMed(metrics.getNetMargin3yAvgMed());
            item.setNetMarginLastYAIndustryAvg(metrics.getNetMarginLastYAAvg());
            item.setNetMarginLastYAIndustryMed(metrics.getNetMarginLastYAMed());
            item.setNetMarginLast2yAIndustryAvg(metrics.getNetMarginLast2yAAvg());
            item.setNetMarginLast2yAIndustryMed(metrics.getNetMarginLast2yAMed());
            item.setNetMarginLast3yAIndustryAvg(metrics.getNetMarginLast3yAAvg());
            item.setNetMarginLast3yAIndustryMed(metrics.getNetMarginLast3yAMed());

            item.setAssetTurnover3yAvgIndustryAvg(metrics.getAssetTurnover3yAvgAvg());
            item.setAssetTurnover3yAvgIndustryMed(metrics.getAssetTurnover3yAvgMed());
            item.setAssetTurnoverLastYAIndustryAvg(metrics.getAssetTurnoverLastYAAvg());
            item.setAssetTurnoverLastYAIndustryMed(metrics.getAssetTurnoverLastYAMed());
            item.setAssetTurnoverLast2yAIndustryAvg(metrics.getAssetTurnoverLast2yAAvg());
            item.setAssetTurnoverLast2yAIndustryMed(metrics.getAssetTurnoverLast2yAMed());
            item.setAssetTurnoverLast3yAIndustryAvg(metrics.getAssetTurnoverLast3yAAvg());
            item.setAssetTurnoverLast3yAIndustryMed(metrics.getAssetTurnoverLast3yAMed());

            item.setEquityMultiplier3yAvgIndustryAvg(metrics.getEquityMultiplier3yAvgAvg());
            item.setEquityMultiplier3yAvgIndustryMed(metrics.getEquityMultiplier3yAvgMed());
            item.setEquityMultiplierLastYAIndustryAvg(metrics.getEquityMultiplierLastYAAvg());
            item.setEquityMultiplierLastYAIndustryMed(metrics.getEquityMultiplierLastYAMed());
            item.setEquityMultiplierLast2yAIndustryAvg(metrics.getEquityMultiplierLast2yAAvg());
            item.setEquityMultiplierLast2yAIndustryMed(metrics.getEquityMultiplierLast2yAMed());
            item.setEquityMultiplierLast3yAIndustryAvg(metrics.getEquityMultiplierLast3yAAvg());
            item.setEquityMultiplierLast3yAIndustryMed(metrics.getEquityMultiplierLast3yAMed());
        }
    }

    private void fillIndustryRanks(List<StockDupontAnalysis> list, Map<String, String> industryMap) {
        Map<String, List<StockDupontAnalysis>> industryGroups = list.stream()
                .filter(item -> StringUtils.isNotBlank(getIndustry(item.getStockCode(), industryMap)))
                .collect(Collectors.groupingBy(item -> getIndustry(item.getStockCode(), industryMap)));

        for (List<StockDupontAnalysis> group : industryGroups.values()) {
            List<StockDupontAnalysis> rankedList = group.stream()
                    .filter(item -> item.getRoe3yAvg() != null)
                    .sorted(Comparator.comparing(StockDupontAnalysis::getRoe3yAvg).reversed())
                    .toList();

            for (int i = 0; i < rankedList.size(); i++) {
                rankedList.get(i).setRoe3yAvgRank(BigDecimal.valueOf(i + 1));
            }
        }

        // 计算所有标的的杜邦质量评分与智能结论
        for (StockDupontAnalysis item : list) {
            calculateScoreAndConclusion(item);
        }
    }

    public void calculateScoreAndConclusion(StockDupontAnalysis item) {
        if (item == null) return;
        BigDecimal roe = item.getRoe3yAvg() != null ? item.getRoe3yAvg() : item.getRoeLastYA();
        BigDecimal netMargin = item.getNetMargin3yAvg() != null ? item.getNetMargin3yAvg() : item.getNetMarginLastYA();
        BigDecimal turnover = item.getAssetTurnover3yAvg() != null ? item.getAssetTurnover3yAvg() : item.getAssetTurnoverLastYA();
        BigDecimal equityMult = item.getEquityMultiplier3yAvg() != null ? item.getEquityMultiplier3yAvg() : item.getEquityMultiplierLastYA();

        BigDecimal netMarginMed = item.getNetMargin3yAvgIndustryMed();
        BigDecimal turnoverMed = item.getAssetTurnover3yAvgIndustryMed();

        double score = 0.0;

        // 1. ROE 水平 (35 分)
        if (roe != null) {
            double r = roe.doubleValue();
            if (r >= 20.0) {
                score += 35.0;
            } else if (r >= 15.0) {
                score += 30.0 + (r - 15.0);
            } else if (r >= 10.0) {
                score += 22.0 + (r - 10.0) * 1.6;
            } else if (r >= 5.0) {
                score += 12.0 + (r - 5.0) * 2.0;
            } else if (r > 0) {
                score += r * 2.4;
            }
        }

        // 2. 净利率盈利驱动 (30 分)
        if (netMargin != null) {
            double nm = netMargin.doubleValue();
            double nmMed = (netMarginMed != null && netMarginMed.doubleValue() > 0) ? netMarginMed.doubleValue() : 8.0;
            if (nm >= nmMed) {
                double leadRatio = Math.min(2.0, nm / nmMed);
                score += 18.0 + (leadRatio - 1.0) * 12.0;
            } else if (nm > 0) {
                score += Math.max(5.0, 18.0 * (nm / nmMed));
            }
        }

        // 3. 资产周转率运营效率 (20 分)
        if (turnover != null) {
            double to = turnover.doubleValue();
            double toMed = (turnoverMed != null && turnoverMed.doubleValue() > 0) ? turnoverMed.doubleValue() : 0.8;
            if (to >= toMed) {
                double leadRatio = Math.min(2.0, to / toMed);
                score += 12.0 + (leadRatio - 1.0) * 8.0;
            } else if (to > 0) {
                score += Math.max(3.0, 12.0 * (to / toMed));
            }
        }

        // 4. 权益乘数财务杠杆健康度 (15 分)
        if (equityMult != null) {
            double em = equityMult.doubleValue();
            if (em >= 1.2 && em <= 2.2) {
                score += 15.0; // 黄金杠杆区间
            } else if (em > 2.2 && em <= 2.6) {
                score += 12.0; // 略高但可控
            } else if (em > 2.6 && em <= 3.2) {
                score += 8.0;  // 偏高
            } else if (em > 3.2 && em <= 4.5) {
                score += 4.0;  // 高杠杆风险
            } else if (em > 0 && em < 1.2) {
                score += 12.0; // 杠杆很低，财务安全
            }
        }

        int finalScore = Math.max(10, Math.min(99, (int) Math.round(score)));
        item.setQualityScore(BigDecimal.valueOf(finalScore));

        if (finalScore >= 80) {
            item.setQualityLevel("优秀");
        } else if (finalScore >= 65) {
            item.setQualityLevel("良好");
        } else if (finalScore >= 50) {
            item.setQualityLevel("中等");
        } else {
            item.setQualityLevel("较差");
        }

        // 结论生成
        item.setConclusion(generateConclusion(item));
    }

    private String generateConclusion(StockDupontAnalysis item) {
        BigDecimal roe = item.getRoe3yAvg() != null ? item.getRoe3yAvg() : item.getRoeLastYA();
        BigDecimal netMargin = item.getNetMargin3yAvg() != null ? item.getNetMargin3yAvg() : item.getNetMarginLastYA();
        BigDecimal turnover = item.getAssetTurnover3yAvg() != null ? item.getAssetTurnover3yAvg() : item.getAssetTurnoverLastYA();
        BigDecimal equityMult = item.getEquityMultiplier3yAvg() != null ? item.getEquityMultiplier3yAvg() : item.getEquityMultiplierLastYA();

        BigDecimal netMarginMed = item.getNetMargin3yAvgIndustryMed();
        BigDecimal turnoverMed = item.getAssetTurnover3yAvgIndustryMed();

        boolean highRoe = roe != null && roe.compareTo(new BigDecimal("15")) >= 0;
        boolean moderateRoe = roe != null && roe.compareTo(new BigDecimal("10")) >= 0;
        boolean highMargin = netMargin != null && (netMarginMed != null ? netMargin.compareTo(netMarginMed) > 0 : netMargin.compareTo(new BigDecimal("10")) >= 0);
        boolean highTurnover = turnover != null && (turnoverMed != null ? turnover.compareTo(turnoverMed) > 0 : turnover.compareTo(new BigDecimal("1.0")) >= 0);
        boolean highLeverage = equityMult != null && equityMult.compareTo(new BigDecimal("2.5")) > 0;
        boolean safeLeverage = equityMult != null && equityMult.compareTo(new BigDecimal("2.2")) <= 0;

        if (highRoe) {
            if (highMargin && safeLeverage) {
                return "ROE较高，主要来自盈利能力，杠杆可控。";
            } else if (highMargin && highTurnover) {
                return "盈利与周转双轮驱动，经营质量优秀，财务稳健。";
            } else if (highMargin && highLeverage) {
                return "盈利能力较强，周转一般，杠杆偏高。";
            } else if (highTurnover && safeLeverage) {
                return "运营周转效率极佳，杠杆可控，盈利质量良好。";
            } else if (highLeverage) {
                return "ROE较高但主要依赖财务杠杆，需注意杠杆与偿债压力。";
            }
            return "ROE表现优异，综合经营质量领先行业。";
        } else if (moderateRoe) {
            if (highMargin) {
                return "盈利能力较好，周转尚可，杠杆可控。";
            } else if (highTurnover) {
                return "运营周转良好，盈利一般，财务稳健。";
            } else if (highLeverage) {
                return "盈利与周转一般，财务杠杆偏高。";
            }
            return "经营质量稳健，ROE处于行业中游水平。";
        } else {
            if (highLeverage) {
                return "ROE偏低且杠杆偏高，盈利与周转待改善。";
            }
            return "盈利与周转表现偏弱，综合质量有待提升。";
        }
    }

    private DupontIndustryMetrics buildIndustryMetrics(List<StockDupontAnalysis> group) {
        DupontIndustryMetrics metrics = new DupontIndustryMetrics();
        metrics.setRoe3yAvgAvg(average(group, StockDupontAnalysis::getRoe3yAvg));
        metrics.setRoe3yAvgMed(median(group, StockDupontAnalysis::getRoe3yAvg));
        metrics.setRoeLastYAAvg(average(group, StockDupontAnalysis::getRoeLastYA));
        metrics.setRoeLastYAMed(median(group, StockDupontAnalysis::getRoeLastYA));
        metrics.setRoeLast2yAAvg(average(group, StockDupontAnalysis::getRoeLast2yA));
        metrics.setRoeLast2yAMed(median(group, StockDupontAnalysis::getRoeLast2yA));
        metrics.setRoeLast3yAAvg(average(group, StockDupontAnalysis::getRoeLast3yA));
        metrics.setRoeLast3yAMed(median(group, StockDupontAnalysis::getRoeLast3yA));

        metrics.setNetMargin3yAvgAvg(average(group, StockDupontAnalysis::getNetMargin3yAvg));
        metrics.setNetMargin3yAvgMed(median(group, StockDupontAnalysis::getNetMargin3yAvg));
        metrics.setNetMarginLastYAAvg(average(group, StockDupontAnalysis::getNetMarginLastYA));
        metrics.setNetMarginLastYAMed(median(group, StockDupontAnalysis::getNetMarginLastYA));
        metrics.setNetMarginLast2yAAvg(average(group, StockDupontAnalysis::getNetMarginLast2yA));
        metrics.setNetMarginLast2yAMed(median(group, StockDupontAnalysis::getNetMarginLast2yA));
        metrics.setNetMarginLast3yAAvg(average(group, StockDupontAnalysis::getNetMarginLast3yA));
        metrics.setNetMarginLast3yAMed(median(group, StockDupontAnalysis::getNetMarginLast3yA));

        metrics.setAssetTurnover3yAvgAvg(average(group, StockDupontAnalysis::getAssetTurnover3yAvg));
        metrics.setAssetTurnover3yAvgMed(median(group, StockDupontAnalysis::getAssetTurnover3yAvg));
        metrics.setAssetTurnoverLastYAAvg(average(group, StockDupontAnalysis::getAssetTurnoverLastYA));
        metrics.setAssetTurnoverLastYAMed(median(group, StockDupontAnalysis::getAssetTurnoverLastYA));
        metrics.setAssetTurnoverLast2yAAvg(average(group, StockDupontAnalysis::getAssetTurnoverLast2yA));
        metrics.setAssetTurnoverLast2yAMed(median(group, StockDupontAnalysis::getAssetTurnoverLast2yA));
        metrics.setAssetTurnoverLast3yAAvg(average(group, StockDupontAnalysis::getAssetTurnoverLast3yA));
        metrics.setAssetTurnoverLast3yAMed(median(group, StockDupontAnalysis::getAssetTurnoverLast3yA));

        metrics.setEquityMultiplier3yAvgAvg(average(group, StockDupontAnalysis::getEquityMultiplier3yAvg));
        metrics.setEquityMultiplier3yAvgMed(median(group, StockDupontAnalysis::getEquityMultiplier3yAvg));
        metrics.setEquityMultiplierLastYAAvg(average(group, StockDupontAnalysis::getEquityMultiplierLastYA));
        metrics.setEquityMultiplierLastYAMed(median(group, StockDupontAnalysis::getEquityMultiplierLastYA));
        metrics.setEquityMultiplierLast2yAAvg(average(group, StockDupontAnalysis::getEquityMultiplierLast2yA));
        metrics.setEquityMultiplierLast2yAMed(median(group, StockDupontAnalysis::getEquityMultiplierLast2yA));
        metrics.setEquityMultiplierLast3yAAvg(average(group, StockDupontAnalysis::getEquityMultiplierLast3yA));
        metrics.setEquityMultiplierLast3yAMed(median(group, StockDupontAnalysis::getEquityMultiplierLast3yA));

        return metrics;
    }

    private BigDecimal average(List<StockDupontAnalysis> list, Function<StockDupontAnalysis, BigDecimal> getter) {
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

    private BigDecimal median(List<StockDupontAnalysis> list, Function<StockDupontAnalysis, BigDecimal> getter) {
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
        return mid1.add(mid2).divide(TWO, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverage(BigDecimal... values) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (BigDecimal val : values) {
            if (val != null) {
                sum = sum.add(val);
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP);
    }

    private List<Integer> getLatestThreeAnnualYears(List<StockPerformanceReport> prList, List<StockBalanceSheet> bsList) {
        Set<Integer> years = new HashSet<>();
        for (StockPerformanceReport r : prList) {
            if (r != null && StockUtils.isAnnualReport(r.getReportDate())) {
                years.add(r.getReportDate().getYear());
            }
        }
        for (StockBalanceSheet b : bsList) {
            if (b != null && StockUtils.isAnnualReport(b.getReportDate())) {
                years.add(b.getReportDate().getYear());
            }
        }
        return years.stream()
                .sorted(Comparator.reverseOrder())
                .limit(3)
                .collect(Collectors.toList());
    }

    private Map<String, String> buildIndustryMap(List<StockPerformanceReport> performanceReports) {
        return performanceReports.stream()
                .filter(r -> r != null && StringUtils.isNotBlank(r.getStockCode()))
                .filter(r -> StringUtils.isNotBlank(r.getIndustry()) && r.getReportDate() != null)
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

    private void copyProperties(StockDupontAnalysis source, StockDupontAnalysis target) {
        target.setStockCode(source.getStockCode());
        target.setStockName(source.getStockName());

        target.setRoe3yAvg(source.getRoe3yAvg());
        target.setRoe3yAvgIndustryMed(source.getRoe3yAvgIndustryMed());
        target.setRoe3yAvgIndustryAvg(source.getRoe3yAvgIndustryAvg());
        target.setRoeLast3yA(source.getRoeLast3yA());
        target.setRoeLast3yAIndustryMed(source.getRoeLast3yAIndustryMed());
        target.setRoeLast3yAIndustryAvg(source.getRoeLast3yAIndustryAvg());
        target.setRoeLast2yA(source.getRoeLast2yA());
        target.setRoeLast2yAIndustryMed(source.getRoeLast2yAIndustryMed());
        target.setRoeLast2yAIndustryAvg(source.getRoeLast2yAIndustryAvg());
        target.setRoeLastYA(source.getRoeLastYA());
        target.setRoeLastYAIndustryMed(source.getRoeLastYAIndustryMed());
        target.setRoeLastYAIndustryAvg(source.getRoeLastYAIndustryAvg());

        target.setNetMargin3yAvg(source.getNetMargin3yAvg());
        target.setNetMargin3yAvgIndustryMed(source.getNetMargin3yAvgIndustryMed());
        target.setNetMargin3yAvgIndustryAvg(source.getNetMargin3yAvgIndustryAvg());
        target.setNetMarginLast3yA(source.getNetMarginLast3yA());
        target.setNetMarginLast3yAIndustryMed(source.getNetMarginLast3yAIndustryMed());
        target.setNetMarginLast3yAIndustryAvg(source.getNetMarginLast3yAIndustryAvg());
        target.setNetMarginLast2yA(source.getNetMarginLast2yA());
        target.setNetMarginLast2yAIndustryMed(source.getNetMarginLast2yAIndustryMed());
        target.setNetMarginLast2yAIndustryAvg(source.getNetMarginLast2yAIndustryAvg());
        target.setNetMarginLastYA(source.getNetMarginLastYA());
        target.setNetMarginLastYAIndustryMed(source.getNetMarginLastYAIndustryMed());
        target.setNetMarginLastYAIndustryAvg(source.getNetMarginLastYAIndustryAvg());

        target.setAssetTurnover3yAvg(source.getAssetTurnover3yAvg());
        target.setAssetTurnover3yAvgIndustryMed(source.getAssetTurnover3yAvgIndustryMed());
        target.setAssetTurnover3yAvgIndustryAvg(source.getAssetTurnover3yAvgIndustryAvg());
        target.setAssetTurnoverLast3yA(source.getAssetTurnoverLast3yA());
        target.setAssetTurnoverLast3yAIndustryMed(source.getAssetTurnoverLast3yAIndustryMed());
        target.setAssetTurnoverLast3yAIndustryAvg(source.getAssetTurnoverLast3yAIndustryAvg());
        target.setAssetTurnoverLast2yA(source.getAssetTurnoverLast2yA());
        target.setAssetTurnoverLast2yAIndustryMed(source.getAssetTurnoverLast2yAIndustryMed());
        target.setAssetTurnoverLast2yAIndustryAvg(source.getAssetTurnoverLast2yAIndustryAvg());
        target.setAssetTurnoverLastYA(source.getAssetTurnoverLastYA());
        target.setAssetTurnoverLastYAIndustryMed(source.getAssetTurnoverLastYAIndustryMed());
        target.setAssetTurnoverLastYAIndustryAvg(source.getAssetTurnoverLastYAIndustryAvg());

        target.setEquityMultiplier3yAvg(source.getEquityMultiplier3yAvg());
        target.setEquityMultiplier3yAvgIndustryMed(source.getEquityMultiplier3yAvgIndustryMed());
        target.setEquityMultiplier3yAvgIndustryAvg(source.getEquityMultiplier3yAvgIndustryAvg());
        target.setEquityMultiplierLast3yA(source.getEquityMultiplierLast3yA());
        target.setEquityMultiplierLast3yAIndustryMed(source.getEquityMultiplierLast3yAIndustryMed());
        target.setEquityMultiplierLast3yAIndustryAvg(source.getEquityMultiplierLast3yAIndustryAvg());
        target.setEquityMultiplierLast2yA(source.getEquityMultiplierLast2yA());
        target.setEquityMultiplierLast2yAIndustryMed(source.getEquityMultiplierLast2yAIndustryMed());
        target.setEquityMultiplierLast2yAIndustryAvg(source.getEquityMultiplierLast2yAIndustryAvg());
        target.setEquityMultiplierLastYA(source.getEquityMultiplierLastYA());
        target.setEquityMultiplierLastYAIndustryMed(source.getEquityMultiplierLastYAIndustryMed());
        target.setEquityMultiplierLastYAIndustryAvg(source.getEquityMultiplierLastYAIndustryAvg());

        target.setRoe3yAvgRank(source.getRoe3yAvgRank());
        target.setIndustry(source.getIndustry());
        target.setQualityScore(source.getQualityScore());
        target.setQualityLevel(source.getQualityLevel());
        target.setConclusion(source.getConclusion());
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(String code, String name, List<StockZhDupontComparisonEm> list) {
        StockDupontAnalysis stockDupontAnalysis = stockDupontAnalysisRepository.findByStockCode(code);

        if (stockDupontAnalysis == null) {
            stockDupontAnalysis = new StockDupontAnalysis();
        }
        stockDupontAnalysis.setStockCode(code);
        stockDupontAnalysis.setStockName(name);

        String plainCode = StockUtils.getPlainCode(code);

        for (StockZhDupontComparisonEm data : list) {
            String c = data.getCode();
            if (plainCode.equals(c)) {
                stockDupontAnalysis.setRoe3yAvgRank(data.getRoe3yAvgRank());

                stockDupontAnalysis.setRoe3yAvg(data.getRoe3yAvg());
                stockDupontAnalysis.setRoeLast3yA(data.getRoe22a());
                stockDupontAnalysis.setRoeLast2yA(data.getRoe23a());
                stockDupontAnalysis.setRoeLastYA(data.getRoe24a());

                stockDupontAnalysis.setNetMargin3yAvg(data.getNetMargin3yAvg());
                stockDupontAnalysis.setNetMarginLast3yA(data.getNetMargin22a());
                stockDupontAnalysis.setNetMarginLast2yA(data.getNetMargin23a());
                stockDupontAnalysis.setNetMarginLastYA(data.getNetMargin24a());

                stockDupontAnalysis.setAssetTurnover3yAvg(data.getAssetTurnover3yAvg());
                stockDupontAnalysis.setAssetTurnoverLast3yA(data.getAssetTurnover22a());
                stockDupontAnalysis.setAssetTurnoverLast2yA(data.getAssetTurnover23a());
                stockDupontAnalysis.setAssetTurnoverLastYA(data.getAssetTurnover24a());

                stockDupontAnalysis.setEquityMultiplier3yAvg(data.getEquityMultiplier3yAvg());
                stockDupontAnalysis.setEquityMultiplierLast3yA(data.getEquityMultiplier22a());
                stockDupontAnalysis.setEquityMultiplierLast2yA(data.getEquityMultiplier23a());
                stockDupontAnalysis.setEquityMultiplierLastYA(data.getEquityMultiplier24a());

                stockDupontAnalysis.setCreatedAt(LocalDateTime.now());
            } else if ("行业中值".equals(c)) {
                stockDupontAnalysis.setRoe3yAvgIndustryMed(data.getRoe3yAvg());
                stockDupontAnalysis.setRoeLast3yAIndustryMed(data.getRoe22a());
                stockDupontAnalysis.setRoeLast2yAIndustryMed(data.getRoe23a());
                stockDupontAnalysis.setRoeLastYAIndustryMed(data.getRoe24a());

                stockDupontAnalysis.setNetMargin3yAvgIndustryMed(data.getNetMargin3yAvg());
                stockDupontAnalysis.setNetMarginLast3yAIndustryMed(data.getNetMargin22a());
                stockDupontAnalysis.setNetMarginLast2yAIndustryMed(data.getNetMargin23a());
                stockDupontAnalysis.setNetMarginLastYAIndustryMed(data.getNetMargin24a());

                stockDupontAnalysis.setAssetTurnover3yAvgIndustryMed(data.getAssetTurnover3yAvg());
                stockDupontAnalysis.setAssetTurnoverLast3yAIndustryMed(data.getAssetTurnover22a());
                stockDupontAnalysis.setAssetTurnoverLast2yAIndustryMed(data.getAssetTurnover23a());
                stockDupontAnalysis.setAssetTurnoverLastYAIndustryMed(data.getAssetTurnover24a());

                stockDupontAnalysis.setEquityMultiplier3yAvgIndustryMed(data.getEquityMultiplier3yAvg());
                stockDupontAnalysis.setEquityMultiplierLast3yAIndustryMed(data.getEquityMultiplier22a());
                stockDupontAnalysis.setEquityMultiplierLast2yAIndustryMed(data.getEquityMultiplier23a());
                stockDupontAnalysis.setEquityMultiplierLastYAIndustryMed(data.getEquityMultiplier24a());

                stockDupontAnalysis.setCreatedAt(LocalDateTime.now());
            } else if ("行业平均".equals(c)) {
                stockDupontAnalysis.setRoe3yAvgIndustryAvg(data.getRoe3yAvg());
                stockDupontAnalysis.setRoeLast3yAIndustryAvg(data.getRoe22a());
                stockDupontAnalysis.setRoeLast2yAIndustryAvg(data.getRoe23a());
                stockDupontAnalysis.setRoeLastYAIndustryAvg(data.getRoe24a());

                stockDupontAnalysis.setNetMargin3yAvgIndustryAvg(data.getNetMargin3yAvg());
                stockDupontAnalysis.setNetMarginLast3yAIndustryAvg(data.getNetMargin22a());
                stockDupontAnalysis.setNetMarginLast2yAIndustryAvg(data.getNetMargin23a());
                stockDupontAnalysis.setNetMarginLastYAIndustryAvg(data.getNetMargin24a());

                stockDupontAnalysis.setAssetTurnover3yAvgIndustryAvg(data.getAssetTurnover3yAvg());
                stockDupontAnalysis.setAssetTurnoverLast3yAIndustryAvg(data.getAssetTurnover22a());
                stockDupontAnalysis.setAssetTurnoverLast2yAIndustryAvg(data.getAssetTurnover23a());
                stockDupontAnalysis.setAssetTurnoverLastYAIndustryAvg(data.getAssetTurnover24a());

                stockDupontAnalysis.setEquityMultiplier3yAvgIndustryAvg(data.getEquityMultiplier3yAvg());
                stockDupontAnalysis.setEquityMultiplierLast3yAIndustryAvg(data.getEquityMultiplier22a());
                stockDupontAnalysis.setEquityMultiplierLast2yAIndustryAvg(data.getEquityMultiplier23a());
                stockDupontAnalysis.setEquityMultiplierLastYAIndustryAvg(data.getEquityMultiplier24a());

                stockDupontAnalysis.setCreatedAt(LocalDateTime.now());
            }
        }
        stockDupontAnalysisRepository.save(stockDupontAnalysis);
    }

}
