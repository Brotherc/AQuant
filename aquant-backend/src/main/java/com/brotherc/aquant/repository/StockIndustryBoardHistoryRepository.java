package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockIndustryBoardHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockIndustryBoardHistoryRepository extends JpaRepository<StockIndustryBoardHistory, Long> {

    List<StockIndustryBoardHistory> findByTradeDateAndBoardCodeIn(String tradeDate, List<String> codeList);

    List<StockIndustryBoardHistory> findByBoardCode(String boardCode);

}
