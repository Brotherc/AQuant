package com.brotherc.aquant.repository.indicator;

import com.brotherc.aquant.entity.indicator.StockBalanceSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface StockBalanceSheetRepository extends JpaRepository<StockBalanceSheet, Long> {

    void deleteByReportDate(LocalDate reportDate);

    boolean existsByReportDate(LocalDate reportDate);

}
