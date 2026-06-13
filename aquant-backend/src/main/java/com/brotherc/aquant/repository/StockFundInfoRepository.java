package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockFundInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface StockFundInfoRepository extends JpaRepository<StockFundInfo, Long>, JpaSpecificationExecutor<StockFundInfo> {
}