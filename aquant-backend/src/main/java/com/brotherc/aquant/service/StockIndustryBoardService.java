package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockIndustryBoard;
import com.brotherc.aquant.model.dto.akshare.StockBoardIndustryNameEm;
import com.brotherc.aquant.repository.StockIndustryBoardRepository;
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
public class StockIndustryBoardService {

    private final StockIndustryBoardRepository stockIndustryBoardRepository;

    @Transactional(rollbackFor = Exception.class)
    public void save(List<StockBoardIndustryNameEm> stockBoardList, LocalDateTime now) {
        List<StockIndustryBoard> dbLlist = stockIndustryBoardRepository.findAll();
        Map<String, StockIndustryBoard> map = dbLlist.stream().collect(Collectors.toMap(StockIndustryBoard::getBoardCode, o -> o));

        List<StockIndustryBoard> list = new ArrayList<>();

        for (StockBoardIndustryNameEm stockBoard : stockBoardList) {
            StockIndustryBoard sq = map.get(stockBoard.getBlockCode());
            if (sq == null) {
                sq = new StockIndustryBoard();
            }
            sq.setBoardCode(stockBoard.getBlockCode());
            sq.setBoardName(stockBoard.getBlockName());
            sq.setRankNo(stockBoard.getRank());
            sq.setLatestPrice(stockBoard.getLatestPrice());
            sq.setChangeAmount(stockBoard.getChangeAmount());
            sq.setChangePercent(stockBoard.getChangePercent());
            sq.setTotalMarketValue(stockBoard.getTotalMarketValue());
            sq.setTurnoverRate(stockBoard.getTurnoverRate());
            sq.setUpCount(stockBoard.getUpCount());
            sq.setDownCount(stockBoard.getDownCount());
            sq.setLeadingStockName(stockBoard.getLeadingStock());
            sq.setLeadingStockChangePercent(stockBoard.getLeadingStockChangePercent());
            String tradeDate = StockUtils.latestTradeDayFallback(LocalDate.now()).toString();
            sq.setTradeDate(tradeDate);
            sq.setCreatedAt(now);

            list.add(sq);
        }
        stockIndustryBoardRepository.saveAll(list);
    }

}
