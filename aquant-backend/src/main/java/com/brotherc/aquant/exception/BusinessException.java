package com.brotherc.aquant.exception;

public class BusinessException extends RuntimeException {

    private final Integer code;

    private final String msg;

    public BusinessException(ExceptionEnum exceptionEnum) {
        super(exceptionEnum.getMsg());
        this.code = exceptionEnum.getCode();
        this.msg = exceptionEnum.getMsg();
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

}
