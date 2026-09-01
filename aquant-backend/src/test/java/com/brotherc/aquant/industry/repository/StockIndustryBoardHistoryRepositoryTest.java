package com.brotherc.aquant.industry.repository;

import com.brotherc.aquant.industry.entity.StockIndustryBoardHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StockIndustryBoardHistoryRepositoryTest {

    @Autowired
    private StockIndustryBoardHistoryRepository repository;

    @Test
    void findsOnlyTheLatestHistoryBeforeStartDateForEachSector() {
        repository.saveAll(List.of(
                history("保险", "2026-08-25", "100"),
                history("保险", "2026-08-27", "102"),
                history("银行", "2026-08-26", "200"),
                history("银行", "2026-08-28", "201")
        ));

        List<StockIndustryBoardHistory> result = repository
                .findLatestBeforeTradeDateForEachSector("2026-08-28");

        assertThat(result).extracting(StockIndustryBoardHistory::getSectorName)
                .containsExactly("保险", "银行");
        assertThat(result).extracting(StockIndustryBoardHistory::getTradeDate)
                .containsExactly("2026-08-27", "2026-08-26");
        assertThat(repository.countCalculableMissingChangeMetrics()).isEqualTo(2);
    }

    private StockIndustryBoardHistory history(String sectorName, String tradeDate, String closePrice) {
        StockIndustryBoardHistory history = new StockIndustryBoardHistory();
        history.setSectorName(sectorName);
        history.setTradeDate(tradeDate);
        history.setClosePrice(new BigDecimal(closePrice));
        return history;
    }
}
