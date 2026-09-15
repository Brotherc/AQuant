package com.brotherc.aquant.portfolio.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.utils.UserContext;
import com.brotherc.aquant.fund.repository.StockFundNetValueRepository;
import com.brotherc.aquant.portfolio.entity.UserBrokerAccount;
import com.brotherc.aquant.portfolio.entity.UserPortfolioImportBatch;
import com.brotherc.aquant.portfolio.entity.UserPortfolioPosition;
import com.brotherc.aquant.portfolio.entity.UserPortfolioTrade;
import com.brotherc.aquant.portfolio.model.vo.PortfolioImportResultVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeBatchReqVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;
import com.brotherc.aquant.portfolio.repository.*;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.stock.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UserPortfolioServiceTest {

    @Mock
    private UserPortfolioRepository portfolioRepository;
    @Mock
    private UserBrokerAccountRepository accountRepository;
    @Mock
    private UserPortfolioImportBatchRepository importBatchRepository;
    @Mock
    private UserPortfolioTradeRepository tradeRepository;
    @Mock
    private UserPortfolioPositionRepository positionRepository;
    @Mock
    private UserPortfolioCashRepository cashRepository;
    @Mock
    private UserPortfolioAccountSnapshotRepository accountSnapshotRepository;
    @Mock
    private StockQuoteRepository stockQuoteRepository;
    @Mock
    private StockQuoteHistoryRepository stockQuoteHistoryRepository;
    @Mock
    private StockFundNetValueRepository fundNetValueRepository;

    @InjectMocks
    private UserPortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        UserContext.set(1L, "tester");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldDeduplicateTradesAndRebuildAverageCostPosition() {
        UserBrokerAccount account = account();
        List<UserPortfolioTrade> savedTrades = new ArrayList<>();
        List<UserPortfolioPosition> savedPositions = new ArrayList<>();
        AtomicLong tradeId = new AtomicLong(1);
        mockTradePersistence(account, savedTrades, savedPositions, tradeId);

        PortfolioTradeSaveReqVO first = trade("BUY", "600000", "100", "10", "5");
        first.setSourceTradeId("T-1");
        PortfolioTradeSaveReqVO second = trade("BUY", "600000", "50", "11", "0");
        second.setSourceTradeId("T-2");
        PortfolioTradeBatchReqVO request = batch(first, second);

        PortfolioImportResultVO result = portfolioService.saveTrades(request);

        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getSkipCount()).isZero();
        assertThat(savedPositions).hasSize(1);
        UserPortfolioPosition position = savedPositions.get(0);
        assertThat(position.getAssetCode()).isEqualTo("sh600000");
        assertThat(position.getQuantity()).isEqualByComparingTo("150");
        assertThat(position.getCostAmount()).isEqualByComparingTo("1555");
        assertThat(position.getCostPrice()).isEqualByComparingTo("10.36666667");
        assertThat(position.getMarketValue()).isEqualByComparingTo("1800");
        assertThat(position.getUnrealizedProfit()).isEqualByComparingTo("245");
    }

    @Test
    void shouldSkipAnExistingTradeWithoutCreatingAnotherPositionChange() {
        UserBrokerAccount account = account();
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(account));
        when(importBatchRepository.save(any(UserPortfolioImportBatch.class))).thenAnswer(invocation -> {
            UserPortfolioImportBatch batch = invocation.getArgument(0);
            batch.setId(20L);
            return batch;
        });
        when(tradeRepository.existsByAccountIdAndDedupKey(any(), any())).thenReturn(true);
        when(tradeRepository.findAllByAccountIdAndStatusOrderByTradeTimeAscIdAsc(10L, "NORMAL"))
                .thenReturn(List.of());
        when(positionRepository.findAllByAccountIdInOrderByMarketValueDesc(anyList())).thenReturn(List.of());

        PortfolioTradeSaveReqVO trade = trade("BUY", "600000", "100", "10", "0");
        trade.setSourceTradeId("T-1");
        PortfolioImportResultVO result = portfolioService.saveTrades(batch(trade));

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getSkipCount()).isEqualTo(1);
    }

    @Test
    void shouldRejectSellingMoreThanTheCurrentPosition() {
        UserBrokerAccount account = account();
        List<UserPortfolioTrade> savedTrades = new ArrayList<>();
        List<UserPortfolioPosition> savedPositions = new ArrayList<>();
        mockTradePersistence(account, savedTrades, savedPositions, new AtomicLong(1));

        PortfolioTradeSaveReqVO initial = trade("POSITION_INIT", "600000", "10", "10", "0");
        initial.setSourceTradeId("T-1");
        PortfolioTradeSaveReqVO sell = trade("SELL", "600000", "11", "11", "0");
        sell.setSourceTradeId("T-2");

        assertThatThrownBy(() -> portfolioService.saveTrades(batch(initial, sell)))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ExceptionEnum.PORTFOLIO_POSITION_INSUFFICIENT.getCode());
    }

    @Test
    void shouldRejectAnAccountOwnedByAnotherUser() {
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.getTrades(10L, org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ExceptionEnum.PORTFOLIO_ACCOUNT_NOT_FOUND.getCode());
    }

    private void mockTradePersistence(
            UserBrokerAccount account,
            List<UserPortfolioTrade> savedTrades,
            List<UserPortfolioPosition> savedPositions,
            AtomicLong tradeId
    ) {
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(account));
        when(importBatchRepository.save(any(UserPortfolioImportBatch.class))).thenAnswer(invocation -> {
            UserPortfolioImportBatch batch = invocation.getArgument(0);
            batch.setId(20L);
            return batch;
        });
        when(tradeRepository.existsByAccountIdAndDedupKey(any(), any())).thenReturn(false);
        when(tradeRepository.save(any(UserPortfolioTrade.class))).thenAnswer(invocation -> {
            UserPortfolioTrade trade = invocation.getArgument(0);
            trade.setId(tradeId.getAndIncrement());
            savedTrades.add(trade);
            return trade;
        });
        when(tradeRepository.findAllByAccountIdAndStatusOrderByTradeTimeAscIdAsc(10L, "NORMAL"))
                .thenAnswer(invocation -> savedTrades);
        lenient().when(positionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPortfolioPosition> positions = invocation.getArgument(0);
            List<UserPortfolioPosition> copied = new ArrayList<>(positions);
            savedPositions.clear();
            savedPositions.addAll(copied);
            return positions;
        });
        lenient().when(positionRepository.findAllByAccountIdInOrderByMarketValueDesc(anyList()))
                .thenAnswer(invocation -> savedPositions);
        StockQuote quote = new StockQuote();
        quote.setCode("sh600000");
        quote.setName("浦发银行");
        quote.setLatestPrice(new BigDecimal("12"));
        quote.setCreatedAt(LocalDateTime.of(2026, 9, 9, 15, 0));
        lenient().when(stockQuoteRepository.findByCodeIn(anyList())).thenReturn(List.of(quote));
    }

    private UserBrokerAccount account() {
        UserBrokerAccount account = new UserBrokerAccount();
        account.setId(10L);
        account.setPortfolioId(5L);
        account.setUserId(1L);
        account.setAccountName("测试账户");
        return account;
    }

    private PortfolioTradeBatchReqVO batch(PortfolioTradeSaveReqVO... trades) {
        PortfolioTradeBatchReqVO request = new PortfolioTradeBatchReqVO();
        request.setAccountId(10L);
        request.setSource("MANUAL");
        request.setTrades(List.of(trades));
        return request;
    }

    private PortfolioTradeSaveReqVO trade(
            String type, String code, String quantity, String price, String commission
    ) {
        PortfolioTradeSaveReqVO trade = new PortfolioTradeSaveReqVO();
        trade.setAssetType("STOCK");
        trade.setAssetCode(code);
        trade.setAssetName("浦发银行");
        trade.setTradeType(type);
        trade.setTradeTime(LocalDateTime.of(2026, 9, 9, 10, 0));
        trade.setQuantity(new BigDecimal(quantity));
        trade.setPrice(new BigDecimal(price));
        trade.setCommission(new BigDecimal(commission));
        return trade;
    }
}
