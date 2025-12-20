package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockQuoteHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockQuoteHistoryRepository extends JpaRepository<StockQuoteHistory, Long> {

    List<StockQuoteHistory> findByTradeDateAndCodeIn(String tradeDate, List<String> codeList);

    @Query(
            value = "SELECT * " +
                    "FROM stock_quote_history " +
                    "WHERE code = :code " +
                    "ORDER BY trade_date DESC " +
                    "LIMIT :limit",
            nativeQuery = true
    )
    List<StockQuoteHistory> findLatestByCode(@Param("code") String code, @Param("limit") int limit);

}
