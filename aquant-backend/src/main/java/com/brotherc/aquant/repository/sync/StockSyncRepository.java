package com.brotherc.aquant.repository.sync;

import com.brotherc.aquant.entity.sync.StockSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockSyncRepository extends JpaRepository<StockSync, Long> {

    StockSync findByName(String name);

}
