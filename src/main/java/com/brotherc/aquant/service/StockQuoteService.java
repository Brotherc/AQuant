package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.model.dto.stock.StockZhASpot;
import com.brotherc.aquant.repository.StockQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockQuoteService {

    private final StockQuoteRepository stockQuoteRepository;

    @Transactional(rollbackFor = Exception.class)
    public void save(List<StockZhASpot> stockZhASpotList) {
        stockQuoteRepository.deleteAll();

        List<StockQuote> list = stockZhASpotList.stream().map(o -> {
            StockQuote stockQuote = new StockQuote();
            stockQuote.setCode(o.get代码());
            stockQuote.setName(o.get名称());
            stockQuote.setLatestPrice(o.get最新价());
            stockQuote.setChangeAmount(o.get涨跌额());
            stockQuote.setChangePercent(o.get涨跌幅());
            stockQuote.setBuyPrice(o.get买入());
            stockQuote.setSellPrice(o.get卖出());
            stockQuote.setPrevClose(o.get昨收());
            stockQuote.setOpenPrice(o.get今开());
            stockQuote.setHighPrice(o.get最高());
            stockQuote.setLowPrice(o.get最低());
            stockQuote.setVolume(o.get成交量());
            stockQuote.setTurnover(o.get成交额());
            stockQuote.setQuoteTime(o.get时间戳());
            stockQuote.setCreatedAt(LocalDateTime.now());
            return stockQuote;
        }).toList();
        stockQuoteRepository.saveAll(list);
    }

}
