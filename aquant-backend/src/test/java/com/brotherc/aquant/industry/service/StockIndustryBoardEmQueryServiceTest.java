package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.industry.entity.StockIndustryBoardHistoryEm;
import com.brotherc.aquant.industry.repository.StockIndustryBoardEmRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardHistoryEmRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockIndustryBoardEmQueryServiceTest {
    @Mock
    private StockIndustryBoardEmRepository boardRepository;
    @Mock
    private StockIndustryBoardHistoryEmRepository historyRepository;
    @InjectMocks
    private StockIndustryBoardEmQueryService service;

    @Test
    void ranksOnlyEastmoneyHistoryRows() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 31);
        when(historyRepository.findByTradeDateBetweenOrderByTradeDateAscSectorNameAsc(
                tradeDate.toString(), tradeDate.toString())).thenReturn(List.of(
                row("东财行业A", "2.1"), row("东财行业B", "1.2")
        ));

        assertThat(service.analysis(tradeDate, tradeDate, 20))
                .extracting(item -> item.getSectorName())
                .containsExactly("东财行业A", "东财行业B");
    }

    private StockIndustryBoardHistoryEm row(String name, String percent) {
        StockIndustryBoardHistoryEm row = new StockIndustryBoardHistoryEm();
        row.setSectorName(name);
        row.setTradeDate("2026-08-31");
        row.setChangePercent(new BigDecimal(percent));
        return row;
    }
}
