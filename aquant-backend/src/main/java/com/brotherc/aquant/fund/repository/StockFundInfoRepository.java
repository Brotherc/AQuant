package com.brotherc.aquant.fund.repository;

import com.brotherc.aquant.fund.entity.StockFundInfo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockFundInfoRepository extends JpaRepository<StockFundInfo, Long>, JpaSpecificationExecutor<StockFundInfo> {

    List<StockFundInfo> findByFundCodeIn(Collection<String> fundCodes);

    Optional<StockFundInfo> findByFundCode(String fundCode);

    @Query("SELECT DISTINCT s.fundType FROM StockFundInfo s WHERE s.fundType IS NOT NULL AND s.fundType <> '' ORDER BY s.fundType ASC")
    List<String> findDistinctFundTypes();

}
