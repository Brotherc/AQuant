package com.brotherc.aquant.industry.repository;

import com.brotherc.aquant.industry.entity.StockIndustryBoardHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockIndustryBoardHistoryRepository extends JpaRepository<StockIndustryBoardHistory, Long>, JpaSpecificationExecutor<StockIndustryBoardHistory> {

    List<StockIndustryBoardHistory> findByTradeDateAndSectorNameIn(String tradeDate, List<String> sectorNames);

    List<StockIndustryBoardHistory> findBySectorName(String sectorName);

    StockIndustryBoardHistory findFirstBySectorNameAndTradeDateOrderByIdDesc(String sectorName, String tradeDate);

    List<StockIndustryBoardHistory> findBySectorNameOrderByTradeDateAsc(String sectorName);

    List<StockIndustryBoardHistory> findBySectorNameAndTradeDateBetweenOrderByTradeDateAsc(String sectorName, String startDate, String endDate);

    List<StockIndustryBoardHistory> findByTradeDateBetween(String startTradeDate, String endTradeDate);

    List<StockIndustryBoardHistory> findAllByOrderBySectorNameAscTradeDateAsc();

    List<StockIndustryBoardHistory> findByTradeDateBetweenOrderByTradeDateAscSectorNameAsc(String startTradeDate,
                                                                                           String endTradeDate);

    @Query("select h from StockIndustryBoardHistory h where h.tradeDate = (" +
            "select max(p.tradeDate) from StockIndustryBoardHistory p " +
            "where p.sectorName = h.sectorName and p.tradeDate < :startDate) " +
            "order by h.sectorName asc")
    List<StockIndustryBoardHistory> findLatestBeforeTradeDateForEachSector(@Param("startDate") String startDate);

    @Query("select count(h) from StockIndustryBoardHistory h " +
            "where h.changePercent is null and h.closePrice is not null and exists (" +
            "select p.id from StockIndustryBoardHistory p where p.sectorName = h.sectorName " +
            "and p.tradeDate < h.tradeDate and p.closePrice is not null)")
    long countCalculableMissingChangeMetrics();

    @Query("select max(s.tradeDate) from StockIndustryBoardHistory s")
    String findMaxTradeDate();

    @Query("select s.sectorName, max(s.tradeDate) from StockIndustryBoardHistory s " +
            "where s.sectorName in :sectorNames and s.tradeDate <= :endDate group by s.sectorName")
    List<Object[]> findMaxTradeDateBySectorNameInBeforeOrEqual(@Param("sectorNames") List<String> sectorNames,
                                                               @Param("endDate") String endDate);

}
