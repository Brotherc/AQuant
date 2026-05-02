package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockStrategyMomentumBacktestSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface StockStrategyMomentumBacktestSnapshotRepository extends
        JpaRepository<StockStrategyMomentumBacktestSnapshot, Long>,
        JpaSpecificationExecutor<StockStrategyMomentumBacktestSnapshot> {

    boolean existsByBatchNoAndMarketAndLookbackDaysAndRecentYears(
            Long batchNo, String market, Integer lookbackDays, Integer recentYears
    );

    long deleteByBatchNoNot(Long batchNo);

}
