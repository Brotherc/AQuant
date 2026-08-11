package com.brotherc.aquant.repository.indicator;

import com.brotherc.aquant.entity.indicator.StockPerformanceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface StockPerformanceReportRepository extends JpaRepository<StockPerformanceReport, Long> {

    void deleteByReportDate(LocalDate reportDate);

    boolean existsByReportDate(LocalDate reportDate);

}
