package com.brotherc.aquant.portfolio.repository;

import com.brotherc.aquant.portfolio.entity.UserPortfolioPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserPortfolioPositionRepository extends JpaRepository<UserPortfolioPosition, Long> {

    List<UserPortfolioPosition> findAllByAccountIdOrderByMarketValueDesc(Long accountId);

    List<UserPortfolioPosition> findAllByAccountIdInOrderByMarketValueDesc(Collection<Long> accountIds);

    long deleteByAccountId(Long accountId);
}
