package com.brotherc.aquant.integration.easytrader;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * easytrader 远程客户端（https://github.com/shidenggui/easytrader 的 Flask 服务模式）。
 *
 * <p>用户在常驻 Windows 机器上以东财证券客户端启动 easytrader 的 web 服务，
 * 本客户端轮询读取持仓与当日成交。网格数据列名随券商客户端版本变化，调用方需做宽容字段映射。
 * 典型部署：{@code python -m easytrader.server}（或按其文档以 server 模式运行），
 * 先 POST /prepare 完成客户端登录，再 GET 以下数据端点。</p>
 */
@Slf4j
@Service
public class EasytraderClient {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public EasytraderClient(OkHttpClient okHttpClient, ObjectMapper objectMapper,
                            @Value("${aquant.portfolio.easytrader-base-url:}") String baseUrl) {
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    public boolean isConfigured() {
        return !baseUrl.isBlank();
    }

    /** 当前持仓（多证券列表，列名随券商客户端变化，如 证券代码/证券名称/股票余额/成本价/市值） */
    public List<Map<String, Object>> position() {
        return fetch("/position");
    }

    /** 当日成交（多证券列表，常见列：成交日期/证券代码/证券名称/操作/成交价格/成交数量/成交金额/合同编号/成交编号） */
    public List<Map<String, Object>> todayTrades() {
        return fetch("/today_trades");
    }

    /** 资金余额（可用金额/总资产等，列名随客户端变化） */
    public Map<String, Object> balance() {
        List<Map<String, Object>> rows = fetch("/balance");
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private List<Map<String, Object>> fetch(String path) {
        if (!isConfigured()) {
            throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                    "未配置 easytrader 服务地址（aquant.portfolio.easytrader-base-url）");
        }
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .get()
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                String error = parseError(body);
                log.warn("easytrader 接口失败: path={}, status={}, error={}", path, response.code(), error);
                throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                        "easytrader 客户端调用失败：" + StringUtils.defaultIfBlank(error, "HTTP " + response.code()));
            }
            return objectMapper.readValue(body, new TypeReference<>() {});
        } catch (IOException e) {
            log.warn("easytrader 接口不可达: path={}, base={}", path, baseUrl, e);
            throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                    "easytrader 服务不可达，请确认客户端服务已启动：" + e.getMessage());
        }
    }

    private String parseError(String body) {
        try {
            return objectMapper.readTree(body).path("error").asText(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static final class StringUtils {
        private static String defaultIfBlank(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }
}
