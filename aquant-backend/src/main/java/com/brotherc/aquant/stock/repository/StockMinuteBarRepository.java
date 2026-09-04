package com.brotherc.aquant.stock.repository;

import com.brotherc.aquant.stock.entity.StockMinuteBar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMinuteBarRepository extends JpaRepository<StockMinuteBar, Long> {

    List<StockMinuteBar> findByCodeAndPeriodAndBarTimeGreaterThanEqualOrderByBarTimeAsc(
            String code, Integer period, String barTime);

    /**
     * 该股票在库中最近 N 个有分钟数据的日子（降序，最多 days 个）
     */
    @Query(value = "SELECT DISTINCT SUBSTRING(bar_time, 1, 10) FROM stock_minute_bar "
            + "WHERE code = :code AND period = :period "
            + "ORDER BY SUBSTRING(bar_time, 1, 10) DESC LIMIT :days", nativeQuery = true)
    List<String> findRecentTradeDates(@Param("code") String code, @Param("period") Integer period, @Param("days") int days);
}
