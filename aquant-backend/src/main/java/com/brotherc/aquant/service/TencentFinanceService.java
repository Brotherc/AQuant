package com.brotherc.aquant.service;

import com.brotherc.aquant.model.dto.tencent.TencentStockQuote;
import com.brotherc.aquant.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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

        // 腾讯返回格式示例：v_sh600519="1~贵州茅台~600519~1700.00~...";
        String[] lines = responseBody.split(";");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || !trimmedLine.contains("~")) {
                continue;
            }

            try {
                // 寻找引号包裹的内容
                int firstQuote = trimmedLine.indexOf("\"");
                int lastQuote = trimmedLine.lastIndexOf("\"");
                if (firstQuote == -1 || lastQuote == -1 || firstQuote >= lastQuote) {
                    continue;
                }

                String content = trimmedLine.substring(firstQuote + 1, lastQuote);
                String[] parts = content.split("~");

                // 腾讯协议字段索引：1: 股票名称, 2: 股票代码, 3: 最新价
                if (parts.length > 3) {
                    String name = parts[1];
                    String code = parts[2];
                    String priceStr = parts[3];
                    
                    if (!priceStr.isEmpty()) {
                        BigDecimal price = new BigDecimal(priceStr);
                        resultMap.put(code, new TencentStockQuote(name, price));
                    }
                }
            } catch (Exception e) {
                log.warn("解析单条腾讯行情失败: line={}, msg={}", trimmedLine, e.getMessage());
            }
        }
    }

}
