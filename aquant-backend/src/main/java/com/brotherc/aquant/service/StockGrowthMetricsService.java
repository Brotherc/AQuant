package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockGrowthMetrics;
import com.brotherc.aquant.model.dto.akshare.StockZhGrowthComparisonEm;
import com.brotherc.aquant.model.vo.stockindicator.GrowthMetricsPageReqVO;
import com.brotherc.aquant.repository.StockGrowthMetricsRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockGrowthMetricsService {

    private final StockGrowthMetricsRepository stockGrowthMetricsRepository;

    public Page<StockGrowthMetrics> pageQuery(GrowthMetricsPageReqVO reqVO, Pageable pageable) {
        Specification<StockGrowthMetrics> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // 股票代码等值查询
            if (StringUtils.isNotBlank(reqVO.getStockCode())) {
                predicates.add(cb.equal(root.get("stockCode"), reqVO.getStockCode()));
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
                predicates
                        .add(cb.greaterThanOrEqualTo(root.get("netProfitGrowthTtm"), reqVO.getNetProfitGrowthTtmMin()));
            }
            if (reqVO.getNetProfitGrowthTtmMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("netProfitGrowthTtm"), reqVO.getNetProfitGrowthTtmMax()));
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

    @Transactional(rollbackFor = Exception.class)
    public void save(String code, String name, List<StockZhGrowthComparisonEm> list) {
        StockGrowthMetrics stockGrowthMetrics = stockGrowthMetricsRepository.findByStockCode(code);

        if (stockGrowthMetrics == null) {
            stockGrowthMetrics = new StockGrowthMetrics();
        }
        stockGrowthMetrics.setStockCode(code);
        stockGrowthMetrics.setStockName(name);

        code = code.substring(2);

        for (StockZhGrowthComparisonEm data : list) {
            String c = data.get代码();
            if (code.equals(c)) {
                stockGrowthMetrics.setEpsGrowth3yCagr(data.get基本每股收益增长率_3年复合());
                stockGrowthMetrics.setEpsGrowthLastYA(data.get基本每股收益增长率_24A());
                stockGrowthMetrics.setEpsGrowthTtm(data.get基本每股收益增长率_TTM());
                stockGrowthMetrics.setEpsGrowthThisYE(data.get基本每股收益增长率_25E());
                stockGrowthMetrics.setEpsGrowthNextYE(data.get基本每股收益增长率_26E());
                stockGrowthMetrics.setEpsGrowthNext2YE(data.get基本每股收益增长率_27E());
                stockGrowthMetrics.setEpsGrowth3yCagrRank(data.get基本每股收益增长率_3年复合排名());

                stockGrowthMetrics.setRevenueGrowth3yCagr(data.get营业收入增长率_3年复合());
                stockGrowthMetrics.setRevenueGrowthLastYA(data.get营业收入增长率_24A());
                stockGrowthMetrics.setRevenueGrowthTtm(data.get营业收入增长率_TTM());
                stockGrowthMetrics.setRevenueGrowthThisYE(data.get营业收入增长率_25E());
                stockGrowthMetrics.setRevenueGrowthNextYE(data.get营业收入增长率_26E());
                stockGrowthMetrics.setRevenueGrowthNext2YE(data.get营业收入增长率_27E());

                stockGrowthMetrics.setNetProfitGrowth3yCagr(data.get净利润增长率_3年复合());
                stockGrowthMetrics.setNetProfitGrowthLastYA(data.get净利润增长率_24A());
                stockGrowthMetrics.setNetProfitGrowthTtm(data.get净利润增长率_TTM());
                stockGrowthMetrics.setNetProfitGrowthThisYE(data.get净利润增长率_25E());
                stockGrowthMetrics.setNetProfitGrowthNextYE(data.get净利润增长率_26E());
                stockGrowthMetrics.setNetProfitGrowthNext2YE(data.get净利润增长率_27E());
                stockGrowthMetrics.setCreatedAt(LocalDateTime.now());
            } else if ("行业中值".equals(c)) {
                stockGrowthMetrics.setEpsGrowth3yCagrIndustryMed(data.get基本每股收益增长率_3年复合());
                stockGrowthMetrics.setEpsGrowthLastYAIndustryMed(data.get基本每股收益增长率_24A());
                stockGrowthMetrics.setEpsGrowthTtmIndustryMed(data.get基本每股收益增长率_TTM());
                stockGrowthMetrics.setEpsGrowthThisYEIndustryMed(data.get基本每股收益增长率_25E());
                stockGrowthMetrics.setEpsGrowthNextYEIndustryMed(data.get基本每股收益增长率_26E());
                stockGrowthMetrics.setEpsGrowthNext2YEIndustryMed(data.get基本每股收益增长率_27E());
                stockGrowthMetrics.setEpsGrowth3yCagrRankIndustryMed(data.get基本每股收益增长率_3年复合排名());

                stockGrowthMetrics.setRevenueGrowth3yCagrIndustryMed(data.get营业收入增长率_3年复合());
                stockGrowthMetrics.setRevenueGrowthLastYAIndustryMed(data.get营业收入增长率_24A());
                stockGrowthMetrics.setRevenueGrowthTtmIndustryMed(data.get营业收入增长率_TTM());
                stockGrowthMetrics.setRevenueGrowthThisYEIndustryMed(data.get营业收入增长率_25E());
                stockGrowthMetrics.setRevenueGrowthNextYEIndustryMed(data.get营业收入增长率_26E());
                stockGrowthMetrics.setRevenueGrowthNext2YEIndustryMed(data.get营业收入增长率_27E());

                stockGrowthMetrics.setNetProfitGrowth3yCagrIndustryMed(data.get净利润增长率_3年复合());
                stockGrowthMetrics.setNetProfitGrowthLastYAIndustryMed(data.get净利润增长率_24A());
                stockGrowthMetrics.setNetProfitGrowthTtmIndustryMed(data.get净利润增长率_TTM());
                stockGrowthMetrics.setNetProfitGrowthThisYEIndustryMed(data.get净利润增长率_25E());
                stockGrowthMetrics.setNetProfitGrowthNextYEIndustryMed(data.get净利润增长率_26E());
                stockGrowthMetrics.setNetProfitGrowthNext2YEIndustryMed(data.get净利润增长率_27E());
                stockGrowthMetrics.setCreatedAt(LocalDateTime.now());
            } else if ("行业平均".equals(c)) {
                stockGrowthMetrics.setEpsGrowth3yCagrIndustryAvg(data.get基本每股收益增长率_3年复合());
                stockGrowthMetrics.setEpsGrowthLastYAIndustryAvg(data.get基本每股收益增长率_24A());
                stockGrowthMetrics.setEpsGrowthTtmIndustryAvg(data.get基本每股收益增长率_TTM());
                stockGrowthMetrics.setEpsGrowthThisYEIndustryAvg(data.get基本每股收益增长率_25E());
                stockGrowthMetrics.setEpsGrowthNextYEIndustryAvg(data.get基本每股收益增长率_26E());
                stockGrowthMetrics.setEpsGrowthNext2YEIndustryAvg(data.get基本每股收益增长率_27E());
                stockGrowthMetrics.setEpsGrowth3yCagrRankIndustryAvg(data.get基本每股收益增长率_3年复合排名());

                stockGrowthMetrics.setRevenueGrowth3yCagrIndustryAvg(data.get营业收入增长率_3年复合());
                stockGrowthMetrics.setRevenueGrowthLastYAIndustryAvg(data.get营业收入增长率_24A());
                stockGrowthMetrics.setRevenueGrowthTtmIndustryAvg(data.get营业收入增长率_TTM());
                stockGrowthMetrics.setRevenueGrowthThisYEIndustryAvg(data.get营业收入增长率_25E());
                stockGrowthMetrics.setRevenueGrowthNextYEIndustryAvg(data.get营业收入增长率_26E());
                stockGrowthMetrics.setRevenueGrowthNext2YEIndustryAvg(data.get营业收入增长率_27E());

                stockGrowthMetrics.setNetProfitGrowth3yCagrIndustryAvg(data.get净利润增长率_3年复合());
                stockGrowthMetrics.setNetProfitGrowthLastYAIndustryAvg(data.get净利润增长率_24A());
                stockGrowthMetrics.setNetProfitGrowthTtmIndustryAvg(data.get净利润增长率_TTM());
                stockGrowthMetrics.setNetProfitGrowthThisYEIndustryAvg(data.get净利润增长率_25E());
                stockGrowthMetrics.setNetProfitGrowthNextYEIndustryAvg(data.get净利润增长率_26E());
                stockGrowthMetrics.setNetProfitGrowthNext2YEIndustryAvg(data.get净利润增长率_27E());
                stockGrowthMetrics.setCreatedAt(LocalDateTime.now());
            }
        }
        stockGrowthMetricsRepository.save(stockGrowthMetrics);
    }

}
