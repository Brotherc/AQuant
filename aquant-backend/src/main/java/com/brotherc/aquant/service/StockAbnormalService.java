package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockAbnormal;
import com.brotherc.aquant.repository.StockAbnormalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAbnormalService {

    private final StockAbnormalRepository stockAbnormalRepository;

    public List<String> findAllCodes() {
        List<StockAbnormal> stockAbnormals = stockAbnormalRepository.findAll();
        if (CollectionUtils.isEmpty(stockAbnormals)) {
            return Collections.emptyList();
        }

        return stockAbnormals.stream()
                .map(StockAbnormal::getCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

}
