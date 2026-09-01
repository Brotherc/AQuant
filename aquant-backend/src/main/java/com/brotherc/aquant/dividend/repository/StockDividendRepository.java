package com.brotherc.aquant.dividend.repository;

import com.brotherc.aquant.dividend.entity.StockDividend;
import com.brotherc.aquant.stock.model.dto.StockDividendProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockDividendRepository extends JpaRepository<StockDividend, Long> {

    void deleteByReportDate(String reportDate);

    boolean existsByReportDate(String reportDate);

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

    @Query("""
            select d from StockDividend d
            where exists (
                select 1 from StockDividend dup
                where dup.stockCode = d.stockCode
                  and dup.latestAnnouncementDate = d.latestAnnouncementDate
                group by dup.stockCode, dup.latestAnnouncementDate
                having count(dup) > 1
            )
            """)
    List<StockDividend> findDuplicateLatestAnnouncementDateRows();

    List<StockDividend> findByPlanStatus(String planStatus);

    long deleteByIdIn(List<Long> ids);

}
