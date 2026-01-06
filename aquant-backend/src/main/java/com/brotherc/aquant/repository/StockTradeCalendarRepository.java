package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockTradeCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockTradeCalendarRepository extends JpaRepository<StockTradeCalendar, Long> {

    boolean existsByTradeDateAndMarket(String tradeDate, String market);

}
