package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockQuoteHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockQuoteHistoryRepository extends JpaRepository<StockQuoteHistory, Long> {

    List<StockQuoteHistory> findByTradeDateAndCodeIn(String tradeDate, List<String> codeList);

}
