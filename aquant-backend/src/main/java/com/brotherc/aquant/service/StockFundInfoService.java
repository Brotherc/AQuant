package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockFundInfo;
import com.brotherc.aquant.model.dto.akshare.FundPurchaseEm;
import com.brotherc.aquant.model.dto.akshare.FundNameEm;
import com.brotherc.aquant.model.vo.stockfund.StockFundInfoPageReqVO;
import com.brotherc.aquant.model.vo.stockfund.StockFundInfoVO;
import com.brotherc.aquant.repository.StockFundInfoRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockFundInfoService {

    private final StockFundInfoRepository stockFundInfoRepository;

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
                    info.setPurchaseStartAmount(purchase.getPurchaseStartAmount());
                    info.setDailyLimitAmount(purchase.getDailyLimitAmount());
                    info.setFeeRate(purchase.getFeeRate());
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
                info.setPurchaseStartAmount(purchase.getPurchaseStartAmount());
                info.setDailyLimitAmount(purchase.getDailyLimitAmount());
                info.setFeeRate(purchase.getFeeRate());
                toSave.add(info);
                existingMap.put(info.getFundCode(), info);
            }
        }

        stockFundInfoRepository.saveAll(toSave);
    }

    public Page<StockFundInfoVO> getPage(StockFundInfoPageReqVO reqVO, Pageable pageable) {
        Specification<StockFundInfo> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(reqVO.getFundCode())) {
                predicates.add(cb.like(root.get("fundCode"), "%" + reqVO.getFundCode() + "%"));
            }

            if (StringUtils.isNotBlank(reqVO.getFundName())) {
                predicates.add(cb.like(root.get("fundName"), "%" + reqVO.getFundName() + "%"));
            }

            if (StringUtils.isNotBlank(reqVO.getFundType())) {
                predicates.add(cb.equal(root.get("fundType"), reqVO.getFundType()));
            }

            if (Boolean.TRUE.equals(reqVO.getIncludeUsStock())) {
                List<Predicate> usStockPredicates = new ArrayList<>();
                String[] keywords = {"QDII", "纳斯达克", "标普", "美国", "全球", "海外", "美元"};
                for (String keyword : keywords) {
                    usStockPredicates.add(cb.like(root.get("fundName"), "%" + keyword + "%"));
                }
                usStockPredicates.add(cb.like(root.get("fundType"), "QDII%"));
                usStockPredicates.add(cb.equal(root.get("fundType"), "指数型-海外股票"));
                
                predicates.add(cb.or(usStockPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return stockFundInfoRepository.findAll(specification, pageable).map(o -> {
            StockFundInfoVO vo = new StockFundInfoVO();
            BeanUtils.copyProperties(o, vo);
            return vo;
        });
    }

}
