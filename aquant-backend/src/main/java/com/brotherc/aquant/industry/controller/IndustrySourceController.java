package com.brotherc.aquant.industry.controller;

import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.model.dto.ResponseDTO;
import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.industry.model.IndustryDataSource;
import com.brotherc.aquant.industry.model.vo.*;
import com.brotherc.aquant.industry.entity.StockIndustryBoard;
import com.brotherc.aquant.industry.repository.StockIndustryBoardRepository;
import com.brotherc.aquant.industry.service.StockBoardConstituentService;
import com.brotherc.aquant.industry.service.StockBoardConstituentEmService;
import com.brotherc.aquant.industry.service.StockIndustryBoardAnalysisService;
import com.brotherc.aquant.industry.service.StockIndustryBoardEmQueryService;
import com.brotherc.aquant.industry.service.StockIndustryBoardEmSyncService;
import com.brotherc.aquant.industry.service.StockIndustryBoardHistoryService;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/industrySource")
public class IndustrySourceController {

    private final StockIndustryBoardAnalysisService analysisService;
    private final StockIndustryBoardEmQueryService emQueryService;
    private final StockIndustryBoardHistoryService historyService;
    private final StockBoardConstituentService constituentService;
    private final StockBoardConstituentEmService emConstituentService;
    private final StockIndustryBoardRepository boardRepository;
    private final StockHelper stockHelper;
    private final StockSyncRepository stockSyncRepository;

    @GetMapping("/analysis")
    public ResponseDTO<IndustrySourceSnapshotVO<List<IndustryRiseAnalysisVO>>> analysis(
            @RequestParam(defaultValue = "THS") IndustryDataSource source,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "20") Integer rankLimit
    ) {
        return ResponseDTO.success(resolve(source, current -> current == IndustryDataSource.THS
                ? analysisService.analysis(startDate, endDate, rankLimit)
                : emQueryService.analysis(startDate, endDate, rankLimit)));
    }

    @GetMapping("/overview")
    public ResponseDTO<IndustrySourceSnapshotVO<StockIndustryBoardVO>> overview(
            @RequestParam(defaultValue = "THS") IndustryDataSource source,
            @RequestParam String industry,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return ResponseDTO.success(resolve(source, current -> current == IndustryDataSource.THS
                ? thsOverview(industry, tradeDate) : emQueryService.overview(industry, tradeDate)));
    }

    @GetMapping("/history/kline")
    public ResponseDTO<IndustrySourceSnapshotVO<List<StockIndustryBoardKVO>>> history(
            @RequestParam(defaultValue = "THS") IndustryDataSource source,
            @RequestParam String industry,
            @RequestParam(defaultValue = "1d") String frequency
    ) {
        return ResponseDTO.success(resolve(source, current -> current == IndustryDataSource.THS
                ? historyService.getHistory(industry, frequency) : emQueryService.history(industry, frequency)));
    }

    @GetMapping("/constituents")
    public ResponseDTO<IndustrySourceSnapshotVO<StockIndustryConstituentSnapshotVO>> constituents(
            @RequestParam(defaultValue = "THS") IndustryDataSource source,
            @RequestParam String industry,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return ResponseDTO.success(resolve(source, current -> {
            StockIndustryConstituentSnapshotVO snapshot = current == IndustryDataSource.THS
                    ? constituentService.getSnapshot(industry, tradeDate)
                    : emConstituentService.getSnapshot(industry, tradeDate);
            return snapshot.isAvailable() ? snapshot : null;
        }));
    }

    private <T> IndustrySourceSnapshotVO<T> resolve(IndustryDataSource requested, java.util.function.Function<IndustryDataSource, T> load) {
        T content = load.apply(requested);
        IndustryDataSource effective = requested;
        boolean fallback = false;
        if (isUnavailable(content)) {
            T fallbackContent = load.apply(requested.fallback());
            if (!isUnavailable(fallbackContent)) {
                content = fallbackContent;
                effective = requested.fallback();
                fallback = true;
            }
        }
        IndustrySourceSnapshotVO<T> snapshot = new IndustrySourceSnapshotVO<>();
        snapshot.setRequestedSource(requested);
        snapshot.setEffectiveSource(effective);
        snapshot.setFallback(fallback);
        snapshot.setAvailable(!isUnavailable(content));
        snapshot.setStale(snapshot.isAvailable() && isStale(effective));
        snapshot.setMessage(fallback ? "所选数据源暂无可用数据，已自动切换" :
                (snapshot.isAvailable() ? null : "两套行业数据源均暂无可用数据"));
        snapshot.setContent(content);
        return snapshot;
    }

    private boolean isUnavailable(Object content) {
        return content == null || (content instanceof List<?> list && list.isEmpty());
    }

    private StockIndustryBoardVO thsOverview(String industry, LocalDate tradeDate) {
        StockIndustryBoard board = boardRepository.findBySectorName(industry);
        if (board == null) return null;
        StockIndustryBoardVO view = new StockIndustryBoardVO();
        BeanUtils.copyProperties(board, view);
        if (tradeDate != null) {
            var history = historyService.findBySectorNameAndTradeDate(industry, tradeDate);
            if (history == null) return null;
            view.setTradeDate(tradeDate);
            view.setChangePercent(history.getChangePercent());
            view.setChangeAmount(history.getChangeAmount());
            view.setAveragePrice(history.getClosePrice());
            view.setTotalVolume(history.getVolume());
            view.setTotalAmount(history.getAmount());
            view.setNetInflow(null);
            view.setRiseCount(null);
            view.setFallCount(null);
            view.setLeadingStock(null);
            view.setLeadingStockPrice(null);
            view.setLeadingStockChangePercent(null);
        }
        return view;
    }

    private boolean isStale(IndustryDataSource source) {
        String name = source == IndustryDataSource.THS ? StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST
                : StockIndustryBoardEmSyncService.WATERMARK;
        StockSync watermark = stockSyncRepository.findByName(name);
        Long value = StockUtils.parseSyncTimestamp(watermark);
        return value == null || value < stockHelper.getLatestClosedTradeDaySyncWatermark(LocalDateTime.now());
    }
}
