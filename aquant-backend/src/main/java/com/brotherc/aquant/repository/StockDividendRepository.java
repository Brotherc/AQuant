package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockDividend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockDividendRepository extends JpaRepository<StockDividend, Long> {

    void deleteByReportDate(String reportDate);

    @Query("""
                SELECT sd
                FROM StockDividend sd
                WHERE sd.latestAnnouncementDate >= :fromDate
            """)
    List<StockDividend> findByLatestAnnouncementDateAfter(@Param("fromDate") LocalDate fromDate);

}
