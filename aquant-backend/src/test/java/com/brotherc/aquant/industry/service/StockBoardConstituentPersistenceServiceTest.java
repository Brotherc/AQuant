package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.industry.entity.StockBoardConstituent;
import com.brotherc.aquant.industry.repository.StockBoardConstituentRepository;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryConsThs;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBoardConstituentPersistenceServiceTest {

    @Mock
    private StockBoardConstituentRepository memberRepository;

    @Mock
    private StockSyncRepository stockSyncRepository;

    @InjectMocks
    private StockBoardConstituentPersistenceService service;

    @Test
    void replacesOnlyAfterAValidSourceResponseAndAdvancesTheIndustryWatermark() {
        StockBoardConstituent existing = new StockBoardConstituent();
        existing.setBoardCode("银行");
        existing.setStockCode("600000");
        existing.setStockName("旧名称");
        when(memberRepository.findByBoardCodeOrderByStockCodeAsc("银行")).thenReturn(List.of(existing));

        service.replace("银行", List.of(
                source("600000", "浦发银行"),
                source("000001", "平安银行"),
                source("000001", "平安银行")
        ), 123L);

        ArgumentCaptor<List<StockBoardConstituent>> saved = ArgumentCaptor.forClass(List.class);
        verify(memberRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(StockBoardConstituent::getStockCode)
                .containsExactly("600000", "000001");
        assertThat(existing.getStockName()).isEqualTo("浦发银行");
        verify(memberRepository).deleteByBoardCodeAndStockCodeNotIn("银行", List.of("600000", "000001"));
        ArgumentCaptor<StockSync> watermark = ArgumentCaptor.forClass(StockSync.class);
        verify(stockSyncRepository).save(watermark.capture());
        assertThat(watermark.getValue().getName())
                .isEqualTo(StockSyncConstant.STOCK_BOARD_CONSTITUENT_LATEST_PREFIX + "银行");
        assertThat(watermark.getValue().getValue()).isEqualTo("123");
    }

    @Test
    void rejectsAnEmptyResponseWithoutChangingMembersOrWatermark() {
        assertThrows(IllegalStateException.class, () -> service.replace("银行", List.of(), 123L));

        verify(memberRepository, never()).saveAll(anyList());
        verify(memberRepository, never()).deleteByBoardCodeAndStockCodeNotIn(any(), anyList());
        verify(stockSyncRepository, never()).save(any());
    }

    private StockBoardIndustryConsThs source(String code, String name) {
        StockBoardIndustryConsThs item = new StockBoardIndustryConsThs();
        item.setStockCode(code);
        item.setStockName(name);
        return item;
    }
}
