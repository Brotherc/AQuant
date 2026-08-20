package com.brotherc.aquant.indicator.repository;

import com.brotherc.aquant.indicator.entity.StockPerformanceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface StockPerformanceReportRepository extends JpaRepository<StockPerformanceReport, Long> {

    void deleteByReportDate(LocalDate reportDate);

    boolean existsByReportDate(LocalDate reportDate);

}
