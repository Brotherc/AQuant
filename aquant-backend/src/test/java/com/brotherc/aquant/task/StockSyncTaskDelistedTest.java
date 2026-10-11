package com.brotherc.aquant.task;

import com.brotherc.aquant.integration.akshare.model.StockInfoShDelist;
import com.brotherc.aquant.integration.akshare.model.StockInfoSzDelist;
import com.brotherc.aquant.integration.akshare.service.AKShareService;
import com.brotherc.aquant.stock.model.dto.StockQuoteCodeNameProjection;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.stock.service.StockAbnormalService;
import com.brotherc.aquant.stock.service.StockQuoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockSyncTaskDelistedTest {

    @Mock
    private AKShareService aKShareService;
    @Mock
    private StockAbnormalService stockAbnormalService;
    @Mock
    private StockQuoteRepository stockQuoteRepository;
    @Mock
    private StockQuoteService stockQuoteService;
    @InjectMocks
    private StockSyncTask task;

    @Test
    void shouldArchiveExchangeListsBeforeDeletingQuotes() {
        when(aKShareService.stockInfoSzDelist("终止上市公司")).thenReturn(List.of(
                new StockInfoSzDelist("000001", "终止上市股票", null, null),
                new StockInfoSzDelist("000001", "终止上市股票", null, null)));
        when(aKShareService.stockInfoShDelist("全部")).thenReturn(List.of(
                new StockInfoShDelist("600001", "退市股票", null, null),
                new StockInfoShDelist("600002", "暂停上市股票", null, null),
                new StockInfoShDelist(" ", "退市无代码", null, null)));
        when(stockAbnormalService.findAllCodes()).thenReturn(List.of("sz000001", "sh600001"));
        StockQuoteCodeNameProjection quote = mock(StockQuoteCodeNameProjection.class);
        when(quote.getCode()).thenReturn("sz000001");
        when(quote.getName()).thenReturn("旧名称");
        when(stockQuoteRepository.findAllCodeNames()).thenReturn(List.of(quote));

        task.clearDelistedStockData();

        var order = inOrder(stockAbnormalService, stockQuoteService);
        order.verify(stockAbnormalService).saveDelistedStocks(Map.of("sz000001", "终止上市股票"),
                "stock_info_sz_delist", "深交所终止上市公司");
        order.verify(stockAbnormalService).saveDelistedStocks(Map.of("sh600001", "退市股票"),
                "stock_info_sh_delist", "上交所退市公司（公司简称包含退市）");
        order.verify(stockAbnormalService).saveDelistedStocks(Map.of(),
                "stock_quote", "本地行情股票名称符合退市规则");
        order.verify(stockAbnormalService).findAllCodes();
        order.verify(stockQuoteService).deleteQuoteAndHistoryByCodes(List.of("sz000001", "sh600001"));
    }

    @Test
    void shouldStopCleanupWhenArchivingFails() {
        when(aKShareService.stockInfoSzDelist("终止上市公司")).thenReturn(List.of(
                new StockInfoSzDelist("000001", "终止上市股票", null, null)));
        doThrow(new IllegalStateException("归档失败")).when(stockAbnormalService)
                .saveDelistedStocks(anyMap(), anyString(), anyString());

        assertThatThrownBy(task::clearDelistedStockData).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(stockQuoteRepository, stockQuoteService);
    }

    @Test
    void shouldUseLocalArchiveWhenExchangeRequestsFail() {
        when(aKShareService.stockInfoSzDelist("终止上市公司")).thenThrow(new IllegalStateException("上游异常"));
        when(aKShareService.stockInfoShDelist("全部")).thenThrow(new IllegalStateException("上游异常"));
        when(stockAbnormalService.findAllCodes()).thenReturn(List.of("sh600001"));
        StockQuoteCodeNameProjection quote = mock(StockQuoteCodeNameProjection.class);
        when(quote.getCode()).thenReturn("sh600001");
        when(quote.getName()).thenReturn("旧名称");
        when(stockQuoteRepository.findAllCodeNames()).thenReturn(List.of(quote));

        task.clearDelistedStockData();

        verify(stockAbnormalService).saveDelistedStocks(Map.of(), "stock_quote", "本地行情股票名称符合退市规则");
        verify(stockQuoteService).deleteQuoteAndHistoryByCodes(List.of("sh600001"));
    }

    @Test
    void shouldArchiveNameMatchesMissingFromExchangeListsAndKeepNormalStocks() {
        when(aKShareService.stockInfoSzDelist("终止上市公司")).thenReturn(List.of());
        when(aKShareService.stockInfoShDelist("全部")).thenReturn(List.of());
        when(stockAbnormalService.findAllCodes()).thenReturn(
                List.of("sz000001", "sh600001", "bj830001", "sh600002"));
        List<StockQuoteCodeNameProjection> quotes = List.of(
                quote("sz000001", "测试退"), quote("sh600001", "退市测试"),
                quote("bj830001", "北交退"), quote("sh600002", "测试退市股票"),
                quote("sh600003", "正常股票"));
        when(stockQuoteRepository.findAllCodeNames()).thenReturn(quotes);

        task.clearDelistedStockData();

        var order = inOrder(stockAbnormalService, stockQuoteService);
        order.verify(stockAbnormalService).saveDelistedStocks(Map.of(
                "sz000001", "测试退", "sh600001", "退市测试",
                "bj830001", "北交退", "sh600002", "测试退市股票"),
                "stock_quote", "本地行情股票名称符合退市规则");
        order.verify(stockAbnormalService).findAllCodes();
        order.verify(stockQuoteService).deleteQuoteAndHistoryByCodes(
                List.of("sz000001", "sh600001", "bj830001", "sh600002"));
    }

    @Test
    void shouldStopDeletionWhenLocalNameArchivingFails() {
        when(aKShareService.stockInfoSzDelist("终止上市公司")).thenReturn(List.of());
        when(aKShareService.stockInfoShDelist("全部")).thenReturn(List.of());
        StockQuoteCodeNameProjection quote = quote("sz000001", "测试退");
        when(stockQuoteRepository.findAllCodeNames()).thenReturn(List.of(quote));
        doAnswer(invocation -> {
            if ("stock_quote".equals(invocation.getArgument(1))) {
                throw new IllegalStateException("本地归档失败");
            }
            return null;
        }).when(stockAbnormalService).saveDelistedStocks(anyMap(), anyString(), anyString());

        assertThatThrownBy(task::clearDelistedStockData).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(stockQuoteService);
    }

    @Test
    void shouldCleanArchivedCodesEvenWhenNoLocalQuotesRemain() {
        when(aKShareService.stockInfoSzDelist("终止上市公司")).thenReturn(List.of());
        when(aKShareService.stockInfoShDelist("全部")).thenReturn(List.of());
        when(stockQuoteRepository.findAllCodeNames()).thenReturn(List.of());
        when(stockAbnormalService.findAllCodes()).thenReturn(List.of("sh600001"));

        task.clearDelistedStockData();

        verify(stockQuoteService).deleteQuoteAndHistoryByCodes(List.of("sh600001"));
    }

    @Test
    void shouldSkipDeletionWhenArchiveIsEmpty() {
        when(aKShareService.stockInfoSzDelist("终止上市公司")).thenReturn(List.of());
        when(aKShareService.stockInfoShDelist("全部")).thenReturn(List.of());
        when(stockQuoteRepository.findAllCodeNames()).thenReturn(List.of());
        when(stockAbnormalService.findAllCodes()).thenReturn(List.of());

        task.clearDelistedStockData();

        verifyNoInteractions(stockQuoteService);
    }

    private StockQuoteCodeNameProjection quote(String code, String name) {
        StockQuoteCodeNameProjection quote = mock(StockQuoteCodeNameProjection.class);
        when(quote.getCode()).thenReturn(code);
        when(quote.getName()).thenReturn(name);
        return quote;
    }
}
