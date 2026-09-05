package com.brotherc.aquant.strategy.repository;

import com.brotherc.aquant.strategy.entity.StockStrategyGridBacktestSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface StockStrategyGridBacktestSnapshotRepository extends
        JpaRepository<StockStrategyGridBacktestSnapshot, Long>,
        JpaSpecificationExecutor<StockStrategyGridBacktestSnapshot> {

    boolean existsByBatchNoAndMarketAndGridRateAndGridCountAndRecentYears(
            Long batchNo,
            String market,
            BigDecimal gridRate,
            Integer gridCount,
            Integer recentYears
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM stock_strategy_grid_backtest_snapshot WHERE batch_no <> :batchNo LIMIT :limit", nativeQuery = true)
    int deleteOldBatchLimit(@Param("batchNo") Long batchNo, @Param("limit") int limit);
}
