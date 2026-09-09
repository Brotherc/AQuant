package com.brotherc.aquant.portfolio.repository;

import com.brotherc.aquant.portfolio.entity.UserPortfolioPositionSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPortfolioPositionSnapshotRepository extends JpaRepository<UserPortfolioPositionSnapshot, Long> {

    long deleteByAccountSnapshotId(Long accountSnapshotId);
}
