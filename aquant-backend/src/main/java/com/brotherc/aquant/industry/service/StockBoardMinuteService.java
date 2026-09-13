package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.industry.entity.StockIndustryBoardEm;
import com.brotherc.aquant.industry.model.vo.StockBoardTrendsVO;
import com.brotherc.aquant.industry.repository.StockIndustryBoardEmRepository;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardTrends;
import com.brotherc.aquant.integration.eastmoney.service.EastmoneyBoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 板块分时/五日走势（东财直连 + 10 秒 TTL 缓存，不落库）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockBoardMinuteService {

    private static final long TTL_MILLIS = 10 * 1000L;

    private final EastmoneyBoardService eastmoneyBoardService;
    private final StockIndustryBoardEmRepository boardRepository;

    private final ConcurrentHashMap<String, CacheEntry<StockBoardTrendsVO>> cache = new ConcurrentHashMap<>();

    /**
     * 当日分时
     *
     * @param boardCode 库内板块代码（即 sectorName，如"院线"）
     */
    public StockBoardTrendsVO getIntradayTrends(String boardCode) {
        return getTrends(boardCode, 1);
    }

    /**
     * 五日分时
     */
    public StockBoardTrendsVO getTrends5d(String boardCode) {
        return getTrends(boardCode, 5);
    }

    private StockBoardTrendsVO getTrends(String boardCode, int days) {
        String cacheKey = boardCode + ":" + days;
        CacheEntry<StockBoardTrendsVO> cached = cache.get(cacheKey);
        if (cached != null && cached.expireAt > System.currentTimeMillis()) {
            return cached.value;
        }
        StockIndustryBoardEm board = boardRepository.findBySectorName(boardCode);
        if (board == null || board.getSectorCode() == null || board.getSectorCode().isBlank()) {
            throw new IllegalArgumentException("未知板块: " + boardCode);
        }
        EastmoneyBoardTrends trends = eastmoneyBoardService.fetchBoardTrends(board.getSectorCode(), days);
        StockBoardTrendsVO vo = buildVO(boardCode, trends);
        cache.put(cacheKey, new CacheEntry<>(vo, System.currentTimeMillis() + TTL_MILLIS));
        return vo;
    }

    private StockBoardTrendsVO buildVO(String boardCode, EastmoneyBoardTrends trends) {
        StockBoardTrendsVO vo = new StockBoardTrendsVO();
        vo.setBoardCode(boardCode);
        vo.setName(trends.getName());
        vo.setPrevClose(trends.getPreClose());
        for (EastmoneyBoardTrends.Trend trend : trends.getTrends()) {
            StockBoardTrendsVO.Point point = new StockBoardTrendsVO.Point();
            point.setTime(trend.getTime());
            point.setPrice(trend.getPrice());
            point.setAvgPrice(trend.getAvgPrice());
            point.setVolume(trend.getVolume());
            vo.getPoints().add(point);
        }
        if (!vo.getPoints().isEmpty()) {
            vo.setLatestPrice(vo.getPoints().get(vo.getPoints().size() - 1).getPrice());
        }
        return vo;
    }

    private static final class CacheEntry<T> {
        private final T value;
        private final long expireAt;

        private CacheEntry(T value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
