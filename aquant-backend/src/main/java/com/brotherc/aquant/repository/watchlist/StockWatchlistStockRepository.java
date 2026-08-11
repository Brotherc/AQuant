package com.brotherc.aquant.repository.watchlist;

import com.brotherc.aquant.entity.watchlist.StockWatchlistStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockWatchlistStockRepository extends JpaRepository<StockWatchlistStock, Long> {

    List<StockWatchlistStock> findByGroupIdOrderBySortNoDesc(Long groupId);

    void deleteByGroupIdAndStockCode(Long groupId, String stockCode);

    boolean existsByGroupIdAndStockCode(Long groupId, String stockCode);

    void deleteByGroupId(Long groupId);

}
