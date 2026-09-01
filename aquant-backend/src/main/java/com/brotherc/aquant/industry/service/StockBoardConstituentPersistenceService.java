package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.industry.entity.StockBoardConstituent;
import com.brotherc.aquant.industry.repository.StockBoardConstituentRepository;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryConsThs;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockBoardConstituentPersistenceService {

    private final StockBoardConstituentRepository stockBoardConstituentRepository;
    private final StockSyncRepository stockSyncRepository;

    @Transactional(rollbackFor = Exception.class)
    public void replace(String industry, List<StockBoardIndustryConsThs> sourceList, long completedTradeDayWatermark) {
        if (CollectionUtils.isEmpty(sourceList)) {
            throw new IllegalStateException("行业成分股上游返回为空");
        }

        Map<String, StockBoardIndustryConsThs> sourceByCode = sourceList.stream()
                .filter(item -> item != null && item.getStockCode() != null && !item.getStockCode().isBlank())
                .filter(item -> item.getStockName() != null && !item.getStockName().isBlank())
                .collect(Collectors.toMap(
                        StockBoardIndustryConsThs::getStockCode,
                        item -> item,
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
        if (sourceByCode.isEmpty()) {
            throw new IllegalStateException("行业成分股上游未包含有效股票代码");
        }

        Map<String, StockBoardConstituent> existingByCode = stockBoardConstituentRepository
                .findByBoardCodeOrderByStockCodeAsc(industry)
                .stream()
                .collect(Collectors.toMap(
                        StockBoardConstituent::getStockCode,
                        item -> item,
                        (first, second) -> second
                ));
        LocalDateTime updatedAt = LocalDateTime.now();
        List<StockBoardConstituent> members = sourceByCode.values().stream()
                .map(source -> {
                    StockBoardConstituent member = existingByCode.get(source.getStockCode());
                    if (member == null) {
                        member = new StockBoardConstituent();
                        member.setBoardCode(industry);
                        member.setStockCode(source.getStockCode());
                    }
                    member.setStockName(source.getStockName());
                    member.setSourceUpdatedAt(updatedAt);
                    return member;
                })
                .toList();
        stockBoardConstituentRepository.saveAll(members);
        stockBoardConstituentRepository.deleteByBoardCodeAndStockCodeNotIn(
                industry, List.copyOf(sourceByCode.keySet())
        );

        String watermarkName = StockSyncConstant.STOCK_BOARD_CONSTITUENT_LATEST_PREFIX + industry;
        StockSync watermark = stockSyncRepository.findByName(watermarkName);
        if (watermark == null) {
            watermark = new StockSync();
            watermark.setName(watermarkName);
        }
        watermark.setValue(String.valueOf(completedTradeDayWatermark));
        stockSyncRepository.save(watermark);
    }
}
