package com.brotherc.aquant.portfolio.repository;

import com.brotherc.aquant.portfolio.entity.UserPortfolioAccountSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserPortfolioAccountSnapshotRepository extends JpaRepository<UserPortfolioAccountSnapshot, Long> {

    Optional<UserPortfolioAccountSnapshot> findByAccountIdAndSnapshotDate(Long accountId, LocalDate snapshotDate);

    List<UserPortfolioAccountSnapshot> findAllByAccountIdInAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Collection<Long> accountIds, LocalDate startDate, LocalDate endDate);

    void deleteByAccountId(Long accountId);

}
