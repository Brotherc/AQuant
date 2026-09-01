package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.industry.entity.StockBoardConstituentQuote;
import com.brotherc.aquant.industry.repository.StockBoardConstituentQuoteRepository;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryConsThs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockBoardConstituentQuoteSyncService {

    private final StockBoardConstituentQuoteRepository stockBoardConstituentQuoteRepository;

    @Transactional(rollbackFor = Exception.class)
    public void sync(String industry, List<StockBoardIndustryConsThs> sourceList) {
        if (CollectionUtils.isEmpty(sourceList)) {
            throw new IllegalStateException("行业成分股上游返回为空");
        }

        Map<String, StockBoardIndustryConsThs> sourceByCode = sourceList.stream()
                .filter(item -> item.getStockCode() != null && !item.getStockCode().isBlank())
                .collect(Collectors.toMap(
                        StockBoardIndustryConsThs::getStockCode,
                        item -> item,
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
        if (sourceByCode.isEmpty()) {
            throw new IllegalStateException("行业成分股上游未包含有效股票代码");
        }

        Map<String, StockBoardConstituentQuote> existingByCode = stockBoardConstituentQuoteRepository
                .findByBoardCodeOrderByChangePercentDesc(industry)
                .stream()
                .collect(Collectors.toMap(
                        StockBoardConstituentQuote::getStockCode,
                        item -> item,
                        (first, second) -> second
                ));
        LocalDateTime now = LocalDateTime.now();
        List<StockBoardConstituentQuote> saveList = new ArrayList<>(sourceByCode.size());
        for (StockBoardIndustryConsThs source : sourceByCode.values()) {
            StockBoardConstituentQuote target = existingByCode.get(source.getStockCode());
            if (target == null) {
                target = new StockBoardConstituentQuote();
                target.setBoardCode(industry);
                target.setStockCode(source.getStockCode());
            }
            target.setStockName(source.getStockName());
            target.setLatestPrice(source.getLatestPrice());
            target.setChangePercent(source.getChangePercent());
            target.setChangeAmount(source.getChangeAmount());
            target.setTurnover(source.getTurnover());
            target.setAmplitude(source.getAmplitude());
            target.setPeTtm(source.getPeTtm());
            target.setCreatedAt(now);
            saveList.add(target);
        }
        stockBoardConstituentQuoteRepository.saveAll(saveList);
        stockBoardConstituentQuoteRepository.deleteByBoardCodeAndStockCodeNotIn(
                industry, new ArrayList<>(sourceByCode.keySet())
        );
    }
}
