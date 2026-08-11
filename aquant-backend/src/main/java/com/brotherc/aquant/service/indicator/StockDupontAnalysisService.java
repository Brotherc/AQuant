package com.brotherc.aquant.service.indicator;

import com.brotherc.aquant.entity.indicator.StockDupontAnalysis;
import com.brotherc.aquant.model.dto.akshare.StockZhDupontComparisonEm;
import com.brotherc.aquant.model.vo.stockindicator.DupontAnalysisPageReqVO;
import com.brotherc.aquant.repository.indicator.StockDupontAnalysisRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockDupontAnalysisService {

    private static final String ROE_3Y_AVG_INDUSTRY_AVG = "roe3yAvgIndustryAvg";
    private static final String ROE_3Y_AVG = "roe3yAvg";

    private final StockDupontAnalysisRepository stockDupontAnalysisRepository;

    public Page<StockDupontAnalysis> pageQuery(DupontAnalysisPageReqVO query, Pageable pageable) {
        Specification<StockDupontAnalysis> specification =(root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 等值查询 stockCode
            if (StringUtils.isNotBlank(query.getStockCode())) {
                predicates.add(cb.equal(root.get("stockCode"), query.getStockCode()));
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return stockDupontAnalysisRepository.findAll(specification, pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(String code, String name, List<StockZhDupontComparisonEm> list) {
        StockDupontAnalysis stockDupontAnalysis = stockDupontAnalysisRepository.findByStockCode(code);

        if (stockDupontAnalysis == null) {
            stockDupontAnalysis = new StockDupontAnalysis();
        }
        stockDupontAnalysis.setStockCode(code);
        stockDupontAnalysis.setStockName(name);

        code = code.substring(2);

        for (StockZhDupontComparisonEm data : list) {
            String c = data.getCode();
            if (code.equals(c)) {
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
