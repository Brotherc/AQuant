package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockStrategyDualMaBacktestSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockStrategyDualMaBacktestSnapshotRepository extends
        JpaRepository<StockStrategyDualMaBacktestSnapshot, Long>,
        JpaSpecificationExecutor<StockStrategyDualMaBacktestSnapshot> {

    boolean existsByBatchNoAndMarketAndMaShortAndMaLongAndRecentYears(
            Long batchNo, String market, Integer maShort, Integer maLong, Integer recentYears
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM stock_strategy_dual_ma_backtest_snapshot WHERE batch_no <> :batchNo LIMIT :limit", nativeQuery = true)
    int deleteOldBatchLimit(@Param("batchNo") Long batchNo, @Param("limit") int limit);

}
