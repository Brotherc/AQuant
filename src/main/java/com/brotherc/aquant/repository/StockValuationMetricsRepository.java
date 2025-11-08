package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockValuationMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockValuationMetricsRepository extends JpaRepository<StockValuationMetrics, Long> {

    StockValuationMetrics findByStockCode(String stockCode);

}
