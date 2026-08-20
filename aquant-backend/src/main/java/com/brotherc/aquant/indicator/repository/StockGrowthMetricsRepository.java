package com.brotherc.aquant.indicator.repository;

import com.brotherc.aquant.indicator.entity.StockGrowthMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface StockGrowthMetricsRepository
        extends JpaRepository<StockGrowthMetrics, Long>, JpaSpecificationExecutor<StockGrowthMetrics> {

    StockGrowthMetrics findByStockCode(String stockCode);

}
