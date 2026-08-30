package com.brotherc.aquant.integration.chinaamc.service;

import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.integration.chinaamc.model.ChinaAMCFundPurchaseLimit;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 华夏基金官网开放状态接入服务。
 */
@Service
@RequiredArgsConstructor
public class ChinaAMCFundService {

    private static final int MAX_RESPONSE_SIZE = 2 * 1024 * 1024;
    private static final Set<String> TARGET_FUND_CODES = Set.of("015299", "015300", "015518");
    private static final Pattern LIMIT_AMOUNT_PATTERN = Pattern.compile(
            "(?:不得超过|不超过|上限(?:调整)?为|限额(?:调整)?为|限制金额为)(?:人民币|美元)?\\s*"
                    + "([\\d,.]+)\\s*(万|亿)?\\s*(?:元|美元)?"
    );

    private final OkHttpClient okHttpClient;

    @Value("${chinaamc-address}")
    private String chinaAMCAddress;

    /**
     * 读取华夏官网维护的纳斯达克100联接基金当前申购、定投状态。
     */
    public List<ChinaAMCFundPurchaseLimit> getNasdaq100PurchaseLimits() {
        HttpUrl url = HttpUrl.get(chinaAMCAddress).newBuilder()
                .addPathSegments("ProductForWeb/getCalendar")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; AQuant/1.0)")
                .get()
                .build();
        try {
            List<ChinaAMCFundPurchaseLimit> limits = parsePurchaseLimits(execute(request));
            Set<String> actualFundCodes = new HashSet<>();
            for (ChinaAMCFundPurchaseLimit limit : limits) {
                actualFundCodes.add(limit.getFundCode());
            }
            if (!actualFundCodes.equals(TARGET_FUND_CODES)) {
                throw new IllegalStateException("华夏纳指100开放状态目标份额不完整，fundCodes=" + actualFundCodes);
            }
            return limits;
        } catch (Exception e) {
            throw new IllegalStateException("获取华夏基金官方开放状态失败", e);
        }
    }

    List<ChinaAMCFundPurchaseLimit> parsePurchaseLimits(byte[] response) {
        List<ChinaAMCFundPurchaseLimit> result = new ArrayList<>();
        for (Element row : Jsoup.parse(new String(response, StandardCharsets.UTF_8)).select("tr")) {
            Elements cells = row.select("td");
            if (cells.size() >= 8 && TARGET_FUND_CODES.contains(cells.get(0).text().trim())) {
                String fundCode = cells.get(0).text().trim();
                String fundName = cells.get(1).text();
                if (!fundName.contains("华夏纳斯达克100")) {
                    throw new IllegalStateException("华夏目标基金代码对应名称发生变化，fundCode=" + fundCode);
                }
                String restriction = cells.get(7).text();
                ChinaAMCFundPurchaseLimit limit = new ChinaAMCFundPurchaseLimit();
                limit.setFundCode(fundCode);
                limit.setCurrency("015518".equals(fundCode) ? "USD" : "CNY");
                limit.setSalesChannel(FundPurchaseLimitConstant.CHANNEL_DIRECT);
                String purchaseStatus = parseStatus(cells.get(2).text());
                String recurringStatus = parseStatus(cells.get(6).text());
                BigDecimal limitAmount = parseLimitAmount(restriction);
                if ((FundPurchaseLimitConstant.STATUS_LIMITED.equals(purchaseStatus)
                        || FundPurchaseLimitConstant.STATUS_LIMITED.equals(recurringStatus))
                        && limitAmount == null) {
                    throw new IllegalStateException("华夏基金有限制但未解析出金额，fundCode=" + fundCode);
                }
                limit.setPurchaseStatus(purchaseStatus);
                limit.setPurchaseLimitAmount(FundPurchaseLimitConstant.STATUS_LIMITED.equals(purchaseStatus)
                        ? limitAmount : null);
                limit.setRecurringStatus(recurringStatus);
                limit.setRecurringLimitAmount(FundPurchaseLimitConstant.STATUS_LIMITED.equals(recurringStatus)
                        ? limitAmount : null);
                result.add(limit);
            }
        }
        return result;
    }

    private String parseStatus(String statusText) {
        String value = statusText.replace(" ", "");
        if (value.contains("暂停") || value.contains("未开放")) {
            return FundPurchaseLimitConstant.STATUS_SUSPENDED;
        }
        if (value.contains("有限制")) {
            return FundPurchaseLimitConstant.STATUS_LIMITED;
        }
        if (value.contains("开放")) {
            return FundPurchaseLimitConstant.STATUS_OPEN;
        }
        throw new IllegalStateException("无法识别华夏基金业务状态，value=" + statusText);
    }

    private BigDecimal parseLimitAmount(String restriction) {
        Matcher matcher = LIMIT_AMOUNT_PATTERN.matcher(restriction);
        if (!matcher.find()) {
            return null;
        }
        return StockUtils.toAmount(matcher.group(1), matcher.group(2));
    }

    private byte[] execute(Request request) {
        try {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(500, 1201));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("华夏基金请求等待被中断", e);
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
                throw new IllegalStateException("华夏基金官网响应跳转到非配置域名");
            }
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("华夏基金官网请求失败，status=" + response.code());
            }
            if (body.contentLength() > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("华夏基金官网响应超过大小限制");
            }
            byte[] bytes = body.bytes();
            if (bytes.length > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("华夏基金官网响应超过大小限制");
            }
            return bytes;
        } catch (Exception e) {
            throw new IllegalStateException("华夏基金官网请求异常，url=" + request.url(), e);
        }
    }

}
