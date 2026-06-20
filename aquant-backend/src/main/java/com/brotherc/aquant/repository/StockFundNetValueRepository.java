package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.StockFundNetValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockFundNetValueRepository extends JpaRepository<StockFundNetValue, Long> {

    List<StockFundNetValue> findByFundCodeOrderByNavDateAsc(String fundCode);

    List<StockFundNetValue> findByFundCodeAndNavDateIn(String fundCode, List<LocalDateTime> navDates);

    @Query("select s.fundCode, max(s.navDate) from StockFundNetValue s where s.fundCode in :fundCodes group by s.fundCode")
    List<Object[]> findMaxNavDateByFundCodeIn(@Param("fundCodes") List<String> fundCodes);

}
