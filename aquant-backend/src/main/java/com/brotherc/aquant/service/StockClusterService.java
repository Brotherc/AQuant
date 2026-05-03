package com.brotherc.aquant.service;

import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardPageReqVO;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardVO;
import com.brotherc.aquant.model.vo.stockquote.StockQuotePageReqVO;
import com.brotherc.aquant.model.vo.stockquote.StockQuoteVO;
import com.brotherc.aquant.task.StockSyncTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockClusterService {

    private final StockQuoteService stockQuoteService;
    private final StockIndustryBoardService stockIndustryBoardService;
    private final StockSyncTask stockSyncTask;

    public Page<StockQuoteVO> stockQuotePage(StockQuotePageReqVO reqVO, Pageable pageable) {
        if (reqVO.getRefresh()) {
            stockSyncTask.syncStackQuote();
        }
        return stockQuoteService.getPage(reqVO, pageable);
    }

    public Page<StockIndustryBoardVO> stockIndustryBoardPage(StockIndustryBoardPageReqVO reqVO, Pageable pageable) {
        if (reqVO.getRefresh()) {
            stockSyncTask.syncStockBoard();
        }
        return stockIndustryBoardService.stockIndustryBoardPage(reqVO, pageable);

    }

}
