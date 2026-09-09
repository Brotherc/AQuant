package com.brotherc.aquant.portfolio.repository;

import com.brotherc.aquant.portfolio.entity.UserPortfolioCash;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserPortfolioCashRepository extends JpaRepository<UserPortfolioCash, Long> {

    List<UserPortfolioCash> findAllByAccountIdIn(Collection<Long> accountIds);

    List<UserPortfolioCash> findAllByAccountIdOrderByCurrencyAsc(Long accountId);

    Optional<UserPortfolioCash> findByAccountIdAndCurrency(Long accountId, String currency);
}
