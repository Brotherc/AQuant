package com.brotherc.aquant.service.indicator;

import com.brotherc.aquant.entity.indicator.StockGrowthMetrics;
import com.brotherc.aquant.model.dto.akshare.StockZhGrowthComparisonEm;
import com.brotherc.aquant.model.vo.stockindicator.GrowthMetricsPageReqVO;
import com.brotherc.aquant.repository.indicator.StockGrowthMetricsRepository;
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
            String c = data.getCode();
            if (code.equals(c)) {
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
