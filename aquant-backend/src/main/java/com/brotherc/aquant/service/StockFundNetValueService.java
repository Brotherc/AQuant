package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockFundNetValue;
import com.brotherc.aquant.model.dto.akshare.FundOpenFundInfoEm;
import com.brotherc.aquant.repository.StockFundNetValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockFundNetValueService {

    private final StockFundNetValueRepository stockFundNetValueRepository;

    public List<StockFundNetValue> getFundNetValues(String fundCode) {
        return stockFundNetValueRepository.findByFundCodeOrderByNavDateAsc(fundCode);
    }

    public Map<String, LocalDateTime> findMaxNavDateMap(List<String> fundCodes) {
        if (CollectionUtils.isEmpty(fundCodes)) {
            return Map.of();
        }

        List<Object[]> rows = stockFundNetValueRepository.findMaxNavDateByFundCodeIn(fundCodes);
        Map<String, LocalDateTime> result = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] instanceof LocalDateTime navDate) {
                result.put(String.valueOf(row[0]), navDate);
            }
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveFundNetValues(String fundCode, List<FundOpenFundInfoEm> fundNetValueList) {
        if (CollectionUtils.isEmpty(fundNetValueList)) {
            return;
        }

        Map<LocalDateTime, FundOpenFundInfoEm> dailyMap = new HashMap<>();
        for (FundOpenFundInfoEm item : fundNetValueList) {
            if (item == null || item.getNavDate() == null) {
                continue;
            }
            try {
                dailyMap.put(LocalDateTime.parse(item.getNavDate()), item);
            } catch (Exception e) {
                log.warn("基金净值日期解析失败，fundCode={}, navDate={}", fundCode, item.getNavDate());
            }
        }
        if (dailyMap.isEmpty()) {
            return;
        }

        List<LocalDateTime> navDates = new ArrayList<>(dailyMap.keySet());
        List<StockFundNetValue> existedList = stockFundNetValueRepository.findByFundCodeAndNavDateIn(fundCode, navDates);
        Map<LocalDateTime, StockFundNetValue> existedMap = new HashMap<>();
        for (StockFundNetValue existed : existedList) {
            if (existed.getNavDate() != null) {
                existedMap.put(existed.getNavDate(), existed);
            }
        }

        List<StockFundNetValue> saveList = new ArrayList<>(dailyMap.size());
        for (Map.Entry<LocalDateTime, FundOpenFundInfoEm> entry : dailyMap.entrySet()) {
            LocalDateTime navDate = entry.getKey();
            FundOpenFundInfoEm dto = entry.getValue();
            StockFundNetValue entity = existedMap.get(navDate);
            if (entity == null) {
                entity = new StockFundNetValue();
                entity.setFundCode(fundCode);
                entity.setNavDate(navDate);
            }
            entity.setUnitNav(dto.getUnitNav());
            entity.setDailyGrowthRate(dto.getDailyGrowthRate());
            saveList.add(entity);
        }

        stockFundNetValueRepository.saveAll(saveList);
    }

}
