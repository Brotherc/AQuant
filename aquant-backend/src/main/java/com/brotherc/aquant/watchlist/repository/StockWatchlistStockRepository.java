package com.brotherc.aquant.watchlist.repository;

import com.brotherc.aquant.watchlist.entity.StockWatchlistStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockWatchlistStockRepository extends JpaRepository<StockWatchlistStock, Long> {

    List<StockWatchlistStock> findByGroupIdOrderBySortNoDesc(Long groupId);

    List<StockWatchlistStock> findByGroupIdIn(List<Long> groupIds);

    void deleteByGroupIdAndStockCode(Long groupId, String stockCode);

    boolean existsByGroupIdAndStockCode(Long groupId, String stockCode);

    void deleteByGroupId(Long groupId);

}
