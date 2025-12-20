package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockSyncRepository extends JpaRepository<StockSync, Long> {

    StockSync findByName(String name);

}
