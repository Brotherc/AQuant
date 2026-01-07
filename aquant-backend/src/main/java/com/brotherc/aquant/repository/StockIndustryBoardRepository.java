package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockIndustryBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface StockIndustryBoardRepository extends JpaRepository<StockIndustryBoard, Long>, JpaSpecificationExecutor<StockIndustryBoard> {

    StockIndustryBoard findByBoardName(String boardName);

}