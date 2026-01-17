package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockDividend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDividendRepository extends JpaRepository<StockDividend, Long> {

    void deleteByReportDate(String reportDate);

}
