package com.brotherc.aquant.repository.index;

import com.brotherc.aquant.entity.index.StockIndexSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockIndexSpotRepository extends JpaRepository<StockIndexSpot, Long>, JpaSpecificationExecutor<StockIndexSpot> {

    Optional<StockIndexSpot> findByCode(String code);

    List<StockIndexSpot> findByCodeIn(Collection<String> codes);

}
