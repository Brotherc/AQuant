package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockShareChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockShareChangeRepository extends JpaRepository<StockShareChange, Long> {
}
