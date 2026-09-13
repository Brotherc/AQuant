package com.brotherc.aquant.portfolio.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioTradeImportErrorVO {

    /**
     * 错误数据在上传文件中的实际行号，包含表头所在行
     */
    private Integer rowNumber;

    /**
     * 该行数据解析或字段校验失败的具体原因
     */
    private String message;

}
