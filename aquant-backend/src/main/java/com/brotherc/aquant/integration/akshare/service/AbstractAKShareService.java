package com.brotherc.aquant.integration.akshare.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.List;

/**
 * AKShare 请求基础抽象类
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAKShareService {

    protected static final String SYMBOL = "symbol";

    @Value("${akshare-address}")
    protected String akshareAddress;

    protected final ObjectMapper objectMapper;
    protected final OkHttpClient okHttpClient;

    /**
     * 从 Request 的 URL 中自动解析并截取 API 名称（取 Path 的最后一个 segment，如 stock_zh_a_spot）
     */
    protected String extractApiName(Request request) {
        List<String> segments = request.url().pathSegments();
        if (!segments.isEmpty()) {
            return segments.get(segments.size() - 1);
        }
        return request.url().encodedPath();
    }

    /**
     * 通用 HTTP 请求处理：执行 Request 并自动转为目标 Java 对象
     */
    protected <T> T executeRequest(Request request, TypeReference<T> typeReference) {
        String apiName = extractApiName(request);
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.info("{} 失败响应: {}", apiName, response);
                throw new BusinessException(ExceptionEnum.API_REQUEST_ERROR);
            }
            return objectMapper.readValue(response.body().string(), typeReference);
        } catch (IOException e) {
            log.error("{} 请求失败", apiName, e);
            throw new BusinessException(ExceptionEnum.API_REQUEST_ERROR);
        }
    }

    /**
     * 通用 GET 请求重载（接收 String URL）
     */
    protected <T> T executeGet(String url, TypeReference<T> typeReference) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return executeRequest(request, typeReference);
    }

    /**
     * 通用 GET 请求重载（接收 HttpUrl 构造器）
     */
    protected <T> T executeGet(HttpUrl httpUrl, TypeReference<T> typeReference) {
        Request request = new Request.Builder()
                .url(httpUrl)
                .get()
                .build();
        return executeRequest(request, typeReference);
    }

}
