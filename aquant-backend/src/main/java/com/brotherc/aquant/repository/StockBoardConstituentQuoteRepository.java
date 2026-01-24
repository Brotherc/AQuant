package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockBoardConstituentQuote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockBoardConstituentQuoteRepository extends JpaRepository<StockBoardConstituentQuote, Long> {

    Page<StockBoardConstituentQuote> findByBoardCode(String boardCode, Pageable pageable);

    StockBoardConstituentQuote findFirstByBoardCode(String boardCode);

    List<StockBoardConstituentQuote> findByBoardCodeAndStockCodeIn(String boardCode, List<String> stockCodeList);

}
