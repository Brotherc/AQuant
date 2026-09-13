package com.brotherc.aquant.portfolio.model.dto;

import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeImportErrorVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeSaveReqVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PortfolioTradeFileParseResult {

    /**
     * 去除路径信息后的原始上传文件名
     */
    private String fileName;

    /**
     * 原始文件内容的 SHA-256 摘要，用于识别重复导入文件
     */
    private String fileHash;

    /**
     * 文件中的非空数据行总数，包含解析成功和解析失败的行
     */
    private int totalCount;

    /**
     * 券商流水中最新业务日期对应的资金余额，标准模板可能不提供
     */
    private BigDecimal latestCashBalance;

    /**
     * 最新资金余额对应的业务日期，用于记录余额的数据时点
     */
    private LocalDate cashBalanceDate;

    /**
     * 最新资金余额的币种，使用 ISO 4217 代码，例如 CNY
     */
    private String cashBalanceCurrency;

    /**
     * 成功解析并转换为系统标准格式的交易及资金流水
     */
    private List<PortfolioTradeSaveReqVO> trades = new ArrayList<>();

    /**
     * 解析或字段校验失败的数据行及错误原因
     */
    private List<PortfolioTradeImportErrorVO> errors = new ArrayList<>();

}
