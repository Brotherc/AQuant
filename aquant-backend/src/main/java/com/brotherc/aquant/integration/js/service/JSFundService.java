package com.brotherc.aquant.integration.js.service;

import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.integration.js.model.JSFundPurchaseLimit;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 嘉实基金官网当前申购上限表接入服务。
 */
@Service
@RequiredArgsConstructor
public class JSFundService {

    private static final int MAX_RESPONSE_SIZE = 2 * 1024 * 1024;
    private static final Set<String> TARGET_FUND_CODES = Set.of(
            "016532", "016533", "021838", "016534", "016535"
    );
    private static final Pattern FUND_CODE_PATTERN = Pattern.compile("\\b\\d{6}\\b");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([\\d,.]+)\\s*(万|亿)?\\s*(?:元|美元)");
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2})/(\\d{1,2})/(\\d{1,2})");

    private final OkHttpClient okHttpClient;

    @Value("${jsfund-address}")
    private String jsFundAddress;

    /**
     * 读取嘉实官网维护的纳斯达克100联接基金当前申购、定投状态。
     */
    public List<JSFundPurchaseLimit> getNasdaq100PurchaseLimits() {
        HttpUrl url = HttpUrl.get(jsFundAddress).newBuilder()
                .addPathSegments("main/a/20151216/191092.shtml")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; AQuant/1.0)")
                .get()
                .build();
        try {
            List<JSFundPurchaseLimit> limits = parsePurchaseLimits(execute(request));
            Set<String> actualFundCodes = new HashSet<>();
            for (JSFundPurchaseLimit limit : limits) {
                actualFundCodes.add(limit.getFundCode());
            }
            if (!actualFundCodes.equals(TARGET_FUND_CODES)) {
                throw new IllegalStateException("嘉实纳指100额度页面目标份额不完整，fundCodes=" + actualFundCodes);
            }
            return limits;
        } catch (Exception e) {
            throw new IllegalStateException("获取嘉实基金官方额度失败", e);
        }
    }

    List<JSFundPurchaseLimit> parsePurchaseLimits(byte[] response) {
        List<JSFundPurchaseLimit> result = new ArrayList<>();
        for (Element row : Jsoup.parse(new String(response, StandardCharsets.UTF_8)).select("tr")) {
            Elements cells = row.select("td");
            if (cells.size() >= 6 && cells.get(1).text().contains("嘉实纳斯达克100ETF发起联接")) {
                String purchaseText = cells.get(2).text();
                String recurringText = cells.get(4).text();
                String effectiveDateText = cells.get(5).text();
                String salesChannel = purchaseText.contains("直销：") || effectiveDateText.contains("直销：")
                        ? FundPurchaseLimitConstant.CHANNEL_DIRECT : FundPurchaseLimitConstant.CHANNEL_ALL;
                String currency = cells.get(1).text().contains("美元") ? "USD" : "CNY";
                Matcher codeMatcher = FUND_CODE_PATTERN.matcher(cells.get(0).text());
                while (codeMatcher.find()) {
                    String fundCode = codeMatcher.group();
                    if (TARGET_FUND_CODES.contains(fundCode)) {
                        JSFundPurchaseLimit limit = new JSFundPurchaseLimit();
                        limit.setFundCode(fundCode);
                        limit.setCurrency(currency);
                        limit.setSalesChannel(salesChannel);
                        limit.setPurchaseStatus(parseStatus(purchaseText));
                        limit.setPurchaseLimitAmount(parseLimitAmount(purchaseText));
                        limit.setRecurringStatus(parseStatus(recurringText));
                        limit.setRecurringLimitAmount(parseLimitAmount(recurringText));
                        limit.setEffectiveDate(parseEffectiveDate(effectiveDateText));
                        result.add(limit);
                    }
                }
            }
        }
        return result;
    }

    private String parseStatus(String text) {
        if (text.contains("暂停") || text.contains("暂未开通") || text.contains("暂不支持")) {
            return FundPurchaseLimitConstant.STATUS_SUSPENDED;
        }
        if (text.contains("不限") || text.contains("无限额")) {
            return FundPurchaseLimitConstant.STATUS_OPEN;
        }
        if (AMOUNT_PATTERN.matcher(text).find()) {
            return FundPurchaseLimitConstant.STATUS_LIMITED;
        }
        throw new IllegalStateException("无法识别嘉实基金业务状态，value=" + text);
    }

    private BigDecimal parseLimitAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return StockUtils.toAmount(matcher.group(1), matcher.group(2));
    }

    private LocalDate parseEffectiveDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            throw new IllegalStateException("无法识别嘉实基金额度生效日期，value=" + text);
        }
        return LocalDate.of(Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

    private byte[] execute(Request request) {
        try {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(500, 1201));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("嘉实基金请求等待被中断", e);
        }
        OkHttpClient client = okHttpClient.newBuilder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(25, TimeUnit.SECONDS)
                .build();
        try (Response response = client.newCall(request).execute()) {
            HttpUrl responseUrl = response.request().url();
            if (!request.url().host().equals(responseUrl.host())
                    || !request.url().scheme().equals(responseUrl.scheme())) {
                throw new IllegalStateException("嘉实基金官网响应跳转到非配置域名");
            }
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("嘉实基金官网请求失败，status=" + response.code());
            }
            if (body.contentLength() > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("嘉实基金官网响应超过大小限制");
            }
            byte[] bytes = body.bytes();
            if (bytes.length > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("嘉实基金官网响应超过大小限制");
            }
            return bytes;
        } catch (Exception e) {
            throw new IllegalStateException("嘉实基金官网请求异常，url=" + request.url(), e);
        }
    }

}
