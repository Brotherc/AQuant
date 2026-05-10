package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockStrategyMomentumBacktestSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockStrategyMomentumBacktestSnapshotRepository extends
        JpaRepository<StockStrategyMomentumBacktestSnapshot, Long>,
        JpaSpecificationExecutor<StockStrategyMomentumBacktestSnapshot> {

    boolean existsByBatchNoAndMarketAndLookbackDaysAndRecentYears(
            Long batchNo, String market, Integer lookbackDays, Integer recentYears
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM stock_strategy_momentum_backtest_snapshot WHERE batch_no <> :batchNo LIMIT :limit", nativeQuery = true)
    int deleteOldBatchLimit(@Param("batchNo") Long batchNo, @Param("limit") int limit);

}
