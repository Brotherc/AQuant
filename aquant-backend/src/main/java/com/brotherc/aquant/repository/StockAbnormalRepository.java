package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockAbnormal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockAbnormalRepository extends JpaRepository<StockAbnormal, Long> {
}
