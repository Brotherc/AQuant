package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockQuote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockQuoteRepository extends JpaRepository<StockQuote, Long> {

    List<StockQuote> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

}