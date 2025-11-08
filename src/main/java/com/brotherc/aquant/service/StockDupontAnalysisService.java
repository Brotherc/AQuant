package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockDupontAnalysis;
import com.brotherc.aquant.model.dto.stock.StockZhDupontComparisonEm;
import com.brotherc.aquant.repository.StockDupontAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockDupontAnalysisService {

    private final StockDupontAnalysisRepository stockDupontAnalysisRepository;

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
            String c = data.get代码();
            if (code.equals(c)) {
                stockDupontAnalysis.setRoe3yAvgRank(data.getROE_3年平均排名());

                stockDupontAnalysis.setRoe3yAvg(data.getROE_3年平均());
                stockDupontAnalysis.setRoeLast3yA(data.getROE_22A());
                stockDupontAnalysis.setRoeLast2yA(data.getROE_23A());
                stockDupontAnalysis.setRoeLastYA(data.getROE_24A());

                stockDupontAnalysis.setNetMargin3yAvg(data.get净利率_3年平均());
                stockDupontAnalysis.setNetMarginLast3yA(data.get净利率_22A());
                stockDupontAnalysis.setNetMarginLast2yA(data.get净利率_23A());
                stockDupontAnalysis.setNetMarginLastYA(data.get净利率_24A());

                stockDupontAnalysis.setAssetTurnover3yAvg(data.get总资产周转率_3年平均());
                stockDupontAnalysis.setAssetTurnoverLast3yA(data.get总资产周转率_22A());
                stockDupontAnalysis.setAssetTurnoverLast2yA(data.get总资产周转率_23A());
                stockDupontAnalysis.setAssetTurnoverLastYA(data.get总资产周转率_24A());

                stockDupontAnalysis.setEquityMultiplier3yAvg(data.get权益乘数_3年平均());
                stockDupontAnalysis.setEquityMultiplierLast3yA(data.get权益乘数_22A());
                stockDupontAnalysis.setEquityMultiplierLast2yA(data.get权益乘数_23A());
                stockDupontAnalysis.setEquityMultiplierLastYA(data.get权益乘数_24A());

                stockDupontAnalysis.setCreatedAt(LocalDateTime.now());
            } else if ("行业中值".equals(c)) {
                stockDupontAnalysis.setRoe3yAvgIndustryMed(data.getROE_3年平均());
                stockDupontAnalysis.setRoeLast3yAIndustryMed(data.getROE_22A());
                stockDupontAnalysis.setRoeLast2yAIndustryMed(data.getROE_23A());
                stockDupontAnalysis.setRoeLastYAIndustryMed(data.getROE_24A());

                stockDupontAnalysis.setNetMargin3yAvgIndustryMed(data.get净利率_3年平均());
                stockDupontAnalysis.setNetMarginLast3yAIndustryMed(data.get净利率_22A());
                stockDupontAnalysis.setNetMarginLast2yAIndustryMed(data.get净利率_23A());
                stockDupontAnalysis.setNetMarginLastYAIndustryMed(data.get净利率_24A());

                stockDupontAnalysis.setAssetTurnover3yAvgIndustryMed(data.get总资产周转率_3年平均());
                stockDupontAnalysis.setAssetTurnoverLast3yAIndustryMed(data.get总资产周转率_22A());
                stockDupontAnalysis.setAssetTurnoverLast2yAIndustryMed(data.get总资产周转率_23A());
                stockDupontAnalysis.setAssetTurnoverLastYAIndustryMed(data.get总资产周转率_24A());

                stockDupontAnalysis.setEquityMultiplier3yAvgIndustryMed(data.get权益乘数_3年平均());
                stockDupontAnalysis.setEquityMultiplierLast3yAIndustryMed(data.get权益乘数_22A());
                stockDupontAnalysis.setEquityMultiplierLast2yAIndustryMed(data.get权益乘数_23A());
                stockDupontAnalysis.setEquityMultiplierLastYAIndustryMed(data.get权益乘数_24A());

                stockDupontAnalysis.setCreatedAt(LocalDateTime.now());
            } else if ("行业平均".equals(c)) {
                stockDupontAnalysis.setRoe3yAvgIndustryAvg(data.getROE_3年平均());
                stockDupontAnalysis.setRoeLast3yAIndustryAvg(data.getROE_22A());
                stockDupontAnalysis.setRoeLast2yAIndustryAvg(data.getROE_23A());
                stockDupontAnalysis.setRoeLastYAIndustryAvg(data.getROE_24A());

                stockDupontAnalysis.setNetMargin3yAvgIndustryAvg(data.get净利率_3年平均());
                stockDupontAnalysis.setNetMarginLast3yAIndustryAvg(data.get净利率_22A());
                stockDupontAnalysis.setNetMarginLast2yAIndustryAvg(data.get净利率_23A());
                stockDupontAnalysis.setNetMarginLastYAIndustryAvg(data.get净利率_24A());

                stockDupontAnalysis.setAssetTurnover3yAvgIndustryAvg(data.get总资产周转率_3年平均());
                stockDupontAnalysis.setAssetTurnoverLast3yAIndustryAvg(data.get总资产周转率_22A());
                stockDupontAnalysis.setAssetTurnoverLast2yAIndustryAvg(data.get总资产周转率_23A());
                stockDupontAnalysis.setAssetTurnoverLastYAIndustryAvg(data.get总资产周转率_24A());

                stockDupontAnalysis.setEquityMultiplier3yAvgIndustryAvg(data.get权益乘数_3年平均());
                stockDupontAnalysis.setEquityMultiplierLast3yAIndustryAvg(data.get权益乘数_22A());
                stockDupontAnalysis.setEquityMultiplierLast2yAIndustryAvg(data.get权益乘数_23A());
                stockDupontAnalysis.setEquityMultiplierLastYAIndustryAvg(data.get权益乘数_24A());

                stockDupontAnalysis.setCreatedAt(LocalDateTime.now());
            }
        }
        stockDupontAnalysisRepository.save(stockDupontAnalysis);
    }

}
