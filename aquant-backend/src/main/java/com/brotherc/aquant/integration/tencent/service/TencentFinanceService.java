package com.brotherc.aquant.integration.tencent.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.integration.tencent.model.TencentMinuteQuote;
import com.brotherc.aquant.integration.tencent.model.TencentOrderBook;
import com.brotherc.aquant.integration.tencent.model.TencentStockQuote;
import com.brotherc.aquant.common.utils.StockUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 腾讯财经行情服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TencentFinanceService {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    /**
     * 获取个股当日分时数据 (腾讯 ifzq 接口)
     *
     * @param code 股票代码，带交易所前缀，如 sh600519
     * @return 分时数据（含日期、名称、昨收与逐分钟价格/累计量/累计额）
     */
    public TencentMinuteQuote fetchMinute(String code) {
        HttpUrl httpUrl = HttpUrl.get("http://web.ifzq.gtimg.cn/appstock/app/minute/query")
                .newBuilder()
                .addQueryParameter("code", code)
                .build();
        Request request = new Request.Builder()
                .url(httpUrl)
                .header("Referer", "http://gu.qq.com")
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("腾讯分时接口响应异常: status={}, symbol={}", response.code(), code);
                throw ExceptionEnum.API_REQUEST_ERROR.toException();
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            JsonNode minuteNode = root.path("data").path(code).path("data");
            JsonNode rows = minuteNode.path("data");
            JsonNode qt = root.path("data").path(code).path("qt").path(code);
            if (!rows.isArray() || rows.isEmpty() || qt.path(4).isMissingNode()) {
                log.warn("腾讯分时接口数据缺失: symbol={}, bodySize={}", code, rows.size());
                throw ExceptionEnum.API_REQUEST_ERROR.toException();
            }

            TencentMinuteQuote quote = new TencentMinuteQuote();
            quote.setCode(code);
            quote.setDate(formatTradeDate(minuteNode.path("date").asText()));
            quote.setName(qt.path(1).asText());
            quote.setPrevClose(new BigDecimal(qt.path(4).asText()));

            for (JsonNode rowNode : rows) {
                String[] parts = rowNode.asText().trim().split("\\s+");
                if (parts.length < 4) {
                    continue;
                }
                TencentMinuteQuote.Point point = new TencentMinuteQuote.Point();
                String rawTime = parts[0];
                point.setTime(rawTime.substring(0, 2) + ":" + rawTime.substring(2));
                point.setPrice(new BigDecimal(parts[1]));
                point.setCumVolume(new BigDecimal(parts[2]));
                point.setCumAmount(new BigDecimal(parts[3]));
                quote.getPoints().add(point);
            }
            return quote;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("腾讯分时接口请求/解析失败, symbol={}", code, e);
            throw ExceptionEnum.API_REQUEST_ERROR.toException(e);
        }
    }

    /**
     * "yyyyMMdd" 转 "yyyy-MM-dd"
     */
    private String formatTradeDate(String raw) {
        if (raw == null || raw.length() != 8) {
            return raw;
        }
        return raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8);
    }

    /**
     * 获取个股实时盘口 (腾讯 qt.gtimg.cn 行情接口，含五档买卖盘与成交信息)
     *
     * @param code 股票代码，带交易所前缀，如 sh600519
     * @return 实时盘口
     */
    public TencentOrderBook fetchOrderBook(String code) {
        HttpUrl httpUrl = HttpUrl.get("http://qt.gtimg.cn/q=" + code).newBuilder().build();
        Request request = new Request.Builder()
                .url(httpUrl)
                .header("Referer", "http://gu.qq.com")
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("腾讯盘口接口响应异常: status={}, symbol={}", response.code(), code);
                throw ExceptionEnum.API_REQUEST_ERROR.toException();
            }

            String body = response.body().string();
            int firstQuote = body.indexOf("\"");
            int lastQuote = body.lastIndexOf("\"");
            if (firstQuote < 0 || lastQuote <= firstQuote) {
                log.warn("腾讯盘口接口响应格式异常: symbol={}", code);
                throw ExceptionEnum.API_REQUEST_ERROR.toException();
            }
            String[] parts = body.substring(firstQuote + 1, lastQuote).split("~");
            if (parts.length < 30 || parts[3].isBlank()) {
                log.warn("腾讯盘口接口数据缺失: symbol={}, parts={}", code, parts.length);
                throw ExceptionEnum.API_REQUEST_ERROR.toException();
            }

            TencentOrderBook book = new TencentOrderBook();
            book.setCode(code);
            book.setName(parts[1]);
            book.setLatestPrice(new BigDecimal(parts[3]));
            book.setPrevClose(new BigDecimal(parts[4]));
            book.setOpen(new BigDecimal(parts[5]));
            book.setVolume(new BigDecimal(parts[6]));
            for (int i = 0; i < 5; i++) {
                book.getBids().add(new TencentOrderBook.Level(
                        new BigDecimal(parts[9 + i * 2]), new BigDecimal(parts[10 + i * 2])));
                book.getAsks().add(new TencentOrderBook.Level(
                        new BigDecimal(parts[19 + i * 2]), new BigDecimal(parts[20 + i * 2])));
            }
            book.setQuoteTime(parts[30]);
            book.setChange(new BigDecimal(parts[31]));
            book.setChangePercent(new BigDecimal(parts[32]));
            book.setHigh(new BigDecimal(parts[33]));
            book.setLow(new BigDecimal(parts[34]));
            book.setTurnover(new BigDecimal(parts[37]));
            book.setTurnoverRate(new BigDecimal(parts[38]));
            if (parts.length > 49 && !parts[49].isBlank()) {
                book.setQuantityRatio(new BigDecimal(parts[49]));
            }
            return book;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("腾讯盘口接口请求/解析失败, symbol={}", code, e);
            throw ExceptionEnum.API_REQUEST_ERROR.toException(e);
        }
    }

    /**
     * 批量获取实时股票行情 (腾讯接口)
     *
     * @param stockCodes 股票代码列表 (例如: ["600519", "000001"])
     * @return 股票代码与行情数据的映射 Map
     */
    public Map<String, TencentStockQuote> fetchBatchQuotes(List<String> stockCodes) {
        Map<String, TencentStockQuote> resultMap = new HashMap<>();
        if (CollectionUtils.isEmpty(stockCodes)) {
            return resultMap;
        }

        // 腾讯接口建议每批次不超过 200 个，以符合 URL 长度限制并降低封禁风险
        int batchSize = 200;
        for (int i = 0; i < stockCodes.size(); i += batchSize) {
            List<String> batchCodes = stockCodes.subList(i, Math.min(i + batchSize, stockCodes.size()));
            String symbols = batchCodes.stream()
                    .map(StockUtils::wrapExchangePrefix)
                    .collect(Collectors.joining(","));

            String url = "http://qt.gtimg.cn/q=" + symbols;
            Request request = new Request.Builder()
                    .url(url)
                    .header("Referer", "http://gu.qq.com")
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    parseTencentResponse(body, resultMap);
                } else {
                    log.warn("腾讯行情接口响应异常: code={}, symbols={}", response.code(), symbols);
                }
            } catch (Exception e) {
                log.error("尝试从腾讯批量获取行情失败: symbols={}, error={}", symbols, e.getMessage());
            }

            // 如果还有下批次的数据，休眠 1 秒以规避高频封禁风险
            if (i + batchSize < stockCodes.size()) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    log.warn("批量获取行情过程中的休眠被中断");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return resultMap;
    }

    /**
     * 解析腾讯行情接口返回的字符串数据
     */
    private void parseTencentResponse(String responseBody, Map<String, TencentStockQuote> resultMap) {
        if (responseBody == null || responseBody.isBlank()) {
            return;
        }

        // 腾讯返回格式示例（响应体内容形如：v_sh600519 对应的字符串 "1~贵州茅台~600519~1700.00~..."）
        String[] lines = responseBody.split(";");
        for (String line : lines) {
            String trimmedLine = line.trim();
            int firstQuote = trimmedLine.indexOf("\"");
            int lastQuote = trimmedLine.lastIndexOf("\"");

            if (trimmedLine.contains("~") && firstQuote != -1 && lastQuote != -1 && firstQuote < lastQuote) {
                try {
                    String content = trimmedLine.substring(firstQuote + 1, lastQuote);
                    String[] parts = content.split("~");

                    // 腾讯协议字段索引：1: 股票名称, 2: 股票代码, 3: 最新价
                    if (parts.length > 3 && !parts[3].isEmpty()) {
                        String name = parts[1];
                        String code = parts[2];
                        BigDecimal price = new BigDecimal(parts[3]);
                        resultMap.put(code, new TencentStockQuote(name, price));
                    }
                } catch (Exception e) {
                    log.warn("解析单条腾讯行情失败: line={}, msg={}", trimmedLine, e.getMessage());
                }
            }
        }
    }

}
