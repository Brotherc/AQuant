package com.brotherc.aquant.repository.index;

import com.brotherc.aquant.entity.index.StockIndexHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockIndexHistoryRepository extends JpaRepository<StockIndexHistory, Long>, JpaSpecificationExecutor<StockIndexHistory> {

    Optional<StockIndexHistory> findByIndexCodeAndTradeDate(String indexCode, LocalDate tradeDate);

    Optional<StockIndexHistory> findFirstByIndexCodeOrderByTradeDateDesc(String indexCode);

    List<StockIndexHistory> findByIndexCodeOrderByTradeDateDesc(String indexCode);

    List<StockIndexHistory> findByIndexCodeOrderByTradeDateAsc(String indexCode);

}
