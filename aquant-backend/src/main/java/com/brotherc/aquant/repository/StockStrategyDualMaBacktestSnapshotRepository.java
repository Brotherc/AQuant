package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockStrategyDualMaBacktestSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface StockStrategyDualMaBacktestSnapshotRepository extends
        JpaRepository<StockStrategyDualMaBacktestSnapshot, Long>,
        JpaSpecificationExecutor<StockStrategyDualMaBacktestSnapshot> {

    boolean existsByBatchNoAndMarketAndMaShortAndMaLongAndRecentYears(
            Long batchNo, String market, Integer maShort, Integer maLong, Integer recentYears
    );

    long deleteByBatchNoNot(Long batchNo);

}
