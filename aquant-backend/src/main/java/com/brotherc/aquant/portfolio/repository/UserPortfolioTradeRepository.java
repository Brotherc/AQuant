package com.brotherc.aquant.portfolio.repository;

import com.brotherc.aquant.portfolio.entity.UserPortfolioTrade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserPortfolioTradeRepository extends JpaRepository<UserPortfolioTrade, Long> {

    Page<UserPortfolioTrade> findAllByAccountIdOrderByTradeTimeDescIdDesc(Long accountId, Pageable pageable);

    Page<UserPortfolioTrade> findAllByAccountIdInOrderByTradeTimeDescIdDesc(Collection<Long> accountIds, Pageable pageable);

    List<UserPortfolioTrade> findAllByAccountIdAndStatusOrderByTradeTimeAscIdAsc(Long accountId, String status);

    List<UserPortfolioTrade> findAllByAccountIdInAndAssetCodeInAndStatusOrderByTradeTimeAscIdAsc(
            Collection<Long> accountIds, Collection<String> assetCodes, String status);

    List<UserPortfolioTrade> findAllByImportBatchIdAndAccountId(Long importBatchId, Long accountId);

    Optional<UserPortfolioTrade> findByIdAndAccountId(Long id, Long accountId);

    boolean existsByAccountIdAndDedupKey(Long accountId, String dedupKey);

    void deleteByAccountId(Long accountId);

}
