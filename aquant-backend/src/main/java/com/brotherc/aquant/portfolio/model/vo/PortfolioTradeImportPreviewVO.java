package com.brotherc.aquant.portfolio.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PortfolioTradeImportPreviewVO {

    @Schema(description = "去除路径信息后的原始上传文件名")
    private String fileName;

    @Schema(description = "原始文件内容的 SHA-256 摘要，用于识别重复导入文件")
    private String fileHash;

    @Schema(description = "文件中的非空数据行总数，包含有效行和错误行")
    private Integer totalCount;

    @Schema(description = "成功解析并通过字段校验的数据行数")
    private Integer validCount;

    @Schema(description = "解析或字段校验失败的数据行数")
    private Integer errorCount;

    @Schema(description = "当前券商账户是否已经成功导入过内容完全相同的文件")
    private Boolean alreadyImported;

    @Schema(description = "券商流水中最新业务日期对应的资金余额；标准模板未提供时为空")
    private BigDecimal latestCashBalance;

    @Schema(description = "最新资金余额对应的业务日期")
    private LocalDate cashBalanceDate;

    @Schema(description = "最新资金余额币种，使用 ISO 4217 代码")
    private String cashBalanceCurrency;

    @Schema(description = "成功解析的交易流水样例，最多返回前 20 条")
    private List<PortfolioTradeSaveReqVO> sampleTrades;

    @Schema(description = "解析或字段校验错误明细，最多返回前 100 条")
    private List<PortfolioTradeImportErrorVO> errors;

}
