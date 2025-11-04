package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockQuoteRepository extends JpaRepository<StockQuote, Long> {

}