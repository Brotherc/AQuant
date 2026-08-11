package com.brotherc.aquant.repository.stock;

import com.brotherc.aquant.entity.stock.StockShareChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockShareChangeRepository extends JpaRepository<StockShareChange, Long> {
}
