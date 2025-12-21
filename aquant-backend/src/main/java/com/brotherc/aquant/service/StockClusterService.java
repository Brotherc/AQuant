package com.brotherc.aquant.service;

import com.brotherc.aquant.constant.StockSyncConstant;
import com.brotherc.aquant.entity.StockSync;
import com.brotherc.aquant.exception.BusinessException;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.dto.akshare.StockZhASpot;
import com.brotherc.aquant.model.vo.stockquote.StockQuotePageReqVO;
import com.brotherc.aquant.model.vo.stockquote.StockQuoteVO;
import com.brotherc.aquant.repository.StockSyncRepository;
import com.brotherc.aquant.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockClusterService {

    private final StockSyncRepository stockSyncRepository;

    private final AKShareService aKShareService;
    private final StockQuoteService stockQuoteService;
    private final StockSyncService stockSyncService;

    public Page<StockQuoteVO> stockQuotePage(StockQuotePageReqVO reqVO, Pageable pageable) {
        if (reqVO.getRefresh()) {
            // 查询上一次同步的时间戳
            StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_DAILY_LATEST);
            Long lastTimestamp = Optional.ofNullable(stockSync).map(StockSync::getValue).map(Long::valueOf).orElse(null);
            boolean startSync = StockUtils.checkIsStartSync(lastTimestamp);

            if (!startSync) {
                throw new BusinessException(ExceptionEnum.STOCK_QUOTE_SYNC_NOT_START);
            }

            long now = System.currentTimeMillis();
            boolean notExceedOneMinute = now <= lastTimestamp + 60_000;
            if (notExceedOneMinute) {
                throw new BusinessException(ExceptionEnum.STOCK_QUOTE_FREQUENT);
            }

            // 查询第三方API获取最新A股股票最新行情
            List<StockZhASpot> stockZhASpots = aKShareService.stockZhASpot();
            // 同步数据
            stockSyncService.stockQuote(stockZhASpots, stockSync, now);
        }
        return stockQuoteService.getPage(reqVO, pageable);
    }

}
