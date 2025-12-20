package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockSync;
import com.brotherc.aquant.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockSyncService {

    private final StockSyncRepository stockSyncRepository;

    @Transactional(rollbackFor = Exception.class)
    public void save(StockSync stockSync, String name, Object value) {
        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName(name);
        }
        stockSync.setValue(value != null ? value.toString() : null);
        stockSyncRepository.save(stockSync);
    }

}
