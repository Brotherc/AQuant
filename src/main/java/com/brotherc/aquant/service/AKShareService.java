package com.brotherc.aquant.service;

import com.brotherc.aquant.model.dto.akshare.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AKShareService {

    @Value("${akshare-address}")
    private String akshareAddress;

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    public List<StockZhASpot> stockZhASpot() {
        Request request = new Request.Builder()
                .url(akshareAddress + "/api/public/stock_zh_a_spot")
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.info("失败响应: {}", response);
                throw new RuntimeException("stock_zh_a_spot请求失败");
            }

            return objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });

        } catch (IOException e) {
            log.error("stock_zh_a_hist请求失败", e);
            throw new RuntimeException("stock_zh_a_hist请求失败");
        }
    }

    public List<StockZhValuationComparisonEm> stockZhValuationComparisonEm(String symbol) {
        Request request = new Request.Builder()
                .url(akshareAddress + "/api/public/stock_zh_valuation_comparison_em?symbol=" + symbol.toUpperCase())
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.info("失败响应: {}", response);
                throw new RuntimeException("stock_zh_valuation_comparison_em请求失败");
            }

            return objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });

        } catch (IOException e) {
            log.error("stock_zh_valuation_comparison_em请求失败", e);
            throw new RuntimeException("stock_zh_valuation_comparison_em请求失败");
        }
    }

    public List<StockZhGrowthComparisonEm> stockZhGrowthComparisonEm(String symbol) {
        Request request = new Request.Builder()
                .url(akshareAddress + "/api/public/stock_zh_growth_comparison_em?symbol=" + symbol.toUpperCase())
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.info("失败响应: {}", response);
                throw new RuntimeException("stock_zh_growth_comparison_em请求失败");
            }

            return objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });

        } catch (IOException e) {
            log.error("stock_zh_growth_comparison_em请求失败", e);
            throw new RuntimeException("stock_zh_growth_comparison_em请求失败");
        }
    }

    public List<StockZhDupontComparisonEm> stockZhDupontComparisonEm(String symbol) {
        Request request = new Request.Builder()
                .url(akshareAddress + "/api/public/stock_zh_dupont_comparison_em?symbol=" + symbol.toUpperCase())
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.info("失败响应: {}", response);
                throw new RuntimeException("stock_zh_dupont_comparison_em请求失败");
            }

            return objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });

        } catch (IOException e) {
            log.error("stock_zh_dupont_comparison_em请求失败", e);
            throw new RuntimeException("stock_zh_dupont_comparison_em请求失败");
        }
    }

    public List<StockZhAHist> stockZhAHist(String symbol) {
        symbol = symbol.substring(2);

        Request request = new Request.Builder()
                .url(akshareAddress + "/api/public/stock_zh_a_hist?symbol=" + symbol + "&&adjust=hfq")
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.info("失败响应: {}", response);
                throw new RuntimeException("stock_zh_a_hist请求失败");
            }

            return objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });

        } catch (IOException e) {
            log.error("stock_zh_a_hist请求失败", e);
            throw new RuntimeException("stock_zh_a_hist请求失败");
        }
    }

    public List<StockZhADaily> stockZhADaily(String symbol) {
        Request request = new Request.Builder()
                .url(akshareAddress + "/api/public/stock_zh_a_daily?symbol=" + symbol + "&&adjust=hfq")
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.info("失败响应: {}", response);
                throw new RuntimeException("stock_zh_a_daily请求失败");
            }

            return objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });

        } catch (IOException e) {
            log.error("stock_zh_a_daily请求失败", e);
            throw new RuntimeException("stock_zh_a_daily请求失败");
        }
    }

}
