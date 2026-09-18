package com.brotherc.aquant.portfolio.service.sync;

import com.brotherc.aquant.integration.eastmoney.service.EastmoneyJywgClient;
import com.brotherc.aquant.portfolio.model.vo.BrokerPositionSnapshotVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EastmoneyWebTradeSyncProviderTest {

    @Mock
    private EastmoneyJywgClient jywgClient;

    @InjectMocks
    private EastmoneyWebTradeSyncProvider provider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsRealJywgMatchRowWithFeesAndSignedAmount() throws Exception {
        // 2026-09-17 实际抓包行（queryTodayMatchWEB/queryHisMatchMergeWEB 小写体系）
        String json = """
                [
                  {"bizdate":"20260916","bsflag":"0B","bsflag_ex":"证券买入","cleardate":"20260916",
                   "fee_ghf":"0.45","fee_jsxf":"8.94","fee_sxf":"11.41","fee_yhs":"0.00",
                   "fundbal":"355.59","fundeffect":"-45644.86","market":"HA","market_ex":"沪市A股",
                   "matchamt":"45633.00","matchcode":"48222364","matchprice":"3.7100",
                   "matchqty":"12300","matchtime":"134357","orderid":"0701201748",
                   "stkbal":"135200","stkcode":"600255","stkname":"鑫科材料","stktype":"0"}
                ]
                """;
        when(jywgClient.todayMatches()).thenReturn(objectMapper.readTree(json));
        when(jywgClient.historyMatches(any(), any())).thenReturn(objectMapper.readTree("[]"));

        assertThat(provider.channel()).isEqualTo("EM_WEB");
        List<PortfolioTradeSaveReqVO> trades = provider.fetchTrades();

        assertThat(trades).hasSize(1);
        PortfolioTradeSaveReqVO buy = trades.get(0);
        assertThat(buy.getTradeType()).isEqualTo("BUY");
        assertThat(buy.getAssetType()).isEqualTo("STOCK");
        assertThat(buy.getMarket()).isEqualTo("SH");
        assertThat(buy.getAssetCode()).isEqualTo("600255");
        assertThat(buy.getAssetName()).isEqualTo("鑫科材料");
        // 日期 bizdate + 时分秒 matchtime 组合
        assertThat(buy.getTradeTime().toString()).isEqualTo("2026-09-16T13:43:57");
        assertThat(buy.getQuantity()).isEqualByComparingTo("12300");
        assertThat(buy.getPrice()).isEqualByComparingTo("3.7100");
        assertThat(buy.getGrossAmount()).isEqualByComparingTo("45633.00");
        // 真实费用明细直接采用，不再推算
        assertThat(buy.getCommission()).isEqualByComparingTo("11.41");
        assertThat(buy.getStampDuty()).isEqualByComparingTo("0.00");
        assertThat(buy.getTransferFee()).isEqualByComparingTo("0.45");
        assertThat(buy.getOtherFee()).isEqualByComparingTo("8.94");
        // fundeffect 签名资金净额（含费）直接采用
        assertThat(buy.getNetAmount()).isEqualByComparingTo("-45644.86");
        assertThat(buy.getSourceTradeId()).isEqualTo("48222364");
    }

    @Test
    void mergesTodayAndHistoryRowsWithDeduplication() throws Exception {
        // 同一笔成交在当日与历史接口都出现，按成交编号去重为一条
        String sellRow = """
                {"bizdate":"20260917","bsflag":"0S","bsflag_ex":"证券卖出","matchamt":"1000.00",
                 "matchcode":"9001","matchprice":"10.00","matchqty":"100","matchtime":"093100",
                 "stkcode":"600036","stkname":"招商银行"}
                """;
        String buyRow = """
                {"bizdate":"20260916","bsflag":"0B","bsflag_ex":"证券买入","matchamt":"900.00",
                 "matchcode":"9002","matchprice":"9.00","matchqty":"100","matchtime":"100000",
                 "stkcode":"600036","stkname":"招商银行"}
                """;
        when(jywgClient.todayMatches()).thenReturn(objectMapper.readTree("[" + sellRow + "]"));
        when(jywgClient.historyMatches(any(), any()))
                .thenReturn(objectMapper.readTree("[" + sellRow + "," + buyRow + "]"));

        List<PortfolioTradeSaveReqVO> trades = provider.fetchTrades();

        assertThat(trades).hasSize(2);
        assertThat(trades.stream().map(PortfolioTradeSaveReqVO::getSourceTradeId))
                .containsExactlyInAnyOrder("9001", "9002");
        assertThat(trades.get(0).getTradeType()).isEqualTo("SELL");
    }

    @Test
    void mapsAssetAndPositionSnapshotFromRealFields() throws Exception {
        // 2026-09-18 queryAssetAndPositionV1 实际抓包：持仓层为拼音缩写体系，
        // Ykbl/Drykbl 为小数比例（×100=百分比口径），行级 Dryk 盘后可能为空
        String json = """
                [{"Zzc":"491950.03","RMBZzc":"491950.03","Zxsz":"491815.00","totalSecMkval":"491815.00",
                  "Kyzj":"135.03","Kqzj":"135.03","Zjye":"135.03","Ljyk":"5320.24","Dryk":"","bonds":[],
                  "positions":[
                   {"Bz":"RMB","Cbjg":"54.484","Cbjgex":"51.633","Ckcb":"256074.42","Ckcbj":"54.484",
                    "Ckyk":"-14494.42","Dryk":"","Drykbl":"","Gddm":"0340835149","Ksssl":"4700",
                    "Kysl":"4700","Ljyk":"-14494.42","Market":"SA","Ykbl":"-0.056604",
                    "Zqdm":"300677","Zqmc":"英科医疗","Zqsl":"4700","Zxjg":"51.400","Zxsz":"241580.00"},
                   {"Bz":"RMB","Cbjg":"-16.003","Cbjgex":"3.584","Ckcb":"-19204.05","Ckcbj":"-16.003",
                    "Ckyk":"23524.05","Dryk":"","Drykbl":"","Kysl":"1200","Ljyk":"23524.05","Ykbl":"0.000000",
                    "Zqdm":"600255","Zqmc":"鑫科材料","Zqsl":"1200","Zxjg":"3.600","Zxsz":"4320.00"}]}]
                """;
        when(jywgClient.assetAndPosition()).thenReturn(objectMapper.readTree(json));

        BrokerPositionSnapshotVO snapshot = provider.fetchPositions();

        assertThat(snapshot.getChannel()).isEqualTo("EM_WEB");
        assertThat(snapshot.getTotalAsset()).isEqualByComparingTo("491950.03");
        assertThat(snapshot.getSecurityMarketValue()).isEqualByComparingTo("491815.00");
        assertThat(snapshot.getEnableBalance()).isEqualByComparingTo("135.03");
        assertThat(snapshot.getFetchBalance()).isEqualByComparingTo("135.03");
        assertThat(snapshot.getMoneyBalance()).isEqualByComparingTo("135.03");
        assertThat(snapshot.getPositionIncome()).isEqualByComparingTo("5320.24");
        // 顶层当日盈亏为空字符串 → 解析为 null
        assertThat(snapshot.getDayIncome()).isNull();
        assertThat(snapshot.getPositions()).hasSize(2);

        var first = snapshot.getPositions().get(0);
        assertThat(first.getSecurityCode()).isEqualTo("300677");
        assertThat(first.getSecurityName()).isEqualTo("英科医疗");
        assertThat(first.getHoldingQuantity()).isEqualByComparingTo("4700");
        assertThat(first.getEnableQuantity()).isEqualByComparingTo("4700");
        assertThat(first.getCostPrice()).isEqualByComparingTo("54.484");
        assertThat(first.getLastPrice()).isEqualByComparingTo("51.400");
        assertThat(first.getMarketValue()).isEqualByComparingTo("241580.00");
        assertThat(first.getIncome()).isEqualByComparingTo("-14494.42");
        // Ykbl 小数比例放大 100 倍 = 官网百分比口径
        assertThat(first.getIncomeRate()).isEqualByComparingTo("-5.6604");
        assertThat(first.getDayIncome()).isNull();
        assertThat(first.getDayIncomeRate()).isNull();

        var second = snapshot.getPositions().get(1);
        assertThat(second.getCostPrice()).isEqualByComparingTo("-16.003");
        assertThat(second.getIncome()).isEqualByComparingTo("23524.05");
        assertThat(second.getIncomeRate()).isEqualByComparingTo("0");
    }

    @Test
    void rejectsUnknownTradeDirection() throws Exception {
        when(jywgClient.todayMatches())
                .thenReturn(objectMapper.readTree("[{\"stkcode\":\"600036\",\"bsflag_ex\":\"未知操作\"}]"));
        when(jywgClient.historyMatches(any(), any())).thenReturn(objectMapper.readTree("[]"));

        assertThatThrownBy(() -> provider.fetchTrades())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("暂不支持东财网页成交方向");
    }
}
