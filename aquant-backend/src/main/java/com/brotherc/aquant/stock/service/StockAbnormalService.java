package com.brotherc.aquant.stock.service;

import com.brotherc.aquant.stock.entity.StockAbnormal;
import com.brotherc.aquant.stock.repository.StockAbnormalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAbnormalService {

    private final StockAbnormalRepository stockAbnormalRepository;

    public List<String> findAllCodes() {
        return stockAbnormalRepository.findAllCodes().stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    /**
     * 归档交易所确认的退市股票，代码必须包含交易所前缀。
     * 仅新增尚未归档的代码，已有异常记录保持不变。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveDelistedStocks(Map<String, String> stockNames, String source, String reason) {
        if (stockNames.isEmpty()) {
            return;
        }
        Set<String> existingCodes = new HashSet<>(stockAbnormalRepository.findCodesByCodeIn(stockNames.keySet()));

        List<StockAbnormal> stocks = stockNames.entrySet().stream()
                .filter(entry -> !existingCodes.contains(entry.getKey()))
                .map(entry -> {
                    StockAbnormal stock = new StockAbnormal();
                    stock.setCode(entry.getKey());
                    if (StringUtils.isNotBlank(entry.getValue())) {
                        stock.setName(entry.getValue());
                    }
                    stock.setAbnormalType("DELISTED");
                    stock.setAbnormalSource(source);
                    stock.setAbnormalReason(reason);
                    return stock;
                })
                .toList();
        if (stocks.isEmpty()) {
            log.info("退市股票均已归档，跳过保存，source={}, count={}", source, stockNames.size());
            return;
        }
        stockAbnormalRepository.saveAll(stocks);
        log.info("归档退市股票完成，source={}, count={}", source, stocks.size());
    }

}
