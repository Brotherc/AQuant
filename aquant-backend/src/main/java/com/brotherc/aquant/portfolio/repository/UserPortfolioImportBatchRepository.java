package com.brotherc.aquant.portfolio.repository;

import com.brotherc.aquant.portfolio.entity.UserPortfolioImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserPortfolioImportBatchRepository extends JpaRepository<UserPortfolioImportBatch, Long> {

    Optional<UserPortfolioImportBatch> findByIdAndAccountId(Long id, Long accountId);

    List<UserPortfolioImportBatch> findAllByAccountIdOrderByCreateTimeDesc(Long accountId);

    List<UserPortfolioImportBatch> findAllByAccountIdInOrderByCreateTimeDesc(Collection<Long> accountIds);

    boolean existsByAccountIdAndSourceFileHashAndStatus(Long accountId, String sourceFileHash, String status);

}
