package com.brotherc.aquant.industry.repository;

import com.brotherc.aquant.industry.entity.StockIndustryBoardHistoryEm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockIndustryBoardHistoryEmRepository extends JpaRepository<StockIndustryBoardHistoryEm, Long> {
    List<StockIndustryBoardHistoryEm> findByTradeDateBetweenOrderByTradeDateAscSectorNameAsc(String startDate, String endDate);
    List<StockIndustryBoardHistoryEm> findBySectorNameOrderByTradeDateAsc(String sectorName);
    StockIndustryBoardHistoryEm findBySectorNameAndTradeDate(String sectorName, String tradeDate);
    StockIndustryBoardHistoryEm findTopBySectorNameOrderByTradeDateDesc(String sectorName);

    /**
     * 查询每个板块在 startDate 之前最近一个交易日的行情（用于补算涨跌幅的昨收基准）。
     * 不能写成"外层全表 + 相关子查询求 max"的形式：该表含 2000 年至今全量 K 线（约 200 万行），
     * 相关子查询会对外层每行各做一次索引回溯，实测 25 秒仍无法完成并触发接口超时；
     * 改为先按板块分组求最大日期（走 (sector_name, trade_date) 索引松散扫描），再回表精确关联，
     * 实测约 60ms
     */
    @Query(value = "select h.* from stock_industry_board_history_em h " +
            "join (select sector_name, max(trade_date) as max_date from stock_industry_board_history_em " +
            "where trade_date < :startDate group by sector_name) p " +
            "on h.sector_name = p.sector_name and h.trade_date = p.max_date " +
            "order by h.sector_name asc", nativeQuery = true)
    List<StockIndustryBoardHistoryEm> findLatestBeforeTradeDateForEachSector(@Param("startDate") String startDate);
}
