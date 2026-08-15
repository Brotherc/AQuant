package com.brotherc.aquant.repository.fund;

import com.brotherc.aquant.entity.fund.StockFundNetValue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import java.util.Collection;

@Repository
public interface StockFundNetValueRepository extends JpaRepository<StockFundNetValue, Long> {

    List<StockFundNetValue> findByFundCodeOrderByNavDateAsc(String fundCode);

    List<StockFundNetValue> findByFundCodeInOrderByNavDateDesc(Collection<String> fundCodes);

    List<StockFundNetValue> findByFundCodeAndNavDateIn(String fundCode, List<LocalDateTime> navDates);

    @Query("select s from StockFundNetValue s where s.fundCode = :fundCode order by s.navDate desc")
    List<StockFundNetValue> findLatestByFundCode(@Param("fundCode") String fundCode, Pageable pageable);

    @Query("select s.fundCode, max(s.navDate) from StockFundNetValue s where s.fundCode in :fundCodes group by s.fundCode")
    List<Object[]> findMaxNavDateByFundCodeIn(@Param("fundCodes") List<String> fundCodes);

}
