package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockNotificationRepository extends JpaRepository<StockNotification, Long> {

    List<StockNotification> findAllByUserId(Long userId);

    List<StockNotification> findAllByUserIdAndStockCode(Long userId, String stockCode);

    List<StockNotification> findAllByStockCodeAndIsEnabled(String stockCode, Integer isEnabled);

    List<StockNotification> findAllByIsEnabled(Integer isEnabled);

    Optional<StockNotification> findByIdAndUserId(Long id, Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT n.stockCode FROM StockNotification n WHERE n.isEnabled = 1")
    List<String> findActiveStockCodes();

}
