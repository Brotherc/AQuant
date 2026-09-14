package com.brotherc.aquant.integration.eastmoney.service;

/**
 * 东财行情主机熔断冷却中：请求被网关直接拒绝（未发出）。
 * 独立于业务异常，便于调用方识别后立即放弃本轮同步——冷却期内的退避重试毫无意义，
 * 只会白白阻塞调用线程（东财封禁期内的重试还会刷新封禁窗口）
 */
public class EastmoneyCoolingDownException extends RuntimeException {

    public EastmoneyCoolingDownException(String message) {
        super(message);
    }

    public EastmoneyCoolingDownException(String host, long remainingMillis) {
        super("东财行情主机熔断冷却中(未发请求): host=" + host + ", 剩余=" + remainingMillis + "ms");
    }
}
