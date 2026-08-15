package com.brotherc.aquant.repository.notification;

import com.brotherc.aquant.entity.notification.StockNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockNotificationRepository extends JpaRepository<StockNotification, Long> {

    List<StockNotification> findAllByUserIdAndStockCodeAndAssetType(Long userId, String stockCode, String assetType);

    List<StockNotification> findAllByIsEnabledAndAssetType(Integer isEnabled, String assetType);

    Optional<StockNotification> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT DISTINCT n.stockCode FROM StockNotification n WHERE n.userId = :userId AND n.assetType = :assetType AND n.stockCode IN :stockCodes")
    List<String> findDistinctStockCodeByUserIdAndAssetTypeAndStockCodeIn(
            @Param("userId") Long userId,
            @Param("assetType") String assetType,
            @Param("stockCodes") List<String> stockCodes
    );

    @Query("SELECT COUNT(DISTINCT n.stockCode) FROM StockNotification n WHERE n.assetType = :assetType")
    long countActiveStockCodes(@Param("assetType") String assetType);

    boolean existsByStockCodeAndAssetType(String stockCode, String assetType);

}
