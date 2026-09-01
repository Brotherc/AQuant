package com.brotherc.aquant.industry.repository;

import com.brotherc.aquant.industry.entity.StockIndustryBoardHistoryEm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockIndustryBoardHistoryEmRepository extends JpaRepository<StockIndustryBoardHistoryEm, Long> {
    List<StockIndustryBoardHistoryEm> findByTradeDateBetweenOrderByTradeDateAscSectorNameAsc(String startDate, String endDate);
    List<StockIndustryBoardHistoryEm> findBySectorNameOrderByTradeDateAsc(String sectorName);
    StockIndustryBoardHistoryEm findBySectorNameAndTradeDate(String sectorName, String tradeDate);
    StockIndustryBoardHistoryEm findTopBySectorNameOrderByTradeDateDesc(String sectorName);
}
