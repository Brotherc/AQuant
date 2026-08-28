package com.brotherc.aquant.indicator.repository;

import com.brotherc.aquant.indicator.entity.StockGrowthMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface StockGrowthMetricsRepository
        extends JpaRepository<StockGrowthMetrics, Long>, JpaSpecificationExecutor<StockGrowthMetrics> {

    StockGrowthMetrics findByStockCode(String stockCode);

    @Query("SELECT DISTINCT s.industry FROM StockGrowthMetrics s WHERE s.industry IS NOT NULL AND s.industry != '' ORDER BY s.industry")
    List<String> findDistinctIndustries();

}
