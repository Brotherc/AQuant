package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockFundNetValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockFundNetValueRepository extends JpaRepository<StockFundNetValue, Long> {

    List<StockFundNetValue> findByFundCodeOrderByNavDateAsc(String fundCode);

}
