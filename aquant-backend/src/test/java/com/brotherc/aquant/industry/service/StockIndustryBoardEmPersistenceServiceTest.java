package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.industry.entity.StockIndustryBoardEm;
import com.brotherc.aquant.industry.entity.StockIndustryBoardHistoryEm;
import com.brotherc.aquant.industry.repository.StockBoardConstituentEmRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardEmRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardHistoryEmRepository;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardDetail;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardKline;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockIndustryBoardEmPersistenceServiceTest {

    @Mock
    private StockHelper stockHelper;

    @Mock
    private StockIndustryBoardEmRepository boardRepository;

    @Mock
    private StockIndustryBoardHistoryEmRepository historyRepository;

    @Mock
    private StockBoardConstituentEmRepository constituentRepository;

    @InjectMocks
    private StockIndustryBoardEmPersistenceService service;

    @Test
    void applyDetailStoresSnapshotWithNormalizedUnits() {
        StockIndustryBoardEm row = new StockIndustryBoardEm();
        row.setSectorCode("BK1201");
        row.setTotalVolume(new BigDecimal("10184.9492"));
        EastmoneyBoardDetail detail = detail();
        LocalDateTime now = LocalDateTime.of(2026, 9, 14, 16, 0);

        service.applyDetail(row, detail, now);

        assertThat(row.getChangeAmount()).isEqualByComparingTo("147.92");
        assertThat(row.getOpenPrice()).isEqualByComparingTo("12591.8");
        assertThat(row.getPreClosePrice()).isEqualByComparingTo("12730.57");
        assertThat(row.getHighPrice()).isEqualByComparingTo("12956.03");
        assertThat(row.getLowPrice()).isEqualByComparingTo("12464.95");
        assertThat(row.getTurnoverRate()).isEqualByComparingTo("2.78");
        assertThat(row.getVolumeRatio()).isEqualByComparingTo("0.98");
        // 外盘 手 → 万手
        assertThat(row.getOuterDisc()).isEqualByComparingTo("5155.072");
        // 流通市值 元 → 亿元
        assertThat(row.getCirculatingMarketValue()).isEqualByComparingTo("156836.073");
        // 流通股本 股 → 亿股
        assertThat(row.getCirculatingShares()).isEqualByComparingTo("3666.8188");
        assertThat(row.getDetailTradeDate()).isEqualTo(LocalDate.of(2026, 9, 14));
        verify(boardRepository).save(row);
    }

    @Test
    void saveHistoryComputesChangeMetricsFromPreviousClose() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 14, 16, 0);
        when(historyRepository.findBySectorNameOrderByTradeDateAsc("电子")).thenReturn(List.of());

        service.saveHistory("电子", List.of(
                bar("2026-09-11 15:00", "12730.57"),
                bar("2026-09-14 15:00", "12878.49")), now);

        ArgumentCaptor<Collection<StockIndustryBoardHistoryEm>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(historyRepository).saveAll(captor.capture());
        List<StockIndustryBoardHistoryEm> saved = new ArrayList<>(captor.getValue());
        assertThat(saved).hasSize(2);
        // 首条无前收盘，涨跌指标留空
        assertThat(saved.get(0).getChangePercent()).isNull();
        assertThat(saved.get(0).getChangeAmount()).isNull();
        // 次日按前收盘推算
        assertThat(saved.get(1).getChangeAmount()).isEqualByComparingTo("147.92");
        BigDecimal expectedPercent = new BigDecimal("147.92").multiply(BigDecimal.valueOf(100))
                .divide(new BigDecimal("12730.57"), 4, RoundingMode.HALF_UP);
        assertThat(saved.get(1).getChangePercent()).isEqualByComparingTo(expectedPercent);
    }

    @Test
    void saveBoardsNormalizesUnitsAndRemovesDelistedBoards() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 14, 16, 0);
        EastmoneyBoardList.Board live = listBoard("BK1201", "电子");
        live.setTotalVolume(new BigDecimal("101849492"));
        live.setTotalAmount(new BigDecimal("429962094959"));
        live.setNetInflow(new BigDecimal(-5000000000L));
        StockIndustryBoardEm stale = new StockIndustryBoardEm();
        stale.setSectorCode("BK9999");
        stale.setSectorName("旧板块");
        when(boardRepository.findAll()).thenReturn(List.of(stale));
        when(stockHelper.latestTradeDayFallback(now.toLocalDate())).thenReturn(LocalDate.of(2026, 9, 14));

        service.saveBoards(List.of(live), now);

        ArgumentCaptor<Collection<StockIndustryBoardEm>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(boardRepository).saveAll(captor.capture());
        StockIndustryBoardEm saved = captor.getValue().iterator().next();
        // 成交量 手 → 万手，成交额/净流入 元 → 亿元
        assertThat(saved.getTotalVolume()).isEqualByComparingTo("10184.9492");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("4299.6209");
        assertThat(saved.getNetInflow()).isEqualByComparingTo("-50");
        // 上游已下线的板块被对账删除
        verify(boardRepository).deleteAll(List.of(stale));
    }

    private EastmoneyBoardDetail detail() {
        EastmoneyBoardDetail detail = new EastmoneyBoardDetail();
        detail.setSectorCode("BK1201");
        detail.setSectorName("电子");
        detail.setChangeAmount(new BigDecimal("147.92"));
        detail.setOpenPrice(new BigDecimal("12591.8"));
        detail.setPreClosePrice(new BigDecimal("12730.57"));
        detail.setHighPrice(new BigDecimal("12956.03"));
        detail.setLowPrice(new BigDecimal("12464.95"));
        detail.setTurnoverRate(new BigDecimal("2.78"));
        detail.setVolumeRatio(new BigDecimal("0.98"));
        detail.setOuterDisc(new BigDecimal("51550720"));
        detail.setCirculatingMarketValue(new BigDecimal("15683607296000"));
        detail.setCirculatingShares(new BigDecimal("366681878528"));
        return detail;
    }

    private EastmoneyBoardKline.Bar bar(String time, String close) {
        EastmoneyBoardKline.Bar bar = new EastmoneyBoardKline.Bar();
        bar.setTime(time);
        bar.setClosePrice(new BigDecimal(close));
        return bar;
    }

    private EastmoneyBoardList.Board listBoard(String code, String name) {
        EastmoneyBoardList.Board board = new EastmoneyBoardList.Board();
        board.setSectorCode(code);
        board.setSectorName(name);
        return board;
    }
}
