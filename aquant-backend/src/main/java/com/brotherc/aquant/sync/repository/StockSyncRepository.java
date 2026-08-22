package com.brotherc.aquant.sync.repository;

import com.brotherc.aquant.sync.entity.StockSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockSyncRepository extends JpaRepository<StockSync, Long> {

    StockSync findByName(String name);

    List<StockSync> findAllByNameOrderByIdDesc(String name);

}
