package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockValuationMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockValuationMetricsRepository extends JpaRepository<StockValuationMetrics, Long>, JpaSpecificationExecutor<StockValuationMetrics> {

    StockValuationMetrics findByStockCode(String stockCode);

    List<StockValuationMetrics> findByStockCodeIn(List<String> stockCodes);

}
