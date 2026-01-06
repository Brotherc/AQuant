package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockIndustryBoardHistory;
import com.brotherc.aquant.model.dto.akshare.StockBoardIndustrySpotEm;
import com.brotherc.aquant.repository.StockIndustryBoardHistoryRepository;
import com.brotherc.aquant.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockIndustryBoardHistoryService {

    private final StockIndustryBoardHistoryRepository stockIndustryBoardHistoryRepository;

    @Transactional(rollbackFor = Exception.class)
    public void save(Map<String, StockBoardIndustrySpotEm> stockBoardDetailMap, LocalDateTime now) {
        // 获取最新的交易日期
        String tradeDate = StockUtils.latestTradeDayFallback(LocalDate.now()).toString();

        // 提取所有股票代码
        List<String> codes = stockBoardDetailMap.keySet().stream().map(o -> o.split(":")[0]).toList();

        // 一次性查出该交易日期已存在的数据
        List<StockIndustryBoardHistory> existedList = stockIndustryBoardHistoryRepository.findByTradeDateAndBoardCodeIn(tradeDate, codes);

        // 构建 Map：code -> entity
        Map<String, StockIndustryBoardHistory> existedMap = existedList.stream()
                .collect(Collectors.toMap(StockIndustryBoardHistory::getBoardCode, e -> e));

        List<StockIndustryBoardHistory> saveList = new ArrayList<>(stockBoardDetailMap.size());

        // 遍历实时行情
        for (Map.Entry<String, StockBoardIndustrySpotEm> entry : stockBoardDetailMap.entrySet()) {
            String[] split = entry.getKey().split(":");
            String boardCode = split[0];
            String boardName = split[1];

            StockBoardIndustrySpotEm value = entry.getValue();

            StockIndustryBoardHistory entity = existedMap.get(boardCode);

            if (entity == null) {
                entity = new StockIndustryBoardHistory();
                entity.setBoardCode(boardCode);
                entity.setTradeDate(tradeDate);
            }

            // 行情更新
            entity.setBoardName(boardName);
            entity.setOpenPrice(value.getOpenPrice());
            entity.setHighPrice(value.getHighPrice());
            entity.setLowPrice(value.getLowPrice());
            entity.setLatestPrice(value.getLatestPrice());
            entity.setChangeAmount(value.getChangeAmount());
            entity.setChangePercent(value.getChangePercent());
            entity.setAmplitude(value.getAmplitude());
            entity.setVolume(value.getVolume());
            entity.setTurnoverAmount(value.getTurnover());
            entity.setTurnoverRate(value.getTurnoverRate());
            entity.setCreatedAt(now);

            saveList.add(entity);
        }

        // 批量保存
        stockIndustryBoardHistoryRepository.saveAll(saveList);
    }

}
