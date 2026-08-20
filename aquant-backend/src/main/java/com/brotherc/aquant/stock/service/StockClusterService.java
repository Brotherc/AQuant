package com.brotherc.aquant.stock.service;

import com.brotherc.aquant.industry.model.vo.StockIndustryBoardPageReqVO;
import com.brotherc.aquant.industry.model.vo.StockIndustryBoardVO;
import com.brotherc.aquant.stock.model.vo.StockQuotePageReqVO;
import com.brotherc.aquant.stock.model.vo.StockQuoteVO;
import com.brotherc.aquant.industry.service.StockIndustryBoardService;
import com.brotherc.aquant.task.StockSyncTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockClusterService {

    private final StockQuoteService stockQuoteService;
    private final StockIndustryBoardService stockIndustryBoardService;
    private final StockSyncTask stockSyncTask;

    public Page<StockQuoteVO> stockQuotePage(StockQuotePageReqVO reqVO, Pageable pageable) {
        if (reqVO.getRefresh()) {
            stockSyncTask.syncStackQuote(LocalDateTime.now());
        }
        return stockQuoteService.getPage(reqVO, pageable);
    }

    public Page<StockIndustryBoardVO> stockIndustryBoardPage(StockIndustryBoardPageReqVO reqVO, Pageable pageable) {
        if (reqVO.getRefresh()) {
            stockSyncTask.syncStockBoard(LocalDateTime.now());
        }
        return stockIndustryBoardService.stockIndustryBoardPage(reqVO, pageable);

    }

}
