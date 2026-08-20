package com.brotherc.aquant.stock.repository;

import com.brotherc.aquant.stock.entity.StockAbnormal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockAbnormalRepository extends JpaRepository<StockAbnormal, Long> {
}
