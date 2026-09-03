package com.brotherc.aquant.strategy.repository;

import com.brotherc.aquant.strategy.entity.StockStrategyMacdBacktestSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockStrategyMacdBacktestSnapshotRepository extends
        JpaRepository<StockStrategyMacdBacktestSnapshot, Long>,
        JpaSpecificationExecutor<StockStrategyMacdBacktestSnapshot> {

    boolean existsByBatchNoAndMarketAndFastPeriodAndSlowPeriodAndSignalPeriodAndRecentYears(
            Long batchNo,
            String market,
            Integer fastPeriod,
            Integer slowPeriod,
            Integer signalPeriod,
            Integer recentYears
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM stock_strategy_macd_backtest_snapshot WHERE batch_no <> :batchNo LIMIT :limit", nativeQuery = true)
    int deleteOldBatchLimit(@Param("batchNo") Long batchNo, @Param("limit") int limit);

}
