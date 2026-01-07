package com.brotherc.aquant.service;

import com.brotherc.aquant.model.dto.akshare.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    /**
     * 历史行情数据-新浪
     * <p>
     * <a href="https://finance.sina.com.cn/realstock/company/sh600006/nc.shtml">...</a>
     * 新浪财经-沪深京 A 股的数据, 历史数据按日频率更新
     *
     * @param symbol 股票代码
     * @return 历史行情日频率数据
     */
    public List<StockZhADaily> stockZhADaily(String symbol, String startDate, String endDate, String adjust) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_zh_a_daily")
                .newBuilder()
                .addQueryParameter("symbol", symbol);

        if (StringUtils.isNotBlank(startDate)) {
            builder.addQueryParameter("start_date", startDate);
        }
        if (StringUtils.isNotBlank(endDate)) {
            builder.addQueryParameter("end_date", endDate);
        }
        if (StringUtils.isNotBlank(adjust)) {
            builder.addQueryParameter("adjust", adjust);
        }

        Request request = new Request.Builder()
                .url(builder.build())
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

    public List<StockHistoryDividend> stockHistoryDividend() {
        Request request = new Request.Builder()
                .url(akshareAddress + "/api/public/stock_history_dividend")
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.info("失败响应: {}", response);
                throw new RuntimeException("stock_history_dividend请求失败");
            }

            return objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });

        } catch (IOException e) {
            log.error("stock_history_dividend请求失败", e);
            throw new RuntimeException("stock_history_dividend请求失败");
        }
    }

    public List<StockBoardIndustryNameEm> stockBoardIndustryNameEm() {
        Request request = new Request.Builder()
                .url(akshareAddress + "/api/public/stock_board_industry_name_em")
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.info("失败响应: {}", response);
                throw new RuntimeException("stock_board_industry_name_em请求失败");
            }

            return objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });

        } catch (IOException e) {
            log.error("stock_board_industry_name_em请求失败", e);
            throw new RuntimeException("stock_board_industry_name_em请求失败");
        }
    }

    public StockBoardIndustrySpotEm stockBoardIndustrySpotEm(String symbol) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_board_industry_spot_em")
                .newBuilder()
                .addQueryParameter("symbol", symbol);

        Request request = new Request.Builder()
                .url(builder.build())
                .get()
                .build();

        List<Map<String, Object>> list;

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.info("失败响应: {}", response);
                throw new RuntimeException("stock_board_industry_spot_em请求失败");
            }

            list = objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });

        } catch (IOException e) {
            log.error("stock_board_industry_spot_em请求失败", e);
            throw new RuntimeException("stock_board_industry_spot_em请求失败");
        }

        return new StockBoardIndustrySpotEm(list);
    }

    public List<StockBoardIndustryHistEm> stockBoardIndustryHistEm(String symbol, String startDate, String endDate, String adjust) {
        HttpUrl.Builder builder = HttpUrl.parse(akshareAddress + "/api/public/stock_board_industry_hist_em")
                .newBuilder()
                .addQueryParameter("symbol", symbol);

        if (StringUtils.isNotBlank(startDate)) {
            builder.addQueryParameter("start_date", startDate);
        }
        if (StringUtils.isNotBlank(endDate)) {
            builder.addQueryParameter("end_date", endDate);
        }
        if (StringUtils.isNotBlank(adjust)) {
            builder.addQueryParameter("adjust", adjust);
        }

        Request request = new Request.Builder()
                .url(builder.build())
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.info("失败响应: {}", response);
                throw new RuntimeException("stock_board_industry_hist_em请求失败");
            }

            return objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });

        } catch (IOException e) {
            log.error("stock_board_industry_hist_em请求失败", e);
            throw new RuntimeException("stock_board_industry_hist_em请求失败");
        }
    }

}
