package com.brotherc.aquant.common.exception;

/**
 * 外部接口确认暂无该标的数据（如上市首日新股尚无日线），重试无法恢复，
 * 调用方应跳过重试，等待下次同步触发时按水位自动补齐。
 */
public class ExternalDataNotFoundException extends BusinessException {

    public ExternalDataNotFoundException() {
        super(ExceptionEnum.API_DATA_NOT_FOUND);
    }
}
