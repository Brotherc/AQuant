package com.brotherc.aquant.portfolio.service.importer;

import com.brotherc.aquant.portfolio.model.dto.PortfolioTradeFileParseResult;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EastmoneyTradeFileAdapterTest {

    private final EastmoneyTradeFileAdapter adapter = new EastmoneyTradeFileAdapter();

    private static final List<String> HEADERS = List.of(
            "交割日期", "业务名称", "证券代码", "证券名称", "成交价格", "成交数量", "成交金额", "发生金额",
            "手续费", "印花税", "过户费", "资金余额", "币种", "合同编号", "备注");

    @Test
    void supportsOnlyEastmoneyStatements() throws Exception {
        assertThat(adapter.supports("EM", "交割单.xlsx", xlsx())).isTrue();
        // 兼容前端券商选择器的 EASTMONEY 写法
        assertThat(adapter.supports("EASTMONEY", "交割单.xlsx", xlsx())).isTrue();
        assertThat(adapter.supports("CMS", "交割单.xlsx", xlsx())).isFalse();
        assertThat(adapter.supports("EM", "other.xlsx", new byte[]{1, 2, 3})).isFalse();
    }

    @Test
    void parsesMultiSecurityStatementWithBusinessTypes() throws Exception {
        PortfolioTradeFileParseResult result = adapter.parse("交割单.xlsx", xlsx());

        assertThat(result.getTotalCount()).isEqualTo(5);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getRowNumber()).isEqualTo(6);

        List<PortfolioTradeSaveReqVO> trades = result.getTrades();
        assertThat(trades).hasSize(4);

        PortfolioTradeSaveReqVO buy = trades.get(0);
        assertThat(buy.getTradeType()).isEqualTo("BUY");
        assertThat(buy.getAssetType()).isEqualTo("STOCK");
        assertThat(buy.getMarket()).isEqualTo("SH");
        assertThat(buy.getAssetCode()).isEqualTo("600519");
        assertThat(buy.getQuantity()).isEqualByComparingTo("100");
        assertThat(buy.getGrossAmount()).isEqualByComparingTo("150000");
        // 买入发生金额为负数（现金扣减），原值保留
        assertThat(buy.getNetAmount()).isEqualByComparingTo("-150030");
        assertThat(buy.getCommission()).isEqualByComparingTo("150");
        assertThat(buy.getSourceTradeId()).isEqualTo("HT001");

        PortfolioTradeSaveReqVO sell = trades.get(1);
        assertThat(sell.getTradeType()).isEqualTo("SELL");
        assertThat(sell.getAssetType()).isEqualTo("ETF");
        // 510300 沪深300ETF 为沪市品种（51 开头）
        assertThat(sell.getMarket()).isEqualTo("SH");
        assertThat(sell.getNetAmount()).isEqualByComparingTo("3990");
        assertThat(sell.getRemark()).contains("部分止盈");

        PortfolioTradeSaveReqVO deposit = trades.get(2);
        assertThat(deposit.getTradeType()).isEqualTo("CASH_DEPOSIT");
        assertThat(deposit.getAssetType()).isEqualTo("CASH");
        assertThat(deposit.getGrossAmount()).isEqualByComparingTo("10000");

        PortfolioTradeSaveReqVO dividend = trades.get(3);
        assertThat(dividend.getTradeType()).isEqualTo("DIVIDEND_CASH");
        assertThat(dividend.getAssetCode()).isEqualTo("601398");

        // 最新交割日期行的资金余额
        assertThat(result.getLatestCashBalance()).isEqualByComparingTo("64490");
        assertThat(result.getCashBalanceCurrency()).isEqualTo("CNY");
        assertThat(result.getCashBalanceDate().toString()).isEqualTo("2026-09-16");
    }

    @Test
    void parsesGb18030CsvStatement() {
        String csv = String.join("\n",
                "交割日期,业务名称,证券代码,证券名称,成交价格,成交数量,成交金额,发生金额,手续费,资金余额,币种,合同编号",
                "2026-09-16,证券买入,600036,招商银行,35.00,200,7000.00,-7010.00,10.00,92990.00,人民币,HT101",
                "2026-09-16,证券卖出,159915,创业板ETF,2.50,5000,12500.00,12490.00,10.00,105480.00,人民币,HT102");
        byte[] content = csv.getBytes(Charset.forName("GB18030"));

        assertThat(adapter.supports("EM", "对账单.csv", content)).isTrue();
        PortfolioTradeFileParseResult result = adapter.parse("对账单.csv", content);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getTrades()).hasSize(2);
        assertThat(result.getTrades().get(0).getMarket()).isEqualTo("SH");
        assertThat(result.getTrades().get(1).getAssetType()).isEqualTo("ETF");
        assertThat(result.getLatestCashBalance()).isEqualByComparingTo("105480");
    }

    private byte[] xlsx() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("交割单");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                header.createCell(i).setCellValue(HEADERS.get(i));
            }
            writeRow(sheet, 1, "2026-09-16", "证券买入", "600519", "贵州茅台", "1500.00", "100", "150000.00",
                    "-150030.00", "150.00", "1500.00", "30.00", "50000.00", "人民币", "HT001", "");
            writeRow(sheet, 2, "2026-09-16", "证券卖出", "510300", "沪深300ETF", "4.00", "1000", "4000.00",
                    "3990.00", "5.00", "4.00", "1.00", "53990.00", "人民币", "HT002", "部分止盈");
            writeRow(sheet, 3, "2026-09-16", "银行转存", "", "", "", "", "", "10000.00", "", "", "",
                    "63990.00", "人民币", "HT003", "银证转入");
            writeRow(sheet, 4, "2026-09-16", "股息入账", "601398", "工商银行", "", "", "", "500.00", "", "", "",
                    "64490.00", "人民币", "HT004", "现金分红");
            writeRow(sheet, 5, "2026-09-16", "测试业务", "600000", "浦发银行", "1.00", "100", "100.00",
                    "-101.00", "1.00", "", "", "64490.00", "人民币", "HT005", "");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void writeRow(Sheet sheet, int rowIndex, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(String.valueOf(values[i]));
        }
    }
}
