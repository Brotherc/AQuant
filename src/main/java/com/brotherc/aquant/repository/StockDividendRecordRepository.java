package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockDividendRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDividendRecordRepository extends JpaRepository<StockDividendRecord, Long> {
}
