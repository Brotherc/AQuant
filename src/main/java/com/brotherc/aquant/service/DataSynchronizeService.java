package com.brotherc.aquant.service;

import com.brotherc.aquant.model.dto.stock.StockZhASpot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataSynchronizeService {

    private final AKShareService aKShareService;
    private final StockQuoteService stockQuoteService;

    public void stockQuote() {
        List<StockZhASpot> stockZhASpots = aKShareService.stockZhASpot();
        if (!CollectionUtils.isEmpty(stockZhASpots)) {
            stockQuoteService.save(stockZhASpots);
        }
    }

}
