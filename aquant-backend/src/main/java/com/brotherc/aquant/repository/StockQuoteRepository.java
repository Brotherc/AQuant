package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.model.dto.stockquote.StockCodeName;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockQuoteRepository extends JpaRepository<StockQuote, Long> {

    List<StockQuote> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

    @Query("select distinct new com.brotherc.aquant.model.dto.stockquote.StockCodeName(s.code, s.name) from StockQuote s")
    List<StockCodeName> findAllStockCodes();

}