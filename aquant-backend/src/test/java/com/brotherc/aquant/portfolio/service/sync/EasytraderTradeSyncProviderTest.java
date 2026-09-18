package com.brotherc.aquant.portfolio.service.sync;

import com.brotherc.aquant.integration.easytrader.EasytraderClient;
import com.brotherc.aquant.portfolio.model.vo.BrokerPositionSnapshotVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EasytraderTradeSyncProviderTest {

    @Mock
    private EasytraderClient easytraderClient;

    @InjectMocks
    private EasytraderTradeSyncProvider provider;

    @Test
    void mapsTodayTradesOfMultipleSecurities() {
        when(easytraderClient.todayTrades()).thenReturn(List.of(
                Map.of("成交日期", "2026-09-16", "成交时间", "14:57:03", "证券代码", "600519",
                        "证券名称", "贵州茅台", "操作", "证券买入", "成交价格", "1500.00",
                        "成交数量", "100", "成交金额", "150000.00", "合同编号", "HT001"),
                Map.of("成交日期", "2026-09-16", "成交时间", "10:00:00", "证券代码", "510300",
                        "证券名称", "沪深300ETF", "操作", "证券卖出", "成交价格", "4.00",
                        "成交数量", "1000", "成交金额", "4000.00", "合同编号", "HT002")));

        assertThat(provider.channel()).isEqualTo("EASYTRADER");
        List<PortfolioTradeSaveReqVO> trades = provider.fetchTrades();

        assertThat(trades).hasSize(2);
        PortfolioTradeSaveReqVO buy = trades.get(0);
        assertThat(buy.getTradeType()).isEqualTo("BUY");
        assertThat(buy.getMarket()).isEqualTo("SH");
        assertThat(buy.getQuantity()).isEqualByComparingTo("100");
        assertThat(buy.getGrossAmount()).isEqualByComparingTo("150000");
        // 买入扣减现金
        assertThat(buy.getNetAmount()).isEqualByComparingTo("-150000");
        assertThat(buy.getTradeTime().toString()).isEqualTo("2026-09-16T14:57:03");
        assertThat(buy.getSourceTradeId()).isEqualTo("HT001");

        PortfolioTradeSaveReqVO sell = trades.get(1);
        assertThat(sell.getTradeType()).isEqualTo("SELL");
        assertThat(sell.getAssetType()).isEqualTo("ETF");
        // 510300 沪深300ETF 为沪市品种（51 开头）
        assertThat(sell.getMarket()).isEqualTo("SH");
        assertThat(sell.getNetAmount()).isEqualByComparingTo("4000");
    }

    @Test
    void rejectsUnknownTradeDirection() {
        when(easytraderClient.todayTrades()).thenReturn(List.of(
                Map.of("证券代码", "600519", "操作", "未知操作")));

        assertThatThrownBy(() -> provider.fetchTrades())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("暂不支持 easytrader 成交方向");
    }

    @Test
    void mapsClientGridRowsToSnapshot() {
        when(easytraderClient.position()).thenReturn(List.of(
                Map.of("证券代码", "600519", "证券名称", "贵州茅台", "股票余额", "100",
                        "可用余额", "100", "成本价", "1500.00", "市值", "150000.00")));

        BrokerPositionSnapshotVO snapshot = provider.fetchPositions();

        assertThat(snapshot.getChannel()).isEqualTo("EASYTRADER");
        assertThat(snapshot.getPositions()).hasSize(1);
        var item = snapshot.getPositions().get(0);
        assertThat(item.getSecurityCode()).isEqualTo("600519");
        assertThat(item.getHoldingQuantity()).isEqualByComparingTo("100");
        assertThat(item.getCostPrice()).isEqualByComparingTo("1500.00");
        assertThat(item.getMarketValue()).isEqualByComparingTo("150000.00");
        // 客户端网格不含盈亏指标，留空
        assertThat(item.getIncome()).isNull();
    }

}
