package com.brotherc.aquant.exception;

public class BusinessException extends RuntimeException {

    private final Integer code;

    private final String msg;

    public BusinessException(ExceptionEnum exceptionEnum) {
        super(exceptionEnum.getMsg());
        this.code = exceptionEnum.getCode();
        this.msg = exceptionEnum.getMsg();
    }

    public BusinessException(ExceptionEnum exceptionEnum, Throwable cause) {
        super(exceptionEnum.getMsg(), cause);
        this.code = exceptionEnum.getCode();
        this.msg = exceptionEnum.getMsg();
    }

    /**
     * 使用自定义消息（一般用于 ExceptionEnum 中模板消息的格式化）
     */
    public BusinessException(ExceptionEnum exceptionEnum, String customMsg) {
        super(customMsg);
        this.code = exceptionEnum.getCode();
        this.msg = customMsg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

}
