package com.brotherc.aquant.service.index;

import com.brotherc.aquant.entity.index.StockIndexHistory;
import com.brotherc.aquant.entity.index.StockIndexSpot;
import com.brotherc.aquant.enums.CoreIndexEnum;
import com.brotherc.aquant.model.dto.akshare.StockZhIndexDaily;
import com.brotherc.aquant.model.dto.akshare.StockZhIndexSpotSina;
import com.brotherc.aquant.model.vo.index.StockIndexCardVO;
import com.brotherc.aquant.repository.index.StockIndexHistoryRepository;
import com.brotherc.aquant.repository.index.StockIndexSpotRepository;
import com.brotherc.aquant.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.brotherc.aquant.entity.stock.StockQuoteHistory;
import com.brotherc.aquant.utils.StockUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockIndexService {

    private static final int SPARKLINE_HISTORY_COUNT = 15;
    private static final List<String> TARGET_INDEX_CODES = List.of(
            "sh000001", "sz399001", "sz399006", "sh000688", "sh000300", "sh000905"
    );

    private final StockIndexSpotRepository stockIndexSpotRepository;
    private final StockIndexHistoryRepository stockIndexHistoryRepository;

    /**
     * 保存/更新指数实时行情快照 (仅针对 stock_index_spot 表)
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveIndexSpot(List<StockZhIndexSpotSina> spotList, LocalDateTime now) {
        if (spotList == null || spotList.isEmpty()) {
            return;
        }

        Map<String, StockIndexSpot> existingMap = stockIndexSpotRepository.findAll().stream()
                .collect(HashMap::new, (map, item) -> map.put(item.getCode(), item), Map::putAll);

        List<StockIndexSpot> toSaveSpot = new ArrayList<>();

        for (StockZhIndexSpotSina item : spotList) {
            if (StringUtils.isBlank(item.getCode())) {
                continue;
            }

            StockIndexSpot spot = existingMap.getOrDefault(item.getCode(), new StockIndexSpot());
            spot.setCode(item.getCode());
            spot.setName(item.getName());
            spot.setLatestPrice(item.getLatestPrice());
            spot.setChangeAmount(item.getChangeAmount());
            spot.setChangePercent(item.getChangePercent());
            spot.setPrevClose(item.getPrevClose());
            spot.setOpenPrice(item.getOpenPrice());
            spot.setHighPrice(item.getHighPrice());
            spot.setLowPrice(item.getLowPrice());
            spot.setVolume(item.getVolume());
            spot.setTurnover(item.getTurnover());
            spot.setCreatedAt(now);

            toSaveSpot.add(spot);
        }

        if (!toSaveSpot.isEmpty()) {
            stockIndexSpotRepository.saveAll(toSaveSpot);
        }
    }

    /**
     * 根据实时数据补充/更新指定重要指数当日的 StockIndexHistory 记录 (包含成交额 turnover)
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTodayHistoryFromSpot(List<StockZhIndexSpotSina> spotList, Set<String> targetCodes, LocalDateTime now) {
        if (spotList == null || spotList.isEmpty() || targetCodes == null || targetCodes.isEmpty()) {
            return;
        }

        LocalDate today = now.toLocalDate();
        for (StockZhIndexSpotSina item : spotList) {
            if (StringUtils.isNotBlank(item.getCode()) && item.getLatestPrice() != null && targetCodes.contains(item.getCode())) {
                StockIndexHistory history = stockIndexHistoryRepository
                        .findByIndexCodeAndTradeDate(item.getCode(), today)
                        .orElseGet(() -> {
                            StockIndexHistory h = new StockIndexHistory();
                            h.setIndexCode(item.getCode());
                            h.setIndexName(item.getName());
                            h.setTradeDate(today);
                            return h;
                        });

                history.setOpenPrice(item.getOpenPrice());
                history.setHighPrice(item.getHighPrice());
                history.setLowPrice(item.getLowPrice());
                history.setClosePrice(item.getLatestPrice());
                history.setVolume(item.getVolume());
                history.setTurnover(item.getTurnover());
                history.setCreatedAt(now);

                stockIndexHistoryRepository.save(history);
            }
        }
    }

    /**
     * 增量保存指数历史日 K 线数据 (幂等防断层校验)
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveIndexHistory(String indexCode, String indexName, List<StockZhIndexDaily> dailyList, LocalDateTime now) {
        if (dailyList == null || dailyList.isEmpty()) {
            return;
        }

        Set<LocalDate> existingDates = stockIndexHistoryRepository.findByIndexCodeOrderByTradeDateDesc(indexCode).stream()
                .map(StockIndexHistory::getTradeDate)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        List<StockIndexHistory> toSave = new ArrayList<>();
        for (StockZhIndexDaily daily : dailyList) {
            if (StringUtils.isBlank(daily.getDate())) {
                continue;
            }

            LocalDate tradeDate = DateUtils.parseLocalDate(daily.getDate());
            if (tradeDate == null || existingDates.contains(tradeDate)) {
                continue;
            }

            StockIndexHistory history = new StockIndexHistory();
            history.setIndexCode(indexCode);
            history.setIndexName(indexName);
            history.setTradeDate(tradeDate);
            history.setOpenPrice(daily.getOpen());
            history.setHighPrice(daily.getHigh());
            history.setLowPrice(daily.getLow());
            history.setClosePrice(daily.getClose());
            history.setVolume(daily.getVolume());
            history.setCreatedAt(now);

            toSave.add(history);
        }

        if (!toSave.isEmpty()) {
            stockIndexHistoryRepository.saveAll(toSave);
            log.info("指数 [{}] 增量补全写入历史日 K 线 {} 条", indexName, toSave.size());
        }
    }

    /**
     * 查询首页核心大盘指数卡片数据 (包含实时行情及历史迷你趋势线)
     */
    public List<StockIndexCardVO> getCoreIndexCards() {
        Map<String, StockIndexSpot> spotMap = stockIndexSpotRepository.findByCodeIn(TARGET_INDEX_CODES).stream()
                .collect(Collectors.toMap(StockIndexSpot::getCode, spot -> spot, (a, b) -> a));

        List<StockIndexCardVO> result = new ArrayList<>();
        for (String code : TARGET_INDEX_CODES) {
            String name = CoreIndexEnum.getNameByCode(code);
            StockIndexSpot spot = spotMap.get(code);

            // 查询该指数近期指定条数的收盘价序列
            List<StockIndexHistory> historyList = stockIndexHistoryRepository.findByIndexCodeOrderByTradeDateDesc(
                    code,
                    PageRequest.of(0, SPARKLINE_HISTORY_COUNT)
            );
            List<BigDecimal> historyPrices = historyList.stream()
                    .map(StockIndexHistory::getClosePrice)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 翻转使得序列按时间正序排列 (左旧右新)
            Collections.reverse(historyPrices);

            StockIndexCardVO vo = new StockIndexCardVO();
            vo.setCode(code);
            vo.setName(name);
            vo.setHistoryPrices(historyPrices);

            if (spot != null) {
                vo.setLatestPrice(spot.getLatestPrice());
                vo.setChangeAmount(spot.getChangeAmount());
                vo.setChangePercent(spot.getChangePercent());
                vo.setOpenPrice(spot.getOpenPrice());
                vo.setHighPrice(spot.getHighPrice());
                vo.setLowPrice(spot.getLowPrice());
                vo.setPrevClose(spot.getPrevClose());
                vo.setVolume(spot.getVolume());
                vo.setTurnover(spot.getTurnover());
            } else if (!historyList.isEmpty()) {
                StockIndexHistory latestHist = historyList.get(0);
                vo.setLatestPrice(latestHist.getClosePrice());
                vo.setOpenPrice(latestHist.getOpenPrice());
                vo.setHighPrice(latestHist.getHighPrice());
                vo.setLowPrice(latestHist.getLowPrice());
                vo.setVolume(latestHist.getVolume());
                vo.setTurnover(latestHist.getTurnover());
                if (historyList.size() > 1 && latestHist.getClosePrice() != null) {
                    BigDecimal prevClose = historyList.get(1).getClosePrice();
                    if (prevClose != null && prevClose.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal diff = latestHist.getClosePrice().subtract(prevClose);
                        BigDecimal pct = diff.divide(prevClose, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                        vo.setChangeAmount(diff);
                        vo.setChangePercent(pct);
                        vo.setPrevClose(prevClose);
                    }
                }
            }

            result.add(vo);
        }

        return result;
    }

    /**
     * 获取大盘指数历史 K 线数据 (支持周期聚合)
     *
     * @param code      指数代码 (如 sh000001, sz399001)
     * @param frequency 频率: 1d, 1w, 1M, 1Q, 1Y
     */
    public List<StockQuoteHistory> getIndexKlineHistory(String code, String frequency) {
        if (StringUtils.isBlank(code)) {
            return Collections.emptyList();
        }

        String rawCode = code;
        String wrappedCode = StockUtils.wrapExchangePrefix(code);
        List<StockIndexHistory> indexHistories = stockIndexHistoryRepository.findByIndexCodeOrderByTradeDateAsc(rawCode);
        if (CollectionUtils.isEmpty(indexHistories) && !rawCode.equals(wrappedCode)) {
            indexHistories = stockIndexHistoryRepository.findByIndexCodeOrderByTradeDateAsc(wrappedCode);
        }
        if (CollectionUtils.isEmpty(indexHistories)) {
            String cleanCode = rawCode.replaceAll("^(sh|sz|bj)", "");
            indexHistories = stockIndexHistoryRepository.findByIndexCodeOrderByTradeDateAsc(cleanCode);
        }

        if (CollectionUtils.isEmpty(indexHistories)) {
            return Collections.emptyList();
        }

        // 转换为通用的 K 线模型 List<StockQuoteHistory>
        List<StockQuoteHistory> dailyList = indexHistories.stream().map(h -> {
            StockQuoteHistory sqh = new StockQuoteHistory();
            sqh.setId(h.getId());
            sqh.setCode(h.getIndexCode());
            sqh.setName(h.getIndexName());
            sqh.setTradeDate(h.getTradeDate() != null ? h.getTradeDate().toString() : "");
            sqh.setOpenPrice(h.getOpenPrice());
            sqh.setClosePrice(h.getClosePrice());
            sqh.setHighPrice(h.getHighPrice());
            sqh.setLowPrice(h.getLowPrice());
            sqh.setVolume(h.getVolume());
            sqh.setTurnover(h.getTurnover());
            return sqh;
        }).collect(Collectors.toList());

        if ("1d".equals(frequency)) {
            return dailyList;
        }

        // 周期聚合逻辑 (1w, 1M, 1Q, 1Y)
        Map<String, List<StockQuoteHistory>> groupedMap = new LinkedHashMap<>();
        for (StockQuoteHistory item : dailyList) {
            String dateStr = item.getTradeDate();
            if (StringUtils.isBlank(dateStr)) continue;
            LocalDate date = LocalDate.parse(dateStr);
            String key;
            switch (frequency) {
                case "1w" -> key = date.getYear() + "-W" + date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                case "1M" -> key = date.getYear() + "-" + date.getMonthValue();
                case "1Q" -> key = date.getYear() + "-Q" + date.get(IsoFields.QUARTER_OF_YEAR);
                case "1Y" -> key = String.valueOf(date.getYear());
                default -> key = dateStr;
            }
            groupedMap.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        List<StockQuoteHistory> result = new ArrayList<>();
        for (List<StockQuoteHistory> group : groupedMap.values()) {
            if (group.isEmpty()) continue;
            StockQuoteHistory first = group.get(0);
            StockQuoteHistory last = group.get(group.size() - 1);

            StockQuoteHistory aggregated = new StockQuoteHistory();
            aggregated.setCode(first.getCode());
            aggregated.setName(first.getName());
            aggregated.setTradeDate(last.getTradeDate());

            aggregated.setOpenPrice(first.getOpenPrice());
            aggregated.setClosePrice(last.getClosePrice());
            aggregated.setHighPrice(group.stream()
                    .map(StockQuoteHistory::getHighPrice)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO));
            aggregated.setLowPrice(group.stream()
                    .map(StockQuoteHistory::getLowPrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO));

            aggregated.setVolume(group.stream()
                    .map(StockQuoteHistory::getVolume)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            aggregated.setTurnover(group.stream()
                    .map(StockQuoteHistory::getTurnover)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            result.add(aggregated);
        }

        return result;
    }

}
