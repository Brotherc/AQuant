package com.brotherc.aquant.industry.repository;

import com.brotherc.aquant.industry.entity.StockBoardConstituentEm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockBoardConstituentEmRepository extends JpaRepository<StockBoardConstituentEm, Long> {
    List<StockBoardConstituentEm> findByBoardCodeOrderByStockCodeAsc(String boardCode);
    boolean existsByBoardCode(String boardCode);
    long deleteByBoardCodeAndStockCodeNotIn(String boardCode, List<String> stockCodeList);
}
