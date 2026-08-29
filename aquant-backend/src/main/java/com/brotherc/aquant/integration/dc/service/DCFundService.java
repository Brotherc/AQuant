package com.brotherc.aquant.integration.dc.service;

import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.integration.dc.model.DCFundPurchaseLimit;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 大成基金官网当前直销额度接入服务。
 */
@Service
@RequiredArgsConstructor
public class DCFundService {

    private static final int MAX_RESPONSE_SIZE = 2 * 1024 * 1024;
    private static final String FUND_CODE_A = "000834";
    private static final String FUND_CODE_C = "008971";
    private static final Pattern LIMIT_AMOUNT_PATTERN = Pattern.compile(
            "A/C各限额\\s*([\\d,.]+)\\s*(万|亿)?\\s*元"
    );

    private final OkHttpClient okHttpClient;

    @Value("${dcfund-address}")
    private String dcFundAddress;

    /**
     * 读取大成官网首页维护的纳斯达克100联接基金当前直销申购、定投额度。
     */
    public List<DCFundPurchaseLimit> getNasdaq100PurchaseLimits() {
        HttpUrl url = HttpUrl.get(dcFundAddress).newBuilder()
                .addPathSegments("main/home/index.shtml")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; AQuant/1.0)")
                .get()
                .build();
        try {
            return parsePurchaseLimits(execute(request));
        } catch (Exception e) {
            throw new IllegalStateException("获取大成基金官方额度失败", e);
        }
    }

    List<DCFundPurchaseLimit> parsePurchaseLimits(byte[] response) {
        Element target = Jsoup.parse(new String(response, StandardCharsets.UTF_8))
                .selectFirst("li[data-value-fund_code=" + FUND_CODE_A + "]");
        if (target == null) {
            throw new IllegalStateException("大成官网首页未找到纳斯达克100目标基金");
        }
        String fundName = target.attr("data-value-fund_name");
        if (!fundName.contains("大成纳斯达克100") || !fundName.endsWith("A")) {
            throw new IllegalStateException("大成目标基金代码对应名称发生变化，fundName=" + fundName);
        }
        String limitText = target.select("p.p1").text().replace(" ", "");
        Matcher matcher = LIMIT_AMOUNT_PATTERN.matcher(limitText);
        if (!matcher.find()) {
            throw new IllegalStateException("无法识别大成纳指100直销额度，value=" + limitText);
        }
        if (!limitText.contains("支持日定投")) {
            throw new IllegalStateException("无法确认大成纳指100定投状态，value=" + limitText);
        }
        BigDecimal limitAmount = StockUtils.toAmount(matcher.group(1), matcher.group(2));
        if (limitAmount == null || limitAmount.signum() <= 0) {
            throw new IllegalStateException("大成纳指100直销额度必须大于零，value=" + limitText);
        }

        List<DCFundPurchaseLimit> result = new ArrayList<>();
        for (String fundCode : List.of(FUND_CODE_A, FUND_CODE_C)) {
            DCFundPurchaseLimit limit = new DCFundPurchaseLimit();
            limit.setFundCode(fundCode);
            limit.setCurrency("CNY");
            limit.setSalesChannel(FundPurchaseLimitConstant.CHANNEL_DIRECT);
            limit.setPurchaseStatus(FundPurchaseLimitConstant.STATUS_LIMITED);
            limit.setPurchaseLimitAmount(limitAmount);
            limit.setRecurringStatus(FundPurchaseLimitConstant.STATUS_LIMITED);
            limit.setRecurringLimitAmount(limitAmount);
            result.add(limit);
        }
        return result;
    }

    private byte[] execute(Request request) {
        try {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(500, 1201));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("大成基金请求等待被中断", e);
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
                throw new IllegalStateException("大成基金官网响应跳转到非配置域名");
            }
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("大成基金官网请求失败，status=" + response.code());
            }
            if (body.contentLength() > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("大成基金官网响应超过大小限制");
            }
            byte[] bytes = body.bytes();
            if (bytes.length > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("大成基金官网响应超过大小限制");
            }
            return bytes;
        } catch (Exception e) {
            throw new IllegalStateException("大成基金官网请求异常，url=" + request.url(), e);
        }
    }

}
