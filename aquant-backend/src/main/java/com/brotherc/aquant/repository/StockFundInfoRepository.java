package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockFundInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockFundInfoRepository extends JpaRepository<StockFundInfo, Long> {
}