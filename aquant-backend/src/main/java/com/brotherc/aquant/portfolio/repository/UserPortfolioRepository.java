package com.brotherc.aquant.portfolio.repository;

import com.brotherc.aquant.portfolio.entity.UserPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPortfolioRepository extends JpaRepository<UserPortfolio, Long> {

    List<UserPortfolio> findAllByUserIdAndDeletedFalseOrderByDefaultPortfolioDescCreateTimeAsc(Long userId);

    List<UserPortfolio> findAllByDeletedFalseOrderByIdAsc();

    Optional<UserPortfolio> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    boolean existsByUserIdAndNameAndDeletedFalse(Long userId, String name);
}
