package com.brotherc.aquant.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExceptionEnum {

    SYS_ERROR(1000001, "系统异常"),
    SYS_CHECK_ERROR(1000002, "系统校验异常"),

    STOCK_STRATEGY_DUAL_MA_ILLEGAL(1000101, "短期均线必须小于长期均线"),
    STOCK_SYNC_NOT_START(1000102, "非交易日时间无需同步"),
    STOCK_REFRESH_FREQUENT(1000103, "1分钟内请勿重复刷新"),
    STOCK_INDUSTRY_BOARD_UN_EXIST(1000104, "行业板块不存在"),
    STOCK_NOT_FOUND(1000105, "股票代码不存在"),
    WATCHLIST_GROUP_NAME_DUPLICATE(1000106, "分组名称已存在"),
    WATCHLIST_GROUP_NOT_FOUND(1000107, "自选分组不存在"),
    STOCK_NOTIFICATION_PRICE_ALERT_PARAMS_ILLEGAL(1000108, "价格通知参数非法"),
    STOCK_NOTIFICATION_DUPLICATE(1000109, "相同通知已存在"),
    STOCK_NOTIFICATION_STOCK_COUNT_LIMIT(1000110, "平台通知股票数量已达上限"),

    AUTH_LOGIN_FAILED(1000201, "用户名或密码错误"),
    AUTH_ACCOUNT_DISABLED(1000202, "账号已被禁用"),
    AUTH_USERNAME_EXISTS(1000203, "用户名已存在"),
    AUTH_TOKEN_INVALID(1000204, "请先登录"),
    AUTH_USER_NOT_FOUND(1000205, "用户不存在"),
    AUTH_TOKEN_EXPIRED(1000206, "登录已过期，请重新登录"),
    AUTH_EMAIL_NOT_FOUND(1000207, "该邮箱未绑定账号"),
    AUTH_RESET_CODE_INVALID(1000208, "验证码不正确"),
    AUTH_RESET_CODE_EXPIRED(1000209, "验证码已过期"),
    AUTH_RESET_CODE_SEND_TOO_FREQUENT(1000210, "验证码发送过于频繁，请稍后再试"),
    AUTH_MAIL_NOT_CONFIGURED(1000211, "邮件服务未配置"),
    AUTH_RESET_PASSWORD_SAME_AS_OLD(1000212, "新密码不能与原密码相同"),
    AUTH_RESET_CODE_SEND_FAILED(1000213, "验证码发送失败，请稍后重试");

    /**
     * 应用(1~2位)、服务(2位)、模块(2位)、异常(2位)
     */
    private final Integer code;

    /**
     * 异常提示信息
     */
    private final String msg;

    public BusinessException toException() {
        return new BusinessException(this);
    }

    public BusinessException toException(Throwable cause) {
        return new BusinessException(this, cause);
    }

}
