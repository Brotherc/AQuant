package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockWatchlistGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockWatchlistGroupRepository extends JpaRepository<StockWatchlistGroup, Long> {

    List<StockWatchlistGroup> findAllByOrderBySortNoAsc();

}
