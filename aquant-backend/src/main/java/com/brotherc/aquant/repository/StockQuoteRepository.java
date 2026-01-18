package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockQuote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockQuoteRepository extends JpaRepository<StockQuote, Long>, JpaSpecificationExecutor<StockQuote> {

    List<StockQuote> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

    @Query("select max(s.createdAt) from StockQuote s")
    LocalDateTime findMaxCreatedAt();

    List<StockQuote> findByCreatedAt(LocalDateTime createdAt);

    List<StockQuote> findByCodeIn(List<String> codeList);

}