package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.model.dto.stock.StockZhASpot;
import com.brotherc.aquant.repository.StockQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockQuoteService {

    private final StockQuoteRepository stockQuoteRepository;

    @Transactional(rollbackFor = Exception.class)
    public void save(List<StockZhASpot> stockZhASpotList) {
        List<StockQuote> dbLlist = stockQuoteRepository.findAll();
        Map<String, StockQuote> map = dbLlist.stream().collect(Collectors.toMap(StockQuote::getCode, o -> o));

        List<StockQuote> list = new ArrayList<>();

        for (StockZhASpot stockZhASpot : stockZhASpotList) {
            StockQuote sq = map.get(stockZhASpot.get代码());
            if (sq == null) {
                sq = new StockQuote();
            }
            sq.setCode(stockZhASpot.get代码());
            sq.setName(stockZhASpot.get名称());
            sq.setLatestPrice(stockZhASpot.get最新价());
            sq.setChangeAmount(stockZhASpot.get涨跌额());
            sq.setChangePercent(stockZhASpot.get涨跌幅());
            sq.setBuyPrice(stockZhASpot.get买入());
            sq.setSellPrice(stockZhASpot.get卖出());
            sq.setPrevClose(stockZhASpot.get昨收());
            sq.setOpenPrice(stockZhASpot.get今开());
            sq.setHighPrice(stockZhASpot.get最高());
            sq.setLowPrice(stockZhASpot.get最低());
            sq.setVolume(stockZhASpot.get成交量());
            sq.setTurnover(stockZhASpot.get成交额());
            sq.setQuoteTime(stockZhASpot.get时间戳());
            sq.setCreatedAt(LocalDateTime.now());
            list.add(sq);
        }
        stockQuoteRepository.saveAll(list);
    }

}
