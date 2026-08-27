package com.brotherc.aquant.indicator.repository;

import com.brotherc.aquant.indicator.entity.StockPerformanceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockPerformanceReportRepository extends JpaRepository<StockPerformanceReport, Long> {

    void deleteByReportDate(LocalDate reportDate);

    boolean existsByReportDate(LocalDate reportDate);

    @Query("SELECT DISTINCT s.industry FROM StockPerformanceReport s WHERE s.industry IS NOT NULL AND s.industry != '' ORDER BY s.industry ASC")
    List<String> findDistinctIndustries();

}
