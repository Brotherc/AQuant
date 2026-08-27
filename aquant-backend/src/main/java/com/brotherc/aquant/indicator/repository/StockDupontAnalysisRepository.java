package com.brotherc.aquant.indicator.repository;

import com.brotherc.aquant.indicator.entity.StockDupontAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockDupontAnalysisRepository extends JpaRepository<StockDupontAnalysis, Long>, JpaSpecificationExecutor<StockDupontAnalysis> {

    StockDupontAnalysis findByStockCode(String stockCode);

    List<StockDupontAnalysis> findByStockCodeIn(List<String> stockCodes);

    @Query("SELECT DISTINCT s.industry FROM StockDupontAnalysis s WHERE s.industry IS NOT NULL AND s.industry != '' ORDER BY s.industry ASC")
    List<String> findDistinctIndustries();

}
