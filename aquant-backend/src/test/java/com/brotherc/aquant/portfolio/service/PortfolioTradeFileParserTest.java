package com.brotherc.aquant.portfolio.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.portfolio.model.dto.PortfolioTradeFileParseResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortfolioTradeFileParserTest {

    private final PortfolioTradeFileParser parser = new PortfolioTradeFileParser();

    @Test
    void shouldParseUtf8CsvAndQuotedValue() {
        String csv = "资产类型,市场,证券代码,证券名称,交易类型,交易时间,数量,成交价格,佣金,备注\n"
                + "股票,上交所,600000,浦发银行,买入,2026-09-10 10:30:00,100,10.5,5,\"首次买入,标准模板\"\n";

        PortfolioTradeFileParseResult result = parser.parse("trade.csv", csv.getBytes(StandardCharsets.UTF_8));

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getTrades()).singleElement().satisfies(trade -> {
            assertThat(trade.getAssetType()).isEqualTo("STOCK");
            assertThat(trade.getMarket()).isEqualTo("SH");
            assertThat(trade.getTradeType()).isEqualTo("BUY");
            assertThat(trade.getQuantity()).isEqualByComparingTo("100");
            assertThat(trade.getRemark()).isEqualTo("首次买入,标准模板");
        });
    }

    @Test
    void shouldCollectInvalidRowsWithoutAcceptingThem() {
        String csv = "资产类型,交易类型,交易时间,证券代码,数量,成交价格\n"
                + "股票,买入,2026-09-10,600000,100,10\n"
                + "股票,未知类型,2026-09-10,600001,100,10\n";

        PortfolioTradeFileParseResult result = parser.parse("trade.csv", csv.getBytes(StandardCharsets.UTF_8));

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getTrades()).hasSize(1);
        assertThat(result.getErrors()).singleElement().satisfies(error -> {
            assertThat(error.getRowNumber()).isEqualTo(3);
            assertThat(error.getMessage()).contains("交易类型不支持");
        });
    }

    @Test
    void shouldParseExcelDateCells() throws Exception {
        byte[] content;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("资产类型");
            header.createCell(1).setCellValue("交易类型");
            header.createCell(2).setCellValue("交易时间");
            header.createCell(3).setCellValue("交收日期");
            header.createCell(4).setCellValue("证券代码");
            header.createCell(5).setCellValue("数量");
            header.createCell(6).setCellValue("成交价格");
            Row data = sheet.createRow(1);
            CreationHelper creationHelper = workbook.getCreationHelper();
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));
            data.createCell(0).setCellValue("基金");
            data.createCell(1).setCellValue("申购");
            data.createCell(2).setCellValue(LocalDate.of(2026, 9, 10));
            data.getCell(2).setCellStyle(dateStyle);
            data.createCell(3).setCellValue(LocalDate.of(2026, 9, 11));
            data.getCell(3).setCellStyle(dateStyle);
            data.createCell(4).setCellValue("006479");
            data.createCell(5).setCellValue(100);
            data.createCell(6).setCellValue(1.5);
            workbook.write(output);
            content = output.toByteArray();
        }

        PortfolioTradeFileParseResult result = parser.parse("trade.xlsx", content);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getTrades().get(0).getSettlementDate()).isEqualTo(LocalDate.of(2026, 9, 11));
    }

    @Test
    void shouldRejectUnsupportedFile() {
        assertThatThrownBy(() -> parser.parse("trade.txt", "test".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持");
    }
}
