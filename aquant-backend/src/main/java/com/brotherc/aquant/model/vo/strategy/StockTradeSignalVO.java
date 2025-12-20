package com.brotherc.aquant.model.vo.strategy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockTradeSignalVO {

    /**
     * 股票代码
     */
    private String code;

    /**
     * 股票名称
     */
    private String name;

    /**
     * 交易信号
     */
    private String signal;

}
