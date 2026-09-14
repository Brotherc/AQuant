package com.brotherc.aquant.portfolio.service.importer;

import com.brotherc.aquant.portfolio.model.dto.PortfolioTradeFileParseResult;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CMSSecuritiesTradeFileAdapterTest {

    private final CMSSecuritiesTradeFileAdapter adapter = new CMSSecuritiesTradeFileAdapter();

    @Test
    void shouldRecognizeTextXlsAndMapCmsBusinessTypes() {
        byte[] content = fileContent().getBytes(Charset.forName("GB18030"));

        assertThat(adapter.supports("CMS", "资金流水.xls", content)).isTrue();
        PortfolioTradeFileParseResult result = adapter.parse("资金流水.xls", content);

        assertThat(result.getTotalCount()).isEqualTo(8);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getLatestCashBalance()).isEqualByComparingTo("888.88");
        assertThat(result.getCashBalanceDate()).isEqualTo(java.time.LocalDate.of(2026, 9, 2));
        assertThat(result.getCashBalanceCurrency()).isEqualTo("CNY");
        Map<String, PortfolioTradeSaveReqVO> trades = result.getTrades().stream()
                .collect(Collectors.toMap(PortfolioTradeSaveReqVO::getSourceTradeId, trade -> trade));
        assertThat(trades.get("1001").getTradeType()).isEqualTo("BUY");
        assertThat(trades.get("1001").getGrossAmount()).isEqualByComparingTo("3004");
        assertThat(trades.get("1001").getOtherFee()).isEqualByComparingTo("0.16");
        assertThat(trades.get("1001").getTransferFee()).isEqualByComparingTo("0.03");
        assertThat(trades.get("1002").getTradeType()).isEqualTo("SELL");
        assertThat(trades.get("1002").getQuantity()).isEqualByComparingTo("100");
        assertThat(trades.get("1003").getTradeType()).isEqualTo("DIVIDEND_CASH");
        assertThat(trades.get("1004").getTradeType()).isEqualTo("TAX");
        assertThat(trades.get("1005").getTradeType()).isEqualTo("CASH_DEPOSIT");
        assertThat(trades.get("1006").getTradeType()).isEqualTo("CASH_WITHDRAW");
        assertThat(trades.get("1008").getTradeType()).isEqualTo("TRANSFER_IN");
        assertThat(trades.get("1008").getGrossAmount()).isEqualByComparingTo("1000");
    }

    private String fileContent() {
        return "\r\n=" + quote("币种") + "\t=" + quote("证券名称") + "\t=" + quote("成交日期")
                + "\t=" + quote("成交价格") + "\t=" + quote("成交数量") + "\t=" + quote("发生金额")
                + "\t=" + quote("资金余额")
                + "\t=" + quote("合同编号") + "\t=" + quote("流水号") + "\t=" + quote("业务名称")
                + "\t=" + quote("印花税") + "\t=" + quote("佣金") + "\t=" + quote("经手费")
                + "\t=" + quote("证管费") + "\t=" + quote("结算费") + "\t=" + quote("过户费")
                + "\t=" + quote("其他费用") + "\t=" + quote("证券代码") + "\t=" + quote("备注") + "\r\n"
                + row("人民币", "盛新锂能", "20260902", "30.04", "100", "-3009", "748699", "1001", "证券买入", "0", "4.81", "0.10", "0.06", "0", "0.03", "0", "002240", "")
                + row("人民币", "中远海控", "20260821", "17", "-100", "1694.13", "1374955", "1002", "证券卖出", "0.85", "4.91", "0.06", "0.03", "0", "0.02", "0", "601919", "")
                + row("人民币", "中远海控", "20260625", "13.82", "0", "176", "0", "1003", "股息入账", "0", "0", "0", "0", "0", "0", "0", "601919", "")
                + row("人民币", "中远海控", "20260824", "0", "0", "-4.4", "0", "1004", "股息红利税补缴", "0", "0", "0", "0", "0", "0", "0", "601919", "")
                + row("人民币", "", "20260723", "0", "0", "6000", "0", "1005", "银行转存", "0", "0", "0", "0", "0", "0", "0", "", "")
                + row("人民币", "", "20260612", "0", "0", "-8405.71", "0", "1006", "银行转取", "0", "0", "0", "0", "0", "0", "0", "", "")
                + row("人民币", "颀中发债", "20251106", "100", "1", "-1000", "1022729", "1007", "新股申购确认缴款", "0", "0", "0", "0", "0", "0", "0", "718352", "")
                + row("人民币", "颀中转债", "20251120", "100", "1", "0", "1022729", "1008", "新股入账", "0", "0", "0", "0", "0", "0", "0", "118059", "");
    }

    private String row(String... values) {
        java.util.List<String> cells = new java.util.ArrayList<>(java.util.Arrays.asList(values));
        cells.add(6, "888.88");
        return cells.stream().map(value -> "=" + quote(value)).collect(Collectors.joining("\t")) + "\r\n";
    }

    private String quote(String value) {
        return "\"" + value + "\"";
    }
}
