package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockDupontAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDupontAnalysisRepository extends JpaRepository<StockDupontAnalysis, Long>, JpaSpecificationExecutor<StockDupontAnalysis> {

    StockDupontAnalysis findByStockCode(String stockCode);

}
