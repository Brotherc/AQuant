package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockValuationMetrics;
import com.brotherc.aquant.model.dto.akshare.StockZhValuationComparisonEm;
import com.brotherc.aquant.repository.StockValuationMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockValuationMetricsService {

    private final StockValuationMetricsRepository stockValuationMetricsRepository;

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
            String c = data.get代码();
            if (code.equals(c)) {
                stockValuationMetrics.setPeg(data.getPEG());
                stockValuationMetrics.setPegRank(data.getPEG排名());

                stockValuationMetrics.setPeLastYearA(data.get市盈率_24A());
                stockValuationMetrics.setPeTtm(data.get市盈率_TTM());
                stockValuationMetrics.setPeThisYE(data.get市盈率_25E());
                stockValuationMetrics.setPeNextYE(data.get市盈率_26E());
                stockValuationMetrics.setPeNext2YE(data.get市盈率_27E());

                stockValuationMetrics.setPsLastYA(data.get市销率_24A());
                stockValuationMetrics.setPsTtm(data.get市销率_TTM());
                stockValuationMetrics.setPsThisYE(data.get市销率_25E());
                stockValuationMetrics.setPsNextYE(data.get市销率_26E());
                stockValuationMetrics.setPsNext2YE(data.get市销率_27E());

                stockValuationMetrics.setPbLastYA(data.get市净率_24A());
                stockValuationMetrics.setPbMrq(data.get市净率_MRQ());

                stockValuationMetrics.setPceLastYA(data.get市现率PCE_24A());
                stockValuationMetrics.setPceTtm(data.get市现率PCE_TTM());

                stockValuationMetrics.setPcfLastYA(data.get市现率PCF_24A());
                stockValuationMetrics.setPcfTtm(data.get市现率PCF_TTM());

                stockValuationMetrics.setEvEbitdaLastYA(data.getEV_EBITDA_24A());

                stockValuationMetrics.setCreatedAt(LocalDateTime.now());
            } else if ("行业中值".equals(c)) {
                stockValuationMetrics.setPegIndustryMed(data.getPEG());

                stockValuationMetrics.setPeLastYearIndustryMed(data.get市盈率_24A());
                stockValuationMetrics.setPeTtmIndustryMed(data.get市盈率_TTM());
                stockValuationMetrics.setPeThisYEIndustryMed(data.get市盈率_25E());
                stockValuationMetrics.setPeNextYEIndustryMed(data.get市盈率_26E());
                stockValuationMetrics.setPeNext2YEIndustryMed(data.get市盈率_27E());

                stockValuationMetrics.setPsLastYAIndustryMed(data.get市销率_24A());
                stockValuationMetrics.setPsTtmIndustryMed(data.get市销率_TTM());
                stockValuationMetrics.setPsThisYEIndustryMed(data.get市销率_25E());
                stockValuationMetrics.setPsNextYEIndustryMed(data.get市销率_26E());
                stockValuationMetrics.setPsNext2YEIndustryMed(data.get市销率_27E());

                stockValuationMetrics.setPbLastYAIndustryMed(data.get市净率_24A());
                stockValuationMetrics.setPbMrqIndustryMed(data.get市净率_MRQ());

                stockValuationMetrics.setPceLastYAIndustryMed(data.get市现率PCE_24A());
                stockValuationMetrics.setPceTtmIndustryMed(data.get市现率PCE_TTM());

                stockValuationMetrics.setPcfLastYAIndustryMed(data.get市现率PCF_24A());
                stockValuationMetrics.setPcfTtmIndustryAvg(data.get市现率PCF_TTM());

                stockValuationMetrics.setEvEbitdaLastYAIndustryMed(data.getEV_EBITDA_24A());

                stockValuationMetrics.setCreatedAt(LocalDateTime.now());
            } else if ("行业平均".equals(c)) {
                stockValuationMetrics.setPegIndustryAvg(data.getPEG());

                stockValuationMetrics.setPeLastYearIndustryAvg(data.get市盈率_24A());
                stockValuationMetrics.setPeTtmIndustryAvg(data.get市盈率_TTM());
                stockValuationMetrics.setPeThisYEIndustryAvg(data.get市盈率_25E());
                stockValuationMetrics.setPeNextYEIndustryAvg(data.get市盈率_26E());
                stockValuationMetrics.setPeNext2YEIndustryAvg(data.get市盈率_27E());

                stockValuationMetrics.setPsLastYAIndustryAvg(data.get市销率_24A());
                stockValuationMetrics.setPsTtmIndustryAvg(data.get市销率_TTM());
                stockValuationMetrics.setPsThisYEIndustryAvg(data.get市销率_25E());
                stockValuationMetrics.setPsNextYEIndustryAvg(data.get市销率_26E());
                stockValuationMetrics.setPsNext2YEIndustryAvg(data.get市销率_27E());

                stockValuationMetrics.setPbLastYAIndustryAvg(data.get市净率_24A());
                stockValuationMetrics.setPbMrqIndustryAvg(data.get市净率_MRQ());

                stockValuationMetrics.setPceLastYAIndustryAvg(data.get市现率PCE_24A());
                stockValuationMetrics.setPceTtmIndustryAvg(data.get市现率PCE_TTM());

                stockValuationMetrics.setPcfLastYAIndustryAvg(data.get市现率PCF_24A());
                stockValuationMetrics.setPcfTtmIndustryAvg(data.get市现率PCF_TTM());

                stockValuationMetrics.setEvEbitdaLastYAIndustryAvg(data.getEV_EBITDA_24A());

                stockValuationMetrics.setCreatedAt(LocalDateTime.now());
            }
        }
        stockValuationMetricsRepository.save(stockValuationMetrics);
    }

}
