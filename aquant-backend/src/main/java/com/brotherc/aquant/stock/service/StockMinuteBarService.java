package com.brotherc.aquant.stock.service;

import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.integration.akshare.model.StockZhAMinute;
import com.brotherc.aquant.stock.entity.StockMinuteBar;
import com.brotherc.aquant.stock.repository.StockMinuteBarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分钟 K 线落库与读取（闸②：只收已收盘完整日；闸③：唯一键 + 查-比-存）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockMinuteBarService {

    private static final int MINUTE_PERIOD = 1;

    /**
     * 判定完整日的最小 bar 数：高流动性股票满量 238，低流动性股票新浪会省略零成交分钟（实测 229+）；
     * 窗口截断首日仅 66 根，远低于该值
     */
    private static final int MIN_COMPLETE_DAY_BAR_COUNT = 200;

    /**
     * 完整日首根 bar 的最晚时间，用于识别 1970 行窗口截断出的半日（首根从盘中开始）
     */
    private static final String DAY_START_CUTOFF = "09:35";

    private final StockMinuteBarRepository stockMinuteBarRepository;
    private final StockHelper stockHelper;

    /**
     * 幂等落库。上游 HTTP 拉取须在本方法之外完成，避免长事务占用连接池。
     *
     * @return 本次实际落库的最大交易日（yyyy-MM-dd），无完整日返回 null
     */
    @Transactional(rollbackFor = Exception.class)
    public String save(List<StockZhAMinute> rows, String code, LocalDateTime now) {
        if (CollectionUtils.isEmpty(rows)) {
            return null;
        }

        String latestClosedDay = stockHelper.latestClosedTradeDay(now).toString();

        // 闸②：按日分组，丢弃 1970 行窗口截断的首日、当日盘中半日与过稀疏日
        Map<String, List<StockZhAMinute>> dayMap = new LinkedHashMap<>();
        for (StockZhAMinute row : rows) {
            String day = row.getDay();
            if (day == null || day.length() < 19) {
                continue;
            }
            String tradeDate = day.substring(0, 10);
            if (tradeDate.compareTo(latestClosedDay) > 0) {
                continue;
            }
            dayMap.computeIfAbsent(tradeDate, k -> new ArrayList<>()).add(row);
        }

        List<String> completeDays = new ArrayList<>();
        for (Map.Entry<String, List<StockZhAMinute>> entry : dayMap.entrySet()) {
            List<StockZhAMinute> dayRows = entry.getValue();
            if (dayRows.size() >= MIN_COMPLETE_DAY_BAR_COUNT
                    && dayRows.get(0).getDay().substring(11, 16).compareTo(DAY_START_CUTOFF) <= 0) {
                completeDays.add(entry.getKey());
            }
        }
        if (completeDays.isEmpty()) {
            log.info("分钟行情无完整收盘日，跳过落库, code={}, latestClosedDay={}", code, latestClosedDay);
            return null;
        }

        // 闸③：按 barTime 去重（后到覆盖先到）
        Map<String, StockMinuteBar> barMap = new LinkedHashMap<>();
        for (String tradeDate : completeDays) {
            for (StockZhAMinute row : dayMap.get(tradeDate)) {
                StockMinuteBar bar = new StockMinuteBar();
                bar.setCode(code);
                bar.setBarTime(row.getDay());
                bar.setPeriod(MINUTE_PERIOD);
                bar.setOpenPrice(row.getOpen());
                bar.setHighPrice(row.getHigh());
                bar.setLowPrice(row.getLow());
                bar.setClosePrice(row.getClose());
                bar.setVolume(row.getVolume());
                bar.setTurnover(row.getAmount());
                bar.setCreatedAt(now);
                barMap.put(row.getDay(), bar);
            }
        }

        // 查：仅查本批窗口内的已有数据，合并历史脏重复后按存在性 upsert
        String minBarTime = barMap.keySet().stream().min(String::compareTo).orElse(null);
        List<StockMinuteBar> existedList = stockMinuteBarRepository
                .findByCodeAndPeriodAndBarTimeGreaterThanEqualOrderByBarTimeAsc(code, MINUTE_PERIOD, minBarTime);
        Map<String, StockMinuteBar> existedMap = new LinkedHashMap<>();
        List<Long> duplicateIds = new ArrayList<>();
        for (StockMinuteBar existed : existedList) {
            mergeExisting(existedMap, duplicateIds, existed.getBarTime(), existed);
        }
        deleteDuplicateBars(duplicateIds);

        List<StockMinuteBar> saveList = new ArrayList<>(barMap.size());
        for (Map.Entry<String, StockMinuteBar> entry : barMap.entrySet()) {
            StockMinuteBar existed = existedMap.get(entry.getKey());
            StockMinuteBar bar = entry.getValue();
            if (existed != null) {
                bar.setId(existed.getId());
            }
            saveList.add(bar);
        }

        stockMinuteBarRepository.saveAll(saveList);

        return completeDays.get(completeDays.size() - 1);
    }

    /**
     * 读取该股票最近 N 个有数据交易日的分钟 K（停牌/数据缺失日自动跳过，向前补足）
     */
    public List<StockMinuteBar> findRecentBars(String code, int days) {
        List<String> recentDates = stockMinuteBarRepository.findRecentTradeDates(code, MINUTE_PERIOD, days);
        if (recentDates.isEmpty()) {
            return List.of();
        }
        String earliest = recentDates.get(recentDates.size() - 1);
        return stockMinuteBarRepository
                .findByCodeAndPeriodAndBarTimeGreaterThanEqualOrderByBarTimeAsc(code, MINUTE_PERIOD, earliest + " 00:00:00");
    }

    private void mergeExisting(
            Map<String, StockMinuteBar> existedMap,
            List<Long> duplicateIds,
            String key,
            StockMinuteBar candidate
    ) {
        if (key == null || candidate == null) {
            return;
        }

        StockMinuteBar current = existedMap.get(key);
        if (current == null) {
            existedMap.put(key, candidate);
            return;
        }

        StockMinuteBar keep = chooseLatest(current, candidate);
        StockMinuteBar duplicate = keep == current ? candidate : current;
        if (duplicate.getId() != null) {
            duplicateIds.add(duplicate.getId());
        }
        existedMap.put(key, keep);
    }

    private StockMinuteBar chooseLatest(StockMinuteBar current, StockMinuteBar candidate) {
        Long currentId = current.getId();
        Long candidateId = candidate.getId();
        if (currentId == null) {
            return candidate;
        }
        if (candidateId == null) {
            return current;
        }
        return candidateId > currentId ? candidate : current;
    }

    private void deleteDuplicateBars(List<Long> duplicateIds) {
        if (!CollectionUtils.isEmpty(duplicateIds)) {
            stockMinuteBarRepository.deleteAllByIdInBatch(duplicateIds);
        }
    }
}
