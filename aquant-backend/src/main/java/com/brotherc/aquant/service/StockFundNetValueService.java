package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockFundNetValue;
import com.brotherc.aquant.repository.StockFundNetValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockFundNetValueService {

    private final StockFundNetValueRepository stockFundNetValueRepository;

    public List<StockFundNetValue> getFundNetValues(String fundCode) {
        return stockFundNetValueRepository.findByFundCodeOrderByNavDateAsc(fundCode);
    }

}
