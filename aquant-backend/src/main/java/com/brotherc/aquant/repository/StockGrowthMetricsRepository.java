package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockGrowthMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockGrowthMetricsRepository extends JpaRepository<StockGrowthMetrics, Long> {

    StockGrowthMetrics findByStockCode(String stockCode);

}
