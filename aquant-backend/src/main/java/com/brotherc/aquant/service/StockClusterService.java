package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockBoardConstituentQuote;
import com.brotherc.aquant.model.dto.akshare.StockBoardIndustryConsEm;
import com.brotherc.aquant.model.vo.stockindustryboard.BoardConstituentQuotePageReqVO;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardPageReqVO;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardVO;
import com.brotherc.aquant.model.vo.stockquote.StockQuotePageReqVO;
import com.brotherc.aquant.model.vo.stockquote.StockQuoteVO;
import com.brotherc.aquant.repository.StockBoardConstituentQuoteRepository;
import com.brotherc.aquant.task.StockSyncTask;
import com.brotherc.aquant.utils.StockHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockClusterService {

    private final StockBoardConstituentQuoteRepository stockBoardConstituentQuoteRepository;
    private final StockBoardConstituentQuoteService stockBoardConstituentQuoteService;

    private final StockHelper stockHelper;
    private final AKShareService aKShareService;
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

    public Page<StockBoardConstituentQuote> pageConstituentQuote(BoardConstituentQuotePageReqVO reqVO, Pageable pageable) {
        StockBoardConstituentQuote constituentQuote = stockBoardConstituentQuoteRepository.findFirstByBoardCode(reqVO.getBoardCode());
        if (constituentQuote == null || stockHelper.checkIsStartSync(constituentQuote.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())) {
            List<StockBoardIndustryConsEm> stockBoardIndustryConsEms = aKShareService.stockBoardIndustryConsEm(reqVO.getBoardCode());
            stockBoardConstituentQuoteService.save(reqVO.getBoardCode(), stockBoardIndustryConsEms);
        }

        return stockBoardConstituentQuoteRepository.findByBoardCode(reqVO.getBoardCode(), pageable);
    }

}
