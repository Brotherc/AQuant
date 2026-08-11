package com.brotherc.aquant.repository.watchlist;

import com.brotherc.aquant.entity.watchlist.StockWatchlistGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockWatchlistGroupRepository extends JpaRepository<StockWatchlistGroup, Long> {

    List<StockWatchlistGroup> findAllByUserIdOrderBySortNoAsc(Long userId);

    List<StockWatchlistGroup> findAllByUserIdAndTypeOrderBySortNoAsc(Long userId, String type);

    Optional<StockWatchlistGroup> findById(Long id);

    Optional<StockWatchlistGroup> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);

    boolean existsByUserIdAndNameAndType(Long userId, String name, String type);

}
