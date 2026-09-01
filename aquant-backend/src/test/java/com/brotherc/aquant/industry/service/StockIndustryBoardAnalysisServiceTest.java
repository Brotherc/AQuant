package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.industry.entity.StockIndustryBoardHistory;
import com.brotherc.aquant.industry.model.vo.IndustryRiseAnalysisVO;
import com.brotherc.aquant.industry.repository.StockIndustryBoardHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockIndustryBoardAnalysisServiceTest {

    @Mock
    private StockIndustryBoardHistoryRepository repository;

    @InjectMocks
    private StockIndustryBoardAnalysisService service;

    @Test
    void ranksEachTradeDateByChangePercentWithStableNameTieBreak() {
        LocalDate startDate = LocalDate.of(2026, 8, 27);
        LocalDate endDate = LocalDate.of(2026, 8, 28);
        when(repository.findByTradeDateBetweenOrderByTradeDateAscSectorNameAsc(
                startDate.toString(), endDate.toString()))
                .thenReturn(List.of(
                        history("银行", "2026-08-27", "1.20", "0.12"),
                        history("保险", "2026-08-27", "1.20", "0.09"),
                        history("煤炭", "2026-08-27", "-0.50", "-0.08"),
                        history("缺失行业", "2026-08-27", null, null),
                        history("软件开发", "2026-08-28", "2.50", "0.31")
                ));
        List<IndustryRiseAnalysisVO> result = service.analysis(startDate, endDate, 20);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(IndustryRiseAnalysisVO::getSectorName)
                .containsExactly("保险", "银行", "煤炭", "软件开发");
        assertThat(result).extracting(IndustryRiseAnalysisVO::getRank)
                .containsExactly(1, 2, 3, 1);
    }

    @Test
    void respectsTheRequestedIndustryCountForEachTradeDate() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 28);
        List<StockIndustryBoardHistory> histories = new ArrayList<>();
        for (int index = 1; index <= 21; index++) {
            histories.add(history(
                    String.format("行业%02d", index),
                    tradeDate.toString(),
                    String.valueOf(index),
                    String.valueOf(index)
            ));
        }
        when(repository.findByTradeDateBetweenOrderByTradeDateAscSectorNameAsc(
                tradeDate.toString(), tradeDate.toString()))
                .thenReturn(histories);

        List<IndustryRiseAnalysisVO> result = service.analysis(tradeDate, tradeDate, 10);

        assertThat(result).hasSize(10);
        assertThat(result).extracting(IndustryRiseAnalysisVO::getRank)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 10).boxed().toList());
        assertThat(result.get(0).getSectorName()).isEqualTo("行业21");
        assertThat(result.get(9).getSectorName()).isEqualTo("行业12");
    }

    @Test
    void calculatesMissingMetricsFromTheLatestCloseBeforeTheRequestedRange() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 28);
        StockIndustryBoardHistory current = history("银行", tradeDate.toString(), null, null);
        current.setClosePrice(new BigDecimal("110"));
        StockIndustryBoardHistory predecessor = history("银行", "2026-08-27", "1", "1");
        predecessor.setClosePrice(new BigDecimal("100"));
        when(repository.findByTradeDateBetweenOrderByTradeDateAscSectorNameAsc(
                tradeDate.toString(), tradeDate.toString()))
                .thenReturn(List.of(current));
        when(repository.findLatestBeforeTradeDateForEachSector(tradeDate.toString()))
                .thenReturn(List.of(predecessor));

        List<IndustryRiseAnalysisVO> result = service.analysis(tradeDate, tradeDate, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChangeAmount()).isEqualByComparingTo("10");
        assertThat(result.get(0).getChangePercent()).isEqualByComparingTo("10.0000");
    }

    @Test
    void rejectsMissingReversedAndOversizedDateRanges() {
        assertThatThrownBy(() -> service.analysis(null, LocalDate.of(2026, 8, 28), 20))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("开始日期和结束日期不能为空");
        assertThatThrownBy(() -> service.analysis(
                LocalDate.of(2026, 8, 29), LocalDate.of(2026, 8, 28), 20))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("开始日期不能晚于结束日期");
        assertThatThrownBy(() -> service.analysis(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 28), 20))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("180个自然日");
        assertThatThrownBy(() -> service.analysis(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 28), 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("排名数量必须在1到100之间");
        assertThatThrownBy(() -> service.analysis(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 28), 101))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("排名数量必须在1到100之间");
    }

    private StockIndustryBoardHistory history(
            String sectorName,
            String tradeDate,
            String changePercent,
            String changeAmount
    ) {
        StockIndustryBoardHistory history = new StockIndustryBoardHistory();
        history.setSectorName(sectorName);
        history.setTradeDate(tradeDate);
        history.setChangePercent(decimal(changePercent));
        history.setChangeAmount(decimal(changeAmount));
        return history;
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
