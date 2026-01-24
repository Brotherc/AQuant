package com.brotherc.aquant.service;

import com.brotherc.aquant.constant.StockSyncConstant;
import com.brotherc.aquant.entity.StockBoardConstituentQuote;
import com.brotherc.aquant.entity.StockSync;
import com.brotherc.aquant.exception.BusinessException;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.dto.akshare.StockBoardIndustryConsEm;
import com.brotherc.aquant.model.dto.akshare.StockBoardIndustryNameEm;
import com.brotherc.aquant.model.dto.akshare.StockBoardIndustrySpotEm;
import com.brotherc.aquant.model.dto.akshare.StockZhASpot;
import com.brotherc.aquant.model.vo.stockindustryboard.BoardConstituentQuotePageReqVO;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardPageReqVO;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardVO;
import com.brotherc.aquant.model.vo.stockquote.StockQuotePageReqVO;
import com.brotherc.aquant.model.vo.stockquote.StockQuoteVO;
import com.brotherc.aquant.repository.StockBoardConstituentQuoteRepository;
import com.brotherc.aquant.repository.StockSyncRepository;
import com.brotherc.aquant.utils.StockHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockClusterService {

    private final StockSyncRepository stockSyncRepository;
    private final StockBoardConstituentQuoteRepository stockBoardConstituentQuoteRepository;
    private final StockBoardConstituentQuoteService stockBoardConstituentQuoteService;

    private final StockHelper stockHelper;
    private final AKShareService aKShareService;
    private final StockQuoteService stockQuoteService;
    private final StockSyncService stockSyncService;
    private final StockIndustryBoardService stockIndustryBoardService;

    public Page<StockQuoteVO> stockQuotePage(StockQuotePageReqVO reqVO, Pageable pageable) {
        if (reqVO.getRefresh()) {
            // 查询上一次同步的时间戳
            StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_DAILY_LATEST);
            Long lastTimestamp = Optional.ofNullable(stockSync).map(StockSync::getValue).map(Long::valueOf).orElse(null);
            boolean startSync = stockHelper.checkIsStartSync(lastTimestamp);

            if (!startSync) {
                throw new BusinessException(ExceptionEnum.STOCK_SYNC_NOT_START);
            }

            long now = System.currentTimeMillis();
            boolean notExceedOneMinute = now <= lastTimestamp + 60_000;
            if (notExceedOneMinute) {
                throw new BusinessException(ExceptionEnum.STOCK_REFRESH_FREQUENT);
            }

            // 查询第三方API获取最新A股股票最新行情
            List<StockZhASpot> stockZhASpots = aKShareService.stockZhASpot();
            // 同步数据
            stockSyncService.stockQuote(stockZhASpots, stockSync, now);
        }
        return stockQuoteService.getPage(reqVO, pageable);
    }

    public Page<StockIndustryBoardVO> stockIndustryBoardPage(StockIndustryBoardPageReqVO reqVO, Pageable pageable) {
        if (reqVO.getRefresh()) {
            // 查询上一次同步的时间戳
            StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST);
            Long lastTimestamp = Optional.ofNullable(stockSync).map(StockSync::getValue).map(Long::valueOf).orElse(null);
            boolean startSync = stockHelper.checkIsStartSync(lastTimestamp);

            if (!startSync) {
                throw new BusinessException(ExceptionEnum.STOCK_SYNC_NOT_START);
            }
            long now = System.currentTimeMillis();

            // 查询第三方API获取最新A股板块行情
            List<StockBoardIndustryNameEm> stockBoardList = aKShareService.stockBoardIndustryNameEm();

            Map<String, StockBoardIndustrySpotEm> stockBoardDetailMap = new HashMap<>();
            for (StockBoardIndustryNameEm stockBoard : stockBoardList) {
                StockBoardIndustrySpotEm stockBoardIndustrySpotEm = aKShareService.stockBoardIndustrySpotEm(stockBoard.getBlockCode());
                stockBoardDetailMap.put(stockBoard.getBlockCode() + ":" + stockBoard.getBlockName(), stockBoardIndustrySpotEm);

                long sleepMillis = ThreadLocalRandom.current().nextLong(2000, 3001);
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("线程被中断，提前结束行业板块行情同步");
                    break;
                }
            }

            // 同步数据
            stockSyncService.stockBoardIndustry(stockBoardList, stockBoardDetailMap, stockSync, now);
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
