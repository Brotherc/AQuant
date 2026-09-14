package com.brotherc.aquant.industry.repository;

import com.brotherc.aquant.industry.entity.StockIndustryBoardEm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockIndustryBoardEmRepository extends JpaRepository<StockIndustryBoardEm, Long> {
    StockIndustryBoardEm findBySectorName(String sectorName);
    List<StockIndustryBoardEm> findAllByOrderBySeqNoAsc();
    Page<StockIndustryBoardEm> findBySectorNameContaining(String sectorName, Pageable pageable);
}
