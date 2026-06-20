package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockFundPortfolioHolding;
import com.brotherc.aquant.model.dto.akshare.FundPortfolioHoldEm;
import com.brotherc.aquant.repository.StockFundPortfolioHoldingRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockFundPortfolioHoldingService {

    private final StockFundPortfolioHoldingRepository stockFundPortfolioHoldingRepository;

    public List<String> findSyncedFundCodes(List<String> fundCodes, Integer reportYear, Integer reportQuarter) {
        if (CollectionUtils.isEmpty(fundCodes) || reportYear == null || reportQuarter == null) {
            return List.of();
        }
        return stockFundPortfolioHoldingRepository.findFundCodesByReportYearAndReportQuarterAndFundCodeIn(
                reportYear, reportQuarter, fundCodes
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveFundPortfolioHoldings(
            String fundCode, Integer reportYear, Integer reportQuarter, List<FundPortfolioHoldEm> holdings
    ) {
        if (StringUtils.isBlank(fundCode) || reportYear == null || reportQuarter == null || CollectionUtils.isEmpty(holdings)) {
            return;
        }

        stockFundPortfolioHoldingRepository.deleteByFundCodeAndReportYearAndReportQuarter(
                fundCode, reportYear, reportQuarter
        );

        List<StockFundPortfolioHolding> saveList = new ArrayList<>();
        for (FundPortfolioHoldEm holding : holdings) {
            if (holding == null || StringUtils.isBlank(holding.getStockCode())) {
                continue;
            }
            StockFundPortfolioHolding entity = new StockFundPortfolioHolding();
            entity.setFundCode(fundCode);
            entity.setReportYear(reportYear);
            entity.setReportQuarter(reportQuarter);
            entity.setSeqNo(holding.getSeqNo());
            entity.setStockCode(holding.getStockCode());
            entity.setStockName(holding.getStockName());
            entity.setNetValueRatio(holding.getNetValueRatio());
            entity.setHoldShares(holding.getHoldShares());
            entity.setMarketValue(holding.getMarketValue());
            saveList.add(entity);
        }

        if (!saveList.isEmpty()) {
            stockFundPortfolioHoldingRepository.saveAll(saveList);
        }
    }

    public List<StockFundPortfolioHolding> getLatestFundHoldings(String fundCode) {
        if (StringUtils.isBlank(fundCode)) {
            return List.of();
        }
        StockFundPortfolioHolding latest = stockFundPortfolioHoldingRepository.findFirstByFundCodeOrderByReportYearDescReportQuarterDesc(fundCode);
        if (latest == null) {
            return List.of();
        }
        return stockFundPortfolioHoldingRepository.findByFundCodeAndReportYearAndReportQuarterOrderBySeqNoAsc(
                fundCode, latest.getReportYear(), latest.getReportQuarter()
        );
    }

}
