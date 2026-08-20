package com.brotherc.aquant.fund.repository;

import com.brotherc.aquant.fund.entity.StockFundAnnouncementSync;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockFundAnnouncementSyncRepository extends JpaRepository<StockFundAnnouncementSync, Long> {

    Optional<StockFundAnnouncementSync> findBySourceAndAnnouncementId(String source, String announcementId);

    List<StockFundAnnouncementSync> findBySourceAndAnnouncementIdIn(
            String source, Collection<String> announcementIds
    );

    List<StockFundAnnouncementSync> findBySourceAndStatusInOrderByAnnouncementDateDesc(
            String source, Collection<String> statuses
    );

    Optional<StockFundAnnouncementSync> findTopBySourceAndStatusOrderByAnnouncementDateDesc(
            String source, String status
    );

}
