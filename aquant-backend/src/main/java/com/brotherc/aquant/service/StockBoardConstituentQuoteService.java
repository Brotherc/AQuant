package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockBoardConstituentQuote;
import com.brotherc.aquant.model.dto.akshare.StockBoardIndustryConsEm;
import com.brotherc.aquant.repository.StockBoardConstituentQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockBoardConstituentQuoteService {

    private final StockBoardConstituentQuoteRepository stockBoardConstituentQuoteRepository;

    @Transactional(rollbackFor = Exception.class)
    public void save(String boardCode, List<StockBoardIndustryConsEm> stockBoardIndustryConsEms) {
        // 获取股票代码列表
        List<String> stockCodeList = stockBoardIndustryConsEms.stream().map(StockBoardIndustryConsEm::getStockCode).toList();
        if (!CollectionUtils.isEmpty(stockCodeList)) {
            // 根据板块代码和股票代码列表查询板块成份股数据
            List<StockBoardConstituentQuote> list = stockBoardConstituentQuoteRepository.findByBoardCodeAndStockCodeIn(boardCode, stockCodeList);
            Map<String, StockBoardConstituentQuote> map = list.stream().collect(Collectors.toMap(StockBoardConstituentQuote::getStockCode, v -> v));

            List<StockBoardConstituentQuote> saveList = new ArrayList<>();

            for (StockBoardIndustryConsEm stockBoard : stockBoardIndustryConsEms) {
                StockBoardConstituentQuote constituentQuote = map.get(stockBoard.getStockCode());
                if (constituentQuote == null) {
                    constituentQuote = new StockBoardConstituentQuote();
                    constituentQuote.setBoardCode(boardCode);
                    constituentQuote.setStockCode(stockBoard.getStockCode());
                }
                constituentQuote.setStockName(stockBoard.getStockName());
                constituentQuote.setLatestPrice(stockBoard.getLatestPrice());
                constituentQuote.setChangePercent(stockBoard.getChangePercent());
                constituentQuote.setChangeAmount(stockBoard.getChangeAmount());
                constituentQuote.setVolume(stockBoard.getVolume());
                constituentQuote.setTurnover(stockBoard.getTurnover());
                constituentQuote.setAmplitude(stockBoard.getAmplitude());
                constituentQuote.setHighPrice(stockBoard.getHighPrice());
                constituentQuote.setLowPrice(stockBoard.getLowPrice());
                constituentQuote.setOpenPrice(stockBoard.getOpenPrice());
                constituentQuote.setPrevClose(stockBoard.getPrevClose());
                constituentQuote.setTurnoverRate(stockBoard.getTurnoverRate());
                constituentQuote.setPeTtm(stockBoard.getPeTtm());
                constituentQuote.setPb(stockBoard.getPb());
                constituentQuote.setCreatedAt(LocalDateTime.now());
                saveList.add(constituentQuote);
            }
            if (!CollectionUtils.isEmpty(saveList)) {
                stockBoardConstituentQuoteRepository.saveAll(saveList);
            }
        }
    }

}
