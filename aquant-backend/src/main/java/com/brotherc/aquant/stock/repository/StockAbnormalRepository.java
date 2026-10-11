package com.brotherc.aquant.stock.repository;

import com.brotherc.aquant.stock.entity.StockAbnormal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface StockAbnormalRepository extends JpaRepository<StockAbnormal, Long> {

    @Query("select s.code from StockAbnormal s")
    List<String> findAllCodes();

    @Query("select s.code from StockAbnormal s where s.code in :codes")
    List<String> findCodesByCodeIn(@Param("codes") Collection<String> codes);

}
