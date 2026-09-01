package com.brotherc.aquant.dividend.service;

import com.brotherc.aquant.dividend.entity.StockDividend;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.integration.akshare.model.StockFhpsDetailEm;
import com.brotherc.aquant.integration.akshare.model.StockFhpsDetailThs;
import com.brotherc.aquant.dividend.model.vo.AnnualDividendSnapshotVO;
import com.brotherc.aquant.dividend.model.vo.DividendOverviewVO;
import com.brotherc.aquant.dividend.model.vo.StockDividendDetailReqVO;
import com.brotherc.aquant.dividend.model.vo.StockDividendDetailVO;
import com.brotherc.aquant.dividend.model.vo.StockDividendStatPageReqVO;
import com.brotherc.aquant.dividend.model.vo.StockDividendStatVO;
import com.brotherc.aquant.dividend.repository.StockDividendRepository;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.indicator.repository.StockValuationMetricsRepository;
import com.brotherc.aquant.indicator.repository.StockDupontAnalysisRepository;
import com.brotherc.aquant.indicator.entity.StockValuationMetrics;
import com.brotherc.aquant.indicator.entity.StockDupontAnalysis;
import com.brotherc.aquant.watchlist.repository.StockWatchlistStockRepository;
import com.brotherc.aquant.watchlist.entity.StockWatchlistStock;
import com.brotherc.aquant.integration.akshare.service.AKShareDividendService;
import com.brotherc.aquant.common.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDividendService {

    private final AKShareDividendService akShareDividendService;

    private final StockDividendRepository stockDividendRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockValuationMetricsRepository stockValuationMetricsRepository;
    private final StockDupontAnalysisRepository stockDupontAnalysisRepository;
    private final StockWatchlistStockRepository stockWatchlistStockRepository;

    /**
     * 获取分红概览看板数据
     */
    public DividendOverviewVO getOverview(Long watchlistGroupId) {
        List<StockDividendStatVO> all = calcFullDividendStats(null, null, null, null, null);

        Set<String> watchlistCodes;
        if (watchlistGroupId != null) {
            watchlistCodes = stockWatchlistStockRepository.findByGroupIdOrderBySortNoDesc(watchlistGroupId)
                    .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
        } else {
            watchlistCodes = stockWatchlistStockRepository.findAll()
                    .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
        }

        int highDividendCount = 0;
        int consecutiveDividendCount = 0;
        int watchlistDividendCount = 0;
        int todayFocusCount = 0;

        LocalDate today = LocalDate.now();

        for (StockDividendStatVO item : all) {
            BigDecimal dy = item.getDividendYield();
            if (dy != null && dy.compareTo(new BigDecimal("3.0")) >= 0) {
                highDividendCount++;
            }
            if (item.getConsecutiveYears() != null && item.getConsecutiveYears() >= 3) {
                consecutiveDividendCount++;
            }
            if (watchlistCodes.contains(item.getStockCode())) {
                if ((item.getConsecutiveYears() != null && item.getConsecutiveYears() >= 1)
                        || (dy != null && dy.compareTo(BigDecimal.ZERO) > 0)) {
                    watchlistDividendCount++;
                }
            }
            if (item.getLatestAnnouncementDate() != null
                    && !item.getLatestAnnouncementDate().isBefore(today.minusDays(30))
                    && dy != null && dy.compareTo(new BigDecimal("3.5")) >= 0) {
                todayFocusCount++;
            }
        }

        DividendOverviewVO vo = new DividendOverviewVO();
        vo.setHighDividendOpportunityCount(highDividendCount);
        vo.setConsecutiveDividendCount(consecutiveDividendCount);
        vo.setWatchlistDividendCount(watchlistDividendCount);
        vo.setTodayFocusCount(todayFocusCount);
        return vo;
    }

    /**
     * 分页查询股票分红数据
     */
    public Page<StockDividendStatVO> pageDividendStats(StockDividendStatPageReqVO reqVO, Pageable pageable) {
        List<StockDividendStatVO> all = calcFullDividendStats(
                reqVO.getRecentYears(), reqVO.getMinAvgDividend(),
                reqVO.getStockCode(), reqVO.getStockName(), reqVO.getWatchlistGroupId()
        );

        // PEG 范围筛选
        if (StringUtils.isNotBlank(reqVO.getPegRange())) {
            all = all.stream().filter(item -> {
                BigDecimal peg = item.getPeg();
                if (peg == null) return false;
                if ("1".equals(reqVO.getPegRange())) {
                    return peg.compareTo(BigDecimal.ZERO) >= 0 && peg.compareTo(new BigDecimal("0.5")) < 0;
                } else if ("2".equals(reqVO.getPegRange())) {
                    return peg.compareTo(new BigDecimal("0.5")) >= 0 && peg.compareTo(BigDecimal.ONE) <= 0;
                }
                return true;
            }).collect(Collectors.toList());
        }

        // 快捷 Tab 过滤
        if (StringUtils.isNotBlank(reqVO.getQuickTab())) {
            String tab = reqVO.getQuickTab().toUpperCase();
            if ("HIGH_DIVIDEND".equals(tab)) {
                all = all.stream()
                        .filter(item -> item.getDividendScore() != null
                                && item.getDividendYield() != null
                                && item.getDividendYield().compareTo(new BigDecimal("3")) >= 0)
                        .collect(Collectors.toList());
            } else if ("STABLE_DIVIDEND".equals(tab)) {
                all = all.stream().filter(item -> item.getDividendScore() != null
                                && item.getDividendScore().compareTo(new BigDecimal("65")) >= 0
                                && item.getConsecutiveYears() != null && item.getConsecutiveYears() >= 3)
                        .collect(Collectors.toList());
            } else if ("DIVIDEND_GROWTH".equals(tab)) {
                all = all.stream().filter(item -> item.getDividendScore() != null
                                && item.getDividendScore().compareTo(new BigDecimal("50")) >= 0
                                && item.getDividendGrowth3y() != null
                                && item.getDividendGrowth3y().compareTo(BigDecimal.ZERO) > 0)
                        .collect(Collectors.toList());
            } else if ("MY_WATCHLIST".equals(tab)) {
                Set<String> watchlistCodes = stockWatchlistStockRepository.findAll()
                        .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
                all = all.stream().filter(item -> watchlistCodes.contains(item.getStockCode()))
                        .collect(Collectors.toList());
            }
        }

        // 排序
        all.sort(buildComparator(pageable.getSort(), reqVO.getQuickTab()));

        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());

        List<StockDividendStatVO> content = start >= all.size() ? Collections.emptyList() : all.subList(start, end);

        return new PageImpl<>(content, pageable, all.size());
    }

    /**
     * 计算并组装完整分红数据与打分
     */
    public List<StockDividendStatVO> calcFullDividendStats(
            Integer recentYears, BigDecimal minAvgDividend, String stockCodeQuery, String stockNameQuery,
            Long watchlistGroupId) {

        int nYears = (recentYears != null && recentYears > 0) ? recentYears : 3;

        Set<String> watchlistCodes = null;
        if (watchlistGroupId != null) {
            watchlistCodes = stockWatchlistStockRepository.findByGroupIdOrderBySortNoDesc(watchlistGroupId)
                    .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
            if (watchlistCodes.isEmpty()) {
                return new ArrayList<>();
            }
        }

        // 查询所有分红记录
        List<StockDividend> allDividends = stockDividendRepository.findAll();
        if (CollectionUtils.isEmpty(allDividends)) {
            return new ArrayList<>();
        }

        // 缓存辅助行情与指标数据
        Map<String, StockQuote> stockQuoteMap = stockQuoteRepository.findAll()
                .stream().collect(Collectors.toMap(o -> o.getCode().length() > 6 ? o.getCode().substring(2) : o.getCode(), o -> o, (a, b) -> a));

        Map<String, StockValuationMetrics> valuationMap = stockValuationMetricsRepository.findAll()
                .stream().collect(Collectors.toMap(o -> o.getStockCode().length() > 6 ? o.getStockCode().substring(2) : o.getStockCode(), o -> o, (a, b) -> a));

        Map<String, StockDupontAnalysis> dupontMap = stockDupontAnalysisRepository.findAll()
                .stream().collect(Collectors.toMap(o -> o.getStockCode().length() > 6 ? o.getStockCode().substring(2) : o.getStockCode(), o -> o, (a, b) -> a));

        // 按股票分组分红记录
        Map<String, List<StockDividend>> groupByStock = allDividends.stream()
                .collect(Collectors.groupingBy(StockDividend::getStockCode));

        List<StockDividendStatVO> result = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();

        for (Map.Entry<String, List<StockDividend>> entry : groupByStock.entrySet()) {
            String stockCode = entry.getKey();
            List<StockDividend> dividends = entry.getValue();
            if (CollectionUtils.isEmpty(dividends)) continue;

            String stockName = dividends.get(0).getStockName();

            boolean matchCode = StringUtils.isBlank(stockCodeQuery) || stockCode.contains(stockCodeQuery);
            boolean matchName = StringUtils.isBlank(stockNameQuery) || stockName.contains(stockNameQuery);
            boolean matchWatchlist = watchlistCodes == null || watchlistCodes.contains(stockCode);

            if (!matchCode || !matchName || !matchWatchlist) {
                continue;
            }

            // 按公告日期降序排序
            dividends.sort(Comparator.comparing(StockDividend::getLatestAnnouncementDate,
                    Comparator.nullsLast(Comparator.reverseOrder())));

            // 基础信息与辅助指标
            StockQuote quote = stockQuoteMap.get(stockCode);
            BigDecimal latestPrice = quote != null ? quote.getLatestPrice() : null;

            StockValuationMetrics valuation = valuationMap.get(stockCode);
            String industry = valuation != null ? valuation.getIndustry() : null;
            BigDecimal peg = valuation != null ? valuation.getPeg() : null;
            BigDecimal pe = valuation != null ? valuation.getPeTtm() : null;
            BigDecimal peIndustryAvg = valuation != null ? valuation.getPeTtmIndustryAvg() : null;

            StockDupontAnalysis dupont = dupontMap.get(stockCode);
            BigDecimal roeActual = dupont != null ? dupont.getRoeLastYA() : null;
            BigDecimal roe3yAvg = dupont != null ? dupont.getRoe3yAvg() : null;
            BigDecimal roeIndustryAvg = dupont != null ? dupont.getRoe3yAvgIndustryMed() : null;

            // 历史年度聚合 (按报告期或公告年份分组累计每股分红)
            Map<Integer, BigDecimal> annualCashDivMap = new HashMap<>();
            Map<Integer, BigDecimal> annualYieldMap = new HashMap<>();
            Map<Integer, BigDecimal> annualEpsMap = new HashMap<>();
            Map<Integer, BigDecimal> annualTransferMap = new HashMap<>();

            for (StockDividend d : dividends) {
                String reportDate = d.getReportDate();
                if (StringUtils.isNotBlank(reportDate)
                        && !reportDate.endsWith("0630") && !reportDate.endsWith("1231")) {
                    continue;
                }

                Integer year = null;
                if (StringUtils.isNotBlank(reportDate) && reportDate.length() >= 4) {
                    try {
                        year = Integer.parseInt(reportDate.substring(0, 4));
                    } catch (Exception ignored) {}
                }
                if (year == null && d.getLatestAnnouncementDate() != null) {
                    year = d.getLatestAnnouncementDate().getYear();
                }
                if (year == null) continue;

                BigDecimal cash = d.getCashDividendRatio() != null ? d.getCashDividendRatio() : BigDecimal.ZERO;
                annualCashDivMap.merge(year, cash, BigDecimal::add);

                if (d.getDividendYield() != null && !annualYieldMap.containsKey(year)) {
                    annualYieldMap.put(year, d.getDividendYield());
                }
                if (d.getEarningsPerShare() != null && !annualEpsMap.containsKey(year)) {
                    annualEpsMap.put(year, d.getEarningsPerShare());
                }

                BigDecimal bonus = d.getBonusShareRatio() != null ? d.getBonusShareRatio() : BigDecimal.ZERO;
                BigDecimal transfer = d.getTransferShareRatio() != null ? d.getTransferShareRatio() : BigDecimal.ZERO;
                annualTransferMap.merge(year, bonus.add(transfer), BigDecimal::add);
            }

            // 评分统一使用最近完整年度，避免将尚未披露完整的当年分红与历史全年数据比较。
            int maxYear = annualCashDivMap.keySet().stream()
                    .filter(year -> year < currentYear)
                    .max(Integer::compareTo)
                    .orElseGet(() -> annualCashDivMap.keySet().stream().max(Integer::compareTo).orElse(currentYear));

            // 最近完整年度分红与转股
            BigDecimal latestYearDividend = annualCashDivMap.getOrDefault(maxYear, BigDecimal.ZERO);
            BigDecimal latestYearTransfer = annualTransferMap.getOrDefault(maxYear, BigDecimal.ZERO);

            // 最近 N 年平均分红
            BigDecimal totalNDiv = BigDecimal.ZERO;
            int nDivCount = 0;
            for (int y = maxYear; y > maxYear - nYears; y--) {
                totalNDiv = totalNDiv.add(annualCashDivMap.getOrDefault(y, BigDecimal.ZERO));
                nDivCount++;
            }
            BigDecimal avgDividend = nDivCount > 0
                    ? totalNDiv.divide(BigDecimal.valueOf(nDivCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            if (minAvgDividend != null && avgDividend.compareTo(minAvgDividend) < 0) {
                continue;
            }

            // 股息率优先使用年度每股分红和最新价计算，避免根据数值大小猜测第三方接口的百分比单位。
            BigDecimal latestDps = latestYearDividend.divide(BigDecimal.TEN, 6, RoundingMode.HALF_UP);
            BigDecimal latestDividendYield = null;
            if (latestPrice != null && latestPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dps = latestDps;
                latestDividendYield = dps.divide(latestPrice, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            } else if (annualYieldMap.get(maxYear) != null) {
                latestDividendYield = annualYieldMap.get(maxYear);
            }
            if (latestDividendYield != null) {
                latestDividendYield = latestDividendYield.setScale(2, RoundingMode.HALF_UP);
            }

            // 连续分红年数
            int consecutiveYears = 0;
            for (int y = maxYear; y >= maxYear - 20; y--) {
                BigDecimal c = annualCashDivMap.get(y);
                if (c != null && c.compareTo(BigDecimal.ZERO) > 0) {
                    consecutiveYears++;
                } else {
                    break;
                }
            }

            // 近 3 年分红复合增长率；缺少基期数据时保持为空，不再虚构正增长。
            BigDecimal div3yAgo = annualCashDivMap.get(maxYear - 3);
            BigDecimal dividendGrowth3y = null;
            if (div3yAgo != null && div3yAgo.compareTo(BigDecimal.ZERO) > 0
                    && latestYearDividend.compareTo(BigDecimal.ZERO) > 0) {
                double growth = (Math.pow(latestYearDividend.divide(div3yAgo, 8, RoundingMode.HALF_UP).doubleValue(), 1.0 / 3.0) - 1) * 100;
                dividendGrowth3y = BigDecimal.valueOf(growth).setScale(1, RoundingMode.HALF_UP);
            }

            BigDecimal annualEps = annualEpsMap.get(maxYear);
            BigDecimal payoutRatio = annualEps != null && annualEps.compareTo(BigDecimal.ZERO) > 0
                    ? latestDps.divide(annualEps, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP)
                    : null;

            BigDecimal profitabilityRoe = roe3yAvg != null ? roe3yAvg : roeActual;
            String profitabilityStatus;
            if (profitabilityRoe == null) {
                profitabilityStatus = "数据不足";
            } else if (profitabilityRoe.compareTo(new BigDecimal("15")) >= 0) {
                profitabilityStatus = "盈利优秀";
            } else if (profitabilityRoe.compareTo(new BigDecimal("8")) >= 0) {
                profitabilityStatus = "盈利良好";
            } else if (profitabilityRoe.compareTo(BigDecimal.ZERO) > 0) {
                profitabilityStatus = "盈利偏弱";
            } else {
                profitabilityStatus = "盈利承压";
            }

            // 分红质量评分：股息率30 + 连续性25 + 三年增长20 + 派息安全15 + 盈利稳定10。
            double yieldScore = 0;
            double dyVal = latestDividendYield != null ? latestDividendYield.doubleValue() : 0;
            if (dyVal >= 5.0) yieldScore = 30;
            else if (dyVal >= 3.0) yieldScore = 20 + (dyVal - 3.0) / 2.0 * 10;
            else if (dyVal >= 1.5) yieldScore = 10 + (dyVal - 1.5) / 1.5 * 10;
            else if (dyVal > 0) yieldScore = dyVal / 1.5 * 10;

            double consScore = 0;
            if (consecutiveYears >= 10) consScore = 25;
            else if (consecutiveYears >= 5) consScore = 18 + (consecutiveYears - 5) / 5.0 * 7;
            else if (consecutiveYears >= 3) consScore = 12 + (consecutiveYears - 3) / 2.0 * 6;
            else if (consecutiveYears >= 1) consScore = 4 + (consecutiveYears - 1) * 4;

            double growthScore = 0;
            if (dividendGrowth3y != null) {
                double gVal = dividendGrowth3y.doubleValue();
                if (gVal >= 10.0) growthScore = 20;
                else if (gVal >= 5.0) growthScore = 15 + (gVal - 5.0);
                else if (gVal >= 0.0) growthScore = 10 + gVal;
                else if (gVal > -10.0) growthScore = 10 + gVal;
            }

            double payoutScore = 0;
            if (payoutRatio != null) {
                double payout = payoutRatio.doubleValue();
                if (payout >= 20 && payout <= 60) payoutScore = 15;
                else if (payout > 0 && payout <= 80) payoutScore = 12;
                else if (payout > 0 && payout <= 100) payoutScore = 7;
                else if (payout > 100 && payout <= 150) payoutScore = 3;
            }

            double profitScore = 0;
            if (profitabilityRoe != null) {
                double roe = profitabilityRoe.doubleValue();
                if (roe >= 15) profitScore = 10;
                else if (roe >= 8) profitScore = 5 + (roe - 8) / 7 * 5;
                else if (roe > 0) profitScore = roe / 8 * 5;
            }

            Integer totalScore = latestYearDividend.compareTo(BigDecimal.ZERO) > 0 && latestDividendYield != null
                    ? Math.min(100, Math.max(0, (int) Math.round(yieldScore + consScore + growthScore + payoutScore + profitScore)))
                    : null;

            String dividendLevel;
            String conclusion;
            if (totalScore == null) {
                dividendLevel = "数据不足";
                conclusion = "缺少有效年度分红或价格数据，暂不进行分红质量评分。";
            } else if (totalScore >= 80) {
                dividendLevel = "优质分红";
                conclusion = "股息回报、分红连续性和可持续性表现较好。";
            } else if (totalScore >= 65) {
                dividendLevel = "稳健分红";
                conclusion = "分红记录较稳定，股息回报和派息能力处于较好水平。";
            } else if (totalScore >= 50) {
                dividendLevel = "分红观察";
                conclusion = "具备一定分红能力，但连续性、增长或派息安全性仍需观察。";
            } else {
                dividendLevel = "较弱分红";
                conclusion = "当前分红回报或可持续性偏弱，不宜仅依据股息率判断。";
            }

            // 最近 4 年快照列表
            List<AnnualDividendSnapshotVO> annualSnapshots = new ArrayList<>();
            for (int y = maxYear - 3; y <= maxYear; y++) {
                BigDecimal cashRatio = annualCashDivMap.getOrDefault(y, BigDecimal.ZERO);
                BigDecimal dps = cashRatio.divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);

                BigDecimal yYield = annualYieldMap.get(y);
                if (yYield == null && latestPrice != null && latestPrice.compareTo(BigDecimal.ZERO) > 0 && dps.compareTo(BigDecimal.ZERO) > 0) {
                    yYield = dps.divide(latestPrice, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                }
                if (yYield != null) {
                    yYield = yYield.setScale(2, RoundingMode.HALF_UP);
                }

                BigDecimal eps = annualEpsMap.get(y);
                BigDecimal payout = null;
                if (eps != null && eps.compareTo(BigDecimal.ZERO) > 0 && dps.compareTo(BigDecimal.ZERO) > 0) {
                    payout = dps.divide(eps, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP);
                }

                String label = String.valueOf(y);
                if (y == maxYear) {
                    label += " (最新)";
                }

                annualSnapshots.add(AnnualDividendSnapshotVO.builder()
                        .year(y)
                        .yearLabel(label)
                        .dividendPerShare(dps)
                        .dividendYield(yYield)
                        .payoutRatio(payout)
                        .build());
            }

            LocalDate latestAnnouncementDate = dividends.get(0).getLatestAnnouncementDate();

            StockDividendStatVO vo = new StockDividendStatVO();
            vo.setStockCode(stockCode);
            vo.setStockName(stockName);
            vo.setIndustry(industry != null ? industry : "未归类");
            vo.setLatestPrice(latestPrice);
            vo.setAvgDividend(avgDividend);
            vo.setLatestYearDividend(latestYearDividend);
            vo.setDividendYield(latestDividendYield);
            vo.setPeg(peg);
            vo.setDividendScore(totalScore == null ? null : BigDecimal.valueOf(totalScore));
            vo.setDividendLevel(dividendLevel);
            vo.setConclusion(conclusion);
            vo.setConsecutiveYears(consecutiveYears);
            vo.setDividendGrowth3y(dividendGrowth3y);
            vo.setPayoutRatio(payoutRatio);
            vo.setProfitabilityStatus(profitabilityStatus);
            vo.setPe(pe);
            vo.setPeIndustryAvg(peIndustryAvg);
            vo.setRoeActual(roeActual);
            vo.setRoe3yAvg(roe3yAvg);
            vo.setRoeIndustryAvg(roeIndustryAvg);
            vo.setLatestYearTransfer(latestYearTransfer);
            vo.setLatestAnnouncementDate(latestAnnouncementDate);
            vo.setAnnualSnapshots(annualSnapshots);

            result.add(vo);
        }

        // 计算全行业股息率均值并回填
        Map<String, List<StockDividendStatVO>> byIndustry = result.stream()
                .filter(r -> StringUtils.isNotBlank(r.getIndustry()))
                .collect(Collectors.groupingBy(StockDividendStatVO::getIndustry));

        Map<String, BigDecimal> industryYieldAvgMap = new HashMap<>();
        for (Map.Entry<String, List<StockDividendStatVO>> indEntry : byIndustry.entrySet()) {
            List<BigDecimal> yields = indEntry.getValue().stream()
                    .map(StockDividendStatVO::getDividendYield)
                    .filter(Objects::nonNull)
                    .toList();
            if (!yields.isEmpty()) {
                BigDecimal sum = yields.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = sum.divide(BigDecimal.valueOf(yields.size()), 2, RoundingMode.HALF_UP);
                industryYieldAvgMap.put(indEntry.getKey(), avg);
            }
        }

        for (StockDividendStatVO vo : result) {
            if (vo.getIndustry() != null) {
                vo.setIndustryDividendYieldAvg(industryYieldAvgMap.getOrDefault(vo.getIndustry(), new BigDecimal("1.41")));
            }
        }

        return result;
    }

    private Comparator<StockDividendStatVO> buildComparator(Sort sort, String quickTab) {
        if (sort != null && sort.isSorted()) {
            Comparator<StockDividendStatVO> result = null;

            for (Sort.Order order : sort) {
                Comparator<StockDividendStatVO> comparator;
                Comparator<BigDecimal> valueComparator = order.getDirection() == Sort.Direction.DESC
                        ? Comparator.nullsLast(Comparator.reverseOrder())
                        : Comparator.nullsLast(Comparator.naturalOrder());

                switch (order.getProperty()) {
                    case "dividendScore":
                        comparator = Comparator.comparing(
                                StockDividendStatVO::getDividendScore,
                                valueComparator);
                        break;

                    case "dividendYield":
                        comparator = Comparator.comparing(
                                StockDividendStatVO::getDividendYield,
                                valueComparator);
                        break;

                    case "latestYearDividend":
                        comparator = Comparator.comparing(
                                StockDividendStatVO::getLatestYearDividend,
                                valueComparator);
                        break;

                    case "avgDividend":
                        comparator = Comparator.comparing(
                                StockDividendStatVO::getAvgDividend,
                                valueComparator);
                        break;

                    case "peg":
                        comparator = Comparator.comparing(
                                StockDividendStatVO::getPeg,
                                valueComparator);
                        break;

                    case "latestPrice":
                        comparator = Comparator.comparing(
                                StockDividendStatVO::getLatestPrice,
                                valueComparator);
                        break;

                    case "dividendGrowth3y":
                        comparator = Comparator.comparing(
                                StockDividendStatVO::getDividendGrowth3y,
                                valueComparator);
                        break;

                    default:
                        continue;
                }

                result = (result == null) ? comparator : result.thenComparing(comparator);
            }

            if (result != null) {
                return result.thenComparing(StockDividendStatVO::getStockCode,
                        Comparator.nullsLast(Comparator.naturalOrder()));
            }
        }

        // 默认按快捷 Tab 排序
        if ("STABLE_DIVIDEND".equalsIgnoreCase(quickTab)) {
            return Comparator.comparing(StockDividendStatVO::getDividendScore,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(StockDividendStatVO::getStockCode, Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("DIVIDEND_GROWTH".equalsIgnoreCase(quickTab)) {
            return Comparator.comparing(StockDividendStatVO::getDividendGrowth3y,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(StockDividendStatVO::getStockCode, Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("HIGH_DIVIDEND".equalsIgnoreCase(quickTab)) {
            return Comparator.comparing(StockDividendStatVO::getDividendYield,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(StockDividendStatVO::getStockCode, Comparator.nullsLast(Comparator.naturalOrder()));
        }

        // 默认按分红评分或股息率倒序
        return Comparator.comparing(StockDividendStatVO::getDividendScore,
                Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(StockDividendStatVO::getStockCode, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    public List<StockDividendDetailVO> getDetailByCode(StockDividendDetailReqVO reqVO) {
        List<StockDividend> list = stockDividendRepository
                .findByStockCodeOrderByLatestAnnouncementDateDesc(reqVO.getStockCode());
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        int currentYear = LocalDate.now().getYear();
        List<StockDividend> targets = list.stream()
                .filter(d -> d.getLatestAnnouncementDate() != null
                        && d.getLatestAnnouncementDate().getYear() != currentYear
                        && (d.getRecordDate() == null || d.getExDividendDate() == null))
                .toList();

        if (!targets.isEmpty()) {
            try {
                String stockCode = reqVO.getStockCode();
                String cleanSymbol = stockCode.length() > 6 ? stockCode.substring(2) : stockCode;
                Set<StockDividend> updatedSet = new HashSet<>();

                // 1. 尝试从 stockFhpsDetailEm 补充
                try {
                    List<StockFhpsDetailEm> detailEms = akShareDividendService.stockFhpsDetailEm(cleanSymbol);
                    if (!CollectionUtils.isEmpty(detailEms)) {
                        for (StockDividend target : targets) {
                            StockFhpsDetailEm match = findMatchingDetailEm(target, detailEms);
                            if (match != null) {
                                LocalDate newRecordDate = DateUtils.parseLocalDate(match.getRecordDate());
                                LocalDate newExDividendDate = DateUtils.parseLocalDate(match.getExDividendDate());
                                boolean updated = false;

                                if (newRecordDate != null && target.getRecordDate() == null) {
                                    target.setRecordDate(newRecordDate);
                                    updated = true;
                                }
                                if (newExDividendDate != null && target.getExDividendDate() == null) {
                                    target.setExDividendDate(newExDividendDate);
                                    updated = true;
                                }
                                if (StringUtils.isNotBlank(match.getPlanStatus()) && !Objects.equals(target.getPlanStatus(), match.getPlanStatus())) {
                                    target.setPlanStatus(match.getPlanStatus());
                                    updated = true;
                                }

                                if (updated) {
                                    updatedSet.add(target);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("从 stockFhpsDetailEm 补充分红数据失败, stockCode={}", reqVO.getStockCode(), e);
                }

                // 2. 如果依然存在股权登记日或除权除息日为空的记录，从 stockFhpsDetailThs 进一步补充
                List<StockDividend> thsTargets = targets.stream()
                        .filter(d -> d.getRecordDate() == null || d.getExDividendDate() == null)
                        .toList();

                if (!thsTargets.isEmpty()) {
                    try {
                        List<StockFhpsDetailThs> detailThsList = akShareDividendService.stockFhpsDetailThs(cleanSymbol);
                        if (!CollectionUtils.isEmpty(detailThsList)) {
                            for (StockDividend target : thsTargets) {
                                StockFhpsDetailThs match = findMatchingDetailThs(target, detailThsList);
                                if (match != null) {
                                    LocalDate newRecordDate = DateUtils.parseLocalDate(match.getAShareRecordDate());
                                    LocalDate newExDividendDate = DateUtils.parseLocalDate(match.getAShareExDividendDate());
                                    boolean updated = false;

                                    if (newRecordDate != null && target.getRecordDate() == null) {
                                        target.setRecordDate(newRecordDate);
                                        updated = true;
                                    }
                                    if (newExDividendDate != null && target.getExDividendDate() == null) {
                                        target.setExDividendDate(newExDividendDate);
                                        updated = true;
                                    }
                                    String thsStatus = convertThsPlanStatus(match.getPlanStatus());
                                    if (StringUtils.isNotBlank(thsStatus) && !Objects.equals(target.getPlanStatus(), thsStatus)) {
                                        target.setPlanStatus(thsStatus);
                                        updated = true;
                                    }

                                    if (updated) {
                                        updatedSet.add(target);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("从 stockFhpsDetailThs 补充分红数据失败, stockCode={}", reqVO.getStockCode(), e);
                    }
                }

                if (!updatedSet.isEmpty()) {
                    stockDividendRepository.saveAll(updatedSet);
                    log.info("结合 EM 与 THS 成功补全股票 {} 的股权登记日/除权除息日/方案进度, 共更新了 {} 条记录",
                            reqVO.getStockCode(), updatedSet.size());
                }
            } catch (Exception e) {
                log.error("尝试补全股票 {} 的股权登记日/除权除息日失败", reqVO.getStockCode(), e);
            }
        }

        return list.stream()
                .sorted(
                        Comparator.comparing(StockDividend::getLatestAnnouncementDate,
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(StockDividend::getReportDate,
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .map(o -> {
                    StockDividendDetailVO vo = new StockDividendDetailVO();
                    BeanUtils.copyProperties(o, vo);
                    return vo;
                })
                .toList();
    }

    private StockFhpsDetailEm findMatchingDetailEm(StockDividend target, List<StockFhpsDetailEm> detailEms) {
        for (StockFhpsDetailEm em : detailEms) {
            LocalDate emAnnouncementDate = DateUtils.parseLocalDate(em.getLatestAnnouncementDate());
            if (emAnnouncementDate == null) {
                emAnnouncementDate = DateUtils.parseLocalDate(em.getProposalAnnouncementDate());
            }
            if (target.getLatestAnnouncementDate() != null && target.getLatestAnnouncementDate().equals(emAnnouncementDate)) {
                return em;
            }
        }
        // 若按公告日没匹配上，尝试按报告期匹配
        if (target.getReportDate() != null) {
            for (StockFhpsDetailEm em : detailEms) {
                LocalDate emReportDate = DateUtils.parseLocalDate(em.getReportDate());
                if (emReportDate != null && target.getReportDate().startsWith(emReportDate.toString())) {
                    return em;
                }
            }
        }
        return null;
    }

    private StockFhpsDetailThs findMatchingDetailThs(StockDividend target, List<StockFhpsDetailThs> detailThsList) {
        if (target.getLatestAnnouncementDate() == null) {
            return null;
        }
        // 按照实施公告日进行精准匹配
        for (StockFhpsDetailThs ths : detailThsList) {
            LocalDate thsNoticeDate = DateUtils.parseLocalDate(ths.getImplementationNoticeDate());
            if (thsNoticeDate != null && target.getLatestAnnouncementDate().equals(thsNoticeDate)) {
                return ths;
            }
        }
        // 兜底：若实施公告日为空或未匹配成功，尝试按股东大会预案公告日 / 董事会日期匹配
        for (StockFhpsDetailThs ths : detailThsList) {
            LocalDate thsDate = DateUtils.parseLocalDate(ths.getShareholdersMeetingProposalDate());
            if (thsDate == null) {
                thsDate = DateUtils.parseLocalDate(ths.getBoardDate());
            }
            if (thsDate != null && target.getLatestAnnouncementDate().equals(thsDate)) {
                return ths;
            }
        }
        return null;
    }

    private String convertThsPlanStatus(String rawStatus) {
        if (StringUtils.isBlank(rawStatus)) {
            return rawStatus;
        }
        if ("实施方案".equals(rawStatus)) {
            return "实施分配";
        }
        if ("董事会预案".equals(rawStatus)) {
            return "董事会决议通过";
        }
        return rawStatus;
    }

}
