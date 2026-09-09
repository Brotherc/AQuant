package com.brotherc.aquant.portfolio.repository;

import com.brotherc.aquant.portfolio.entity.UserBrokerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserBrokerAccountRepository extends JpaRepository<UserBrokerAccount, Long> {

    List<UserBrokerAccount> findAllByPortfolioIdAndUserIdAndDeletedFalseOrderByCreateTimeAsc(Long portfolioId, Long userId);

    List<UserBrokerAccount> findAllByPortfolioIdInAndUserIdAndDeletedFalse(Collection<Long> portfolioIds, Long userId);

    Optional<UserBrokerAccount> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    long countByPortfolioIdAndUserIdAndDeletedFalse(Long portfolioId, Long userId);

    boolean existsByUserIdAndBrokerCodeAndAccountNoHashAndDeletedFalse(Long userId, String brokerCode, String accountNoHash);
}
