package com.brotherc.aquant.repository.stock;

import com.brotherc.aquant.entity.stock.StockTradeCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockTradeCalendarRepository extends JpaRepository<StockTradeCalendar, Long> {

    boolean existsByTradeDateAndMarket(String tradeDate, String market);

    StockTradeCalendar findByTradeDate(String tradeDate);

}
