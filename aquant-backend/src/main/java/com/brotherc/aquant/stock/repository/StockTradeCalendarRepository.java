package com.brotherc.aquant.stock.repository;

import com.brotherc.aquant.stock.entity.StockTradeCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockTradeCalendarRepository extends JpaRepository<StockTradeCalendar, Long> {

    boolean existsByTradeDateAndMarket(String tradeDate, String market);

    StockTradeCalendar findByTradeDate(String tradeDate);

}
