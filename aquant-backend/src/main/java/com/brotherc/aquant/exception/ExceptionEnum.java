package com.brotherc.aquant.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExceptionEnum {

    SYS_ERROR(1000001, "系统异常"),
    SYS_CHECK_ERROR(1000002, "系统校验异常"),

    STOCK_STRATEGY_DUAL_MA_ILLEGAL(1000101, "短期均线必须小于长期均线")
    ;

    /**
     * 应用(1~2位)、服务(2位)、模块(2位)、异常(2位)
     */
    private Integer code;

    /**
     * 异常提示信息
     */
    private String msg;

    public void throwException() {
        throw new BusinessException(this);
    }

}
