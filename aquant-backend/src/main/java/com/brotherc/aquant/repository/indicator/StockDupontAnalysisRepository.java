package com.brotherc.aquant.repository.indicator;

import com.brotherc.aquant.entity.indicator.StockDupontAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockDupontAnalysisRepository extends JpaRepository<StockDupontAnalysis, Long>, JpaSpecificationExecutor<StockDupontAnalysis> {

    StockDupontAnalysis findByStockCode(String stockCode);

    List<StockDupontAnalysis> findByStockCodeIn(List<String> stockCodes);

}
