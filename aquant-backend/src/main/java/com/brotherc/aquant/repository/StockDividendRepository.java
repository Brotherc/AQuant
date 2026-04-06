package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockDividend;
import com.brotherc.aquant.model.projection.StockDividendProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockDividendRepository extends JpaRepository<StockDividend, Long> {

    void deleteByReportDate(String reportDate);

    @Query("select d.stockCode as stockCode, d.stockName as stockName, " +
            "d.latestAnnouncementDate as latestAnnouncementDate, d.cashDividendRatio as cashDividendRatio, " +
            "d.bonusShareRatio as bonusShareRatio, d.transferShareRatio as transferShareRatio " +
            "from StockDividend d where d.latestAnnouncementDate >= :fromDate")
    List<StockDividendProjection> findByLatestAnnouncementDateGreaterThanEqualProjectedBy(@Param("fromDate") LocalDate fromDate);

    @Query("select d.stockCode as stockCode, d.stockName as stockName, " +
            "d.latestAnnouncementDate as latestAnnouncementDate, d.cashDividendRatio as cashDividendRatio, " +
            "d.bonusShareRatio as bonusShareRatio, d.transferShareRatio as transferShareRatio " +
            "from StockDividend d")
    List<StockDividendProjection> findAllProjectedBy();

    List<StockDividend> findByStockCodeOrderByLatestAnnouncementDateDesc(String stockCode);

    List<StockDividend> findByStockCodeIn(List<String> stockCodes);

}
