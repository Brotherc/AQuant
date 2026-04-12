package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("SELECT DISTINCT n.stockCode FROM StockNotification n WHERE n.userId = :userId AND n.stockCode IN :stockCodes")
    List<String> findDistinctStockCodeByUserIdAndStockCodeIn(Long userId, List<String> stockCodes);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT n.stockCode FROM StockNotification n WHERE n.isEnabled = 1")
    List<String> findActiveStockCodes();

    @Query("SELECT COUNT(DISTINCT n.stockCode) FROM StockNotification n")
    long countActiveStockCodes();

    boolean existsByStockCode(String stockCode);

}
