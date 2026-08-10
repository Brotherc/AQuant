package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockFundInfo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockFundInfoRepository extends JpaRepository<StockFundInfo, Long>, JpaSpecificationExecutor<StockFundInfo> {

    List<StockFundInfo> findByFundCodeIn(Collection<String> fundCodes);

    Optional<StockFundInfo> findByFundCode(String fundCode);

}
