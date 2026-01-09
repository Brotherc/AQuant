package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockIndustryBoardHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockIndustryBoardHistoryRepository extends JpaRepository<StockIndustryBoardHistory, Long>, JpaSpecificationExecutor<StockIndustryBoardHistory> {

    List<StockIndustryBoardHistory> findByTradeDateAndBoardCodeIn(String tradeDate, List<String> codeList);

    List<StockIndustryBoardHistory> findByBoardCode(String boardCode);

    List<StockIndustryBoardHistory> findByBoardCodeOrderByTradeDateAsc(String boardCode);

}
