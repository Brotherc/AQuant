package com.brotherc.aquant.fund.service;

import com.brotherc.aquant.fund.entity.StockFundInfo;
import com.brotherc.aquant.fund.entity.StockFundPurchaseLimit;
import com.brotherc.aquant.integration.akshare.model.FundPurchaseEm;
import com.brotherc.aquant.integration.akshare.model.FundNameEm;
import com.brotherc.aquant.fund.model.vo.StockFundInfoPageReqVO;
import com.brotherc.aquant.fund.model.vo.StockFundInfoVO;
import com.brotherc.aquant.fund.repository.StockFundInfoRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockFundInfoService {

    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final String FUND_TYPE = "fundType";

    private final StockFundInfoRepository stockFundInfoRepository;
    private final StockFundPurchaseLimitService stockFundPurchaseLimitService;

    @Transactional(rollbackFor = Exception.class)
    public void saveFundInfos(List<FundNameEm> fundNameEms, List<FundPurchaseEm> fundPurchaseEms) {
        if (CollectionUtils.isEmpty(fundNameEms) && CollectionUtils.isEmpty(fundPurchaseEms)) {
            return;
        }

        List<StockFundInfo> allExisting = stockFundInfoRepository.findAll();
        Map<String, StockFundInfo> existingMap = allExisting.stream()
                .collect(Collectors.toMap(StockFundInfo::getFundCode, Function.identity(), (a, b) -> a));

        Map<String, FundPurchaseEm> purchaseMap = Map.of();
        if (!CollectionUtils.isEmpty(fundPurchaseEms)) {
            purchaseMap = fundPurchaseEms.stream().collect(
                    Collectors.toMap(FundPurchaseEm::getFundCode, Function.identity(), (a, b) -> a)
            );
        }

        List<StockFundInfo> toSave = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fundNameEms)) {
            for (FundNameEm em : fundNameEms) {
                StockFundInfo info = existingMap.get(em.getFundCode());
                if (info == null) {
                    info = new StockFundInfo();
                    info.setFundCode(em.getFundCode());
                }
                info.setPinyinAbbr(em.getPinyinAbbr());
                info.setFundName(em.getFundName());
                info.setFundType(em.getFundType());
                info.setPinyinFull(em.getPinyinFull());

                FundPurchaseEm purchase = purchaseMap.get(em.getFundCode());
                if (purchase != null) {
                    applyPurchaseInfo(info, purchase);
                }
                toSave.add(info);
                existingMap.put(info.getFundCode(), info);
            }
        }

        if (!CollectionUtils.isEmpty(fundPurchaseEms)) {
            for (FundPurchaseEm purchase : fundPurchaseEms) {
                if (existingMap.containsKey(purchase.getFundCode())) {
                    continue;
                }
                StockFundInfo info = new StockFundInfo();
                info.setFundCode(purchase.getFundCode());
                info.setFundName(purchase.getFundName());
                info.setFundType(purchase.getFundType());
                applyPurchaseInfo(info, purchase);
                toSave.add(info);
                existingMap.put(info.getFundCode(), info);
            }
        }

        stockFundInfoRepository.saveAll(toSave);
    }

    private void applyPurchaseInfo(StockFundInfo info, FundPurchaseEm purchase) {
        info.setPurchaseStartAmount(purchase.getPurchaseStartAmount());
        info.setDailyLimitAmount(purchase.getDailyLimitAmount());
        info.setFeeRate(purchase.getFeeRate());
        if (StringUtils.isBlank(purchase.getLatestNetValueReportTime())) {
            return;
        }

        try {
            LocalDate today = LocalDate.now();
            LocalDate reportDate = MonthDay.parse(
                    purchase.getLatestNetValueReportTime(), MONTH_DAY_FORMATTER
            ).atYear(today.getYear());
            if (reportDate.isAfter(today)) {
                reportDate = reportDate.minusYears(1);
            }
            info.setLatestNetValueReportDate(reportDate);
        } catch (DateTimeParseException e) {
            log.warn("基金最新净值报告日期格式不正确，fundCode={}, reportTime={}",
                    purchase.getFundCode(), purchase.getLatestNetValueReportTime());
        }
    }

    public Page<StockFundInfoVO> getPage(StockFundInfoPageReqVO reqVO, Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isSorted() && sort.getOrderFor("dailyLimitAmount") != null && sort.getOrderFor("fundName") == null) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    sort.and(Sort.by(Sort.Direction.ASC, "fundName"))
            );
        }

        Specification<StockFundInfo> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(reqVO.getFundCode())) {
                predicates.add(cb.like(root.get("fundCode"), "%" + reqVO.getFundCode() + "%"));
            }

            if (StringUtils.isNotBlank(reqVO.getFundName())) {
                predicates.add(cb.like(root.get("fundName"), "%" + reqVO.getFundName() + "%"));
            }

            if (StringUtils.isNotBlank(reqVO.getFundType())) {
                predicates.add(cb.equal(root.get(FUND_TYPE), reqVO.getFundType()));
            }

            if (Boolean.TRUE.equals(reqVO.getIncludeUsStock())) {
                List<Predicate> usStockPredicates = new ArrayList<>();
                String[] keywords = {"QDII", "纳斯达克", "标普", "美国", "全球", "海外", "美元"};
                for (String keyword : keywords) {
                    usStockPredicates.add(cb.like(root.get("fundName"), "%" + keyword + "%"));
                }
                usStockPredicates.add(cb.like(root.get(FUND_TYPE), "QDII%"));
                usStockPredicates.add(cb.equal(root.get(FUND_TYPE), "指数型-海外股票"));
                
                predicates.add(cb.or(usStockPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<StockFundInfo> entityPage = stockFundInfoRepository.findAll(specification, pageable);
        Map<String, StockFundPurchaseLimit> purchaseLimitMap =
                stockFundPurchaseLimitService.getPurchaseSummaries(
                        entityPage.getContent().stream().map(StockFundInfo::getFundCode).toList()
                );
        return entityPage.map(o -> {
            StockFundInfoVO vo = new StockFundInfoVO();
            BeanUtils.copyProperties(o, vo);
            StockFundPurchaseLimit purchaseLimit = purchaseLimitMap.get(o.getFundCode());
            if (purchaseLimit != null) {
                vo.setOfficialPurchaseSource(purchaseLimit.getSource());
                vo.setOfficialPurchaseSourceName(purchaseLimit.getSourceName());
                vo.setOfficialPurchaseStatus(purchaseLimit.getStatus());
                vo.setOfficialPurchaseLimitAmount(purchaseLimit.getLimitAmount());
                vo.setOfficialPurchaseEffectiveDate(purchaseLimit.getEffectiveDate());
            }
            return vo;
        });
    }

}
