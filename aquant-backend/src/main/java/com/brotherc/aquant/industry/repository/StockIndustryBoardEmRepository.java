package com.brotherc.aquant.industry.repository;

import com.brotherc.aquant.industry.entity.StockIndustryBoardEm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockIndustryBoardEmRepository extends JpaRepository<StockIndustryBoardEm, Long> {
    StockIndustryBoardEm findBySectorName(String sectorName);

    StockIndustryBoardEm findBySectorCode(String sectorCode);

    List<StockIndustryBoardEm> findAllByOrderBySeqNoAsc();

    Page<StockIndustryBoardEm> findBySectorNameContaining(String sectorName, Pageable pageable);

    /** 是否存在详情快照早于指定日期或从未抓取过的板块（详情按日刷新的门控） */
    boolean existsByDetailTradeDateLessThanOrDetailTradeDateIsNull(LocalDate detailTradeDate);
}
