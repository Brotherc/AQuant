package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockIndustryBoardHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockIndustryBoardHistoryRepository extends JpaRepository<StockIndustryBoardHistory, Long>, JpaSpecificationExecutor<StockIndustryBoardHistory> {

    List<StockIndustryBoardHistory> findByTradeDateAndSectorNameIn(String tradeDate, List<String> sectorNames);

    List<StockIndustryBoardHistory> findBySectorName(String sectorName);

    List<StockIndustryBoardHistory> findBySectorNameOrderByTradeDateAsc(String sectorName);

    List<StockIndustryBoardHistory> findBySectorNameAndTradeDateBetweenOrderByTradeDateAsc(String sectorName, String startDate, String endDate);

    List<StockIndustryBoardHistory> findByTradeDateBetween(String startTradeDate, String endTradeDate);

    @Query("select max(s.tradeDate) from StockIndustryBoardHistory s")
    String findMaxTradeDate();

}
