package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockFundPortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockFundPortfolioHoldingRepository extends JpaRepository<StockFundPortfolioHolding, Long> {

    @Query("select distinct s.fundCode from StockFundPortfolioHolding s " +
            "where s.reportYear = :reportYear and s.reportQuarter = :reportQuarter and s.fundCode in :fundCodes")
    List<String> findFundCodesByReportYearAndReportQuarterAndFundCodeIn(
            @Param("reportYear") Integer reportYear,
            @Param("reportQuarter") Integer reportQuarter,
            @Param("fundCodes") List<String> fundCodes
    );

    @Modifying
    void deleteByFundCodeAndReportYearAndReportQuarter(String fundCode, Integer reportYear, Integer reportQuarter);

    StockFundPortfolioHolding findFirstByFundCodeOrderByReportYearDescReportQuarterDesc(String fundCode);

    List<StockFundPortfolioHolding> findByFundCodeAndReportYearAndReportQuarterOrderBySeqNoAsc(String fundCode, Integer reportYear, Integer reportQuarter);

}
