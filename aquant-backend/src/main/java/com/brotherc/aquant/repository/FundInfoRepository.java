package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.FundInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundInfoRepository extends JpaRepository<FundInfo, Long> {
}