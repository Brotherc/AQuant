package com.brotherc.aquant.service.dividend;

import com.brotherc.aquant.entity.dividend.StockDividend;
import com.brotherc.aquant.entity.stock.StockQuote;
import com.brotherc.aquant.model.dto.akshare.StockFhpsDetailEm;
import com.brotherc.aquant.model.dto.akshare.StockFhpsDetailThs;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendDetailReqVO;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendDetailVO;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendStatPageReqVO;
import com.brotherc.aquant.model.vo.stockdividend.StockDividendStatVO;
import com.brotherc.aquant.repository.dividend.StockDividendRepository;
import com.brotherc.aquant.repository.stock.StockQuoteRepository;
import com.brotherc.aquant.repository.indicator.StockValuationMetricsRepository;
import com.brotherc.aquant.repository.indicator.StockDupontAnalysisRepository;
import com.brotherc.aquant.entity.indicator.StockValuationMetrics;
import com.brotherc.aquant.entity.indicator.StockDupontAnalysis;
import com.brotherc.aquant.model.dto.stockquote.StockDividendProjection;
import com.brotherc.aquant.repository.watchlist.StockWatchlistStockRepository;
import com.brotherc.aquant.entity.watchlist.StockWatchlistStock;
import com.brotherc.aquant.service.akshare.AKShareDividendService;
import com.brotherc.aquant.utils.DateUtils;
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

    public Page<StockDividendStatVO> pageDividendStats(StockDividendStatPageReqVO reqVO, Pageable pageable) {
        List<StockDividendStatVO> all = calcDividendStats(reqVO.getRecentYears(), reqVO.getMinAvgDividend(),
                reqVO.getStockCode(), reqVO.getStockName(), reqVO.getWatchlistGroupId());

        Map<String, StockQuote> stockQuoteMap = stockQuoteRepository.findAll()
                .stream().collect(Collectors.toMap(o -> o.getCode().substring(2), o -> o));

        Map<String, StockValuationMetrics> valuationMap = stockValuationMetricsRepository.findAll()
                .stream().collect(Collectors.toMap(o -> o.getStockCode().substring(2), o -> o));

        Map<String, StockDupontAnalysis> dupontMap = stockDupontAnalysisRepository.findAll()
                .stream().collect(Collectors.toMap(o -> o.getStockCode().substring(2), o -> o));

        for (StockDividendStatVO item : all) {
            StockQuote stockQuote = stockQuoteMap.get(item.getStockCode());
            if (stockQuote != null) {
                item.setLatestPrice(stockQuote.getLatestPrice());
            }

            StockValuationMetrics valuation = valuationMap.get(item.getStockCode());
            if (valuation != null) {
                item.setPeg(valuation.getPeg());
                item.setPe(valuation.getPeTtm());
                item.setPeIndustryAvg(valuation.getPeTtmIndustryAvg());
            }

            StockDupontAnalysis dupont = dupontMap.get(item.getStockCode());
            if (dupont != null) {
                item.setRoeActual(dupont.getRoeLastYA());
                item.setRoe3yAvg(dupont.getRoe3yAvg());
            }
        }

        // PEG 范围筛选
        if (StringUtils.isNotBlank(reqVO.getPegRange())) {
            all = all.stream().filter(item -> {
                BigDecimal peg = item.getPeg();
                if (peg == null)
                    return false;
                if ("1".equals(reqVO.getPegRange())) {
                    return peg.compareTo(BigDecimal.ZERO) > 0 && peg.compareTo(new BigDecimal("0.5")) < 0;
                } else if ("2".equals(reqVO.getPegRange())) {
                    return peg.compareTo(new BigDecimal("0.5")) >= 0 && peg.compareTo(BigDecimal.ONE) < 0;
                }
                return true;
            }).collect(Collectors.toList());
        }

        // 排序（按最近一年分红倒序）
        all.sort(buildComparator(pageable.getSort()));

        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());

        List<StockDividendStatVO> content = start >= all.size() ? Collections.emptyList() : all.subList(start, end);

        return new PageImpl<>(content, pageable, all.size());
    }

    public List<StockDividendStatVO> calcDividendStats(
            Integer recentYears, BigDecimal minAvgDividend, String stockCodeQuery, String stockNameQuery,
            Long watchlistGroupId) {

        Set<String> watchlistCodes = null;
        if (watchlistGroupId != null) {
            watchlistCodes = stockWatchlistStockRepository.findByGroupIdOrderBySortNoDesc(watchlistGroupId)
                    .stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toSet());
            if (watchlistCodes.isEmpty()) {
                return new ArrayList<>();
            }
        }

        List<StockDividendProjection> list;
        if (recentYears != null) {
            LocalDate fromDate = LocalDate.now().minusYears(recentYears).withDayOfYear(1);
            list = stockDividendRepository.findByLatestAnnouncementDateGreaterThanEqualProjectedBy(fromDate);
        } else {
            list = stockDividendRepository.findAllProjectedBy();
        }

        // 按股票分组
        Map<String, List<StockDividendProjection>> group = list.stream()
                .collect(Collectors.groupingBy(StockDividendProjection::getStockCode));

        List<StockDividendStatVO> result = new ArrayList<>();

        for (Map.Entry<String, List<StockDividendProjection>> entry : group.entrySet()) {
            String stockCode = entry.getKey();
            List<StockDividendProjection> dividends = entry.getValue();
            String stockName = dividends.get(0).getStockName();

            boolean matchCode = StringUtils.isBlank(stockCodeQuery) || stockCode.contains(stockCodeQuery);
            boolean matchName = StringUtils.isBlank(stockNameQuery) || stockName.contains(stockNameQuery);
            boolean matchWatchlist = watchlistCodes == null || watchlistCodes.contains(stockCode);

            if (matchCode && matchName && matchWatchlist) {
                int currentYear = LocalDate.now().getYear();
                int minYear = dividends.stream()
                        .map(d -> d.getLatestAnnouncementDate().getYear())
                        .min(Integer::compareTo)
                        .orElse(currentYear);
                int years = currentYear - minYear + 1;

                // 最近 N 年平均分红
                BigDecimal avg = dividends.stream()
                        .map(StockDividendProjection::getCashDividendRatio)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(
                                BigDecimal.valueOf(years),
                                4,
                                RoundingMode.HALF_UP);

                if (minAvgDividend == null || avg.compareTo(minAvgDividend) >= 0) {
                    // 最近一年分红（按公告日最大年）
                    int latestYear = dividends.stream()
                            .map(d -> d.getLatestAnnouncementDate().getYear())
                            .max(Integer::compareTo)
                            .orElse(0);

                    BigDecimal latestYearDividend = dividends.stream()
                            .filter(d -> d.getLatestAnnouncementDate().getYear() == latestYear)
                            .map(StockDividendProjection::getCashDividendRatio)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal latestYearTransfer = dividends.stream()
                            .filter(d -> d.getLatestAnnouncementDate().getYear() == latestYear)
                            .map(d -> {
                                BigDecimal bonus = d.getBonusShareRatio() != null ? d.getBonusShareRatio() : BigDecimal.ZERO;
                                BigDecimal transfer = d.getTransferShareRatio() != null ? d.getTransferShareRatio()
                                        : BigDecimal.ZERO;
                                return bonus.add(transfer);
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    StockDividendStatVO stockDividendStatVO = new StockDividendStatVO();
                    stockDividendStatVO.setStockCode(stockCode);
                    stockDividendStatVO.setStockName(stockName);
                    stockDividendStatVO.setAvgDividend(avg);
                    stockDividendStatVO.setLatestYearDividend(latestYearDividend);
                    stockDividendStatVO.setLatestYearTransfer(latestYearTransfer);
                    result.add(stockDividendStatVO);
                }
            }
        }

        return result;
    }

    private Comparator<StockDividendStatVO> buildComparator(Sort sort) {
        Comparator<StockDividendStatVO> result = null;

        for (Sort.Order order : sort) {
            Comparator<StockDividendStatVO> comparator;

            switch (order.getProperty()) {
                case "latestYearDividend":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getLatestYearDividend,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                case "latestYearTransfer":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getLatestYearTransfer,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                case "avgDividend":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getAvgDividend,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                case "latestPrice":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getLatestPrice,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                case "peg":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getPeg,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                case "pe":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getPe,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                case "roe":
                    comparator = Comparator.comparing(
                            StockDividendStatVO::getRoeActual,
                            Comparator.nullsLast(BigDecimal::compareTo));
                    break;

                default:
                    // 不认识的排序字段，直接跳过
                    continue;
            }

            if (order.getDirection() == Sort.Direction.DESC) {
                comparator = comparator.reversed();
            }

            result = (result == null) ? comparator : result.thenComparing(comparator);
        }

        // 默认排序（兜底）
        return result != null ? result
                : Comparator.comparing(
                StockDividendStatVO::getLatestYearDividend,
                Comparator.nullsLast(BigDecimal::compareTo)).reversed();
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
