package com.brotherc.aquant.repository.fund;

import com.brotherc.aquant.entity.fund.StockFundPurchaseLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockFundPurchaseLimitRepository extends JpaRepository<StockFundPurchaseLimit, Long> {

    Optional<StockFundPurchaseLimit> findBySourceAndFundCodeAndSalesChannelAndBusinessType(
            String source, String fundCode, String salesChannel, String businessType
    );

    List<StockFundPurchaseLimit> findByBusinessTypeAndFundCodeIn(
            String businessType, Collection<String> fundCodes
    );

    List<StockFundPurchaseLimit> findByFundCodeOrderBySourceAscBusinessTypeAscSalesChannelAsc(String fundCode);

    boolean existsBySourceAndFundCodeAndBusinessType(String source, String fundCode, String businessType);

}
