package com.brotherc.aquant.integration.cmf.service;

import com.brotherc.aquant.common.constant.FundPurchaseLimitConstant;
import com.brotherc.aquant.integration.cmf.model.CMFFundPurchaseLimit;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 招商基金官网纳斯达克100联接基金直销额度接入服务。
 */
@Service
@RequiredArgsConstructor
public class CMFFundService {

    private static final int MAX_RESPONSE_SIZE = 2 * 1024 * 1024;
    private static final Pattern ANNOUNCEMENT_ID_PATTERN = Pattern.compile("/(\\d+)/index\\.html$");
    private static final Pattern EFFECTIVE_DATE_PATTERN = Pattern.compile(
            "(?:暂停大额申购起始日|恢复(?:大额)?申购(?:起始日)?|自)"
                    + ".{0,40}?(20\\d{2})年(\\d{1,2})月(\\d{1,2})日"
    );
    private static final Pattern FALLBACK_EFFECTIVE_DATE_PATTERN = Pattern.compile(
            "自(20\\d{2})年(\\d{1,2})月(\\d{1,2})日起"
    );
    private static final Pattern PURCHASE_LIMIT_PATTERN = Pattern.compile(
            "限制申购金额[^\\d]{0,50}([\\d,.]+)"
    );
    private static final Pattern RECURRING_LIMIT_PATTERN = Pattern.compile(
            "限制定期定额投资金额[^\\d]{0,20}([\\d,.]+)"
    );

    private final OkHttpClient okHttpClient;

    @Value("${cmffund-address}")
    private String cmfFundAddress;

    /**
     * 查询产品页中最新的招商纳斯达克100直销申购额度公告，并解析其当前规则。
     */
    public CMFFundPurchaseLimit getLatestNasdaq100DirectLimitAnnouncement() {
        HttpUrl productUrl = HttpUrl.get(cmfFundAddress).newBuilder()
                .addPathSegments("web/fundDetail/019547/index.html")
                .build();
        try {
            return parseLatestAnnouncement(execute(productUrl), productUrl);
        } catch (Exception e) {
            throw new IllegalStateException("获取招商基金官方直销额度公告失败", e);
        }
    }

    public CMFFundPurchaseLimit getNasdaq100DirectPurchaseLimit(CMFFundPurchaseLimit announcement) {
        try {
            HttpUrl detailUrl = HttpUrl.get(announcement.getDetailUrl());
            return parsePurchaseLimit(execute(detailUrl), announcement);
        } catch (Exception e) {
            throw new IllegalStateException("解析招商基金官方直销额度公告失败", e);
        }
    }

    CMFFundPurchaseLimit parseLatestAnnouncement(byte[] response, HttpUrl productUrl) {
        Document document = Jsoup.parse(new String(response, StandardCharsets.UTF_8), productUrl.toString());
        for (Element link : document.select(".pro_articlelist a.item[href]")) {
            String title = link.select("p").text().trim();
            String normalizedTitle = title.replace(" ", "");
            boolean target = normalizedTitle.contains("招商纳斯达克100")
                    && normalizedTitle.contains("直销")
                    && normalizedTitle.contains("申购")
                    && (normalizedTitle.contains("大额") || normalizedTitle.contains("暂停")
                    || normalizedTitle.contains("恢复"));
            if (target) {
                HttpUrl detailUrl = productUrl.resolve(link.attr("href"));
                Matcher idMatcher = detailUrl == null ? null
                        : ANNOUNCEMENT_ID_PATTERN.matcher(detailUrl.encodedPath());
                Element dateElement = link.selectFirst(".date");
                if (detailUrl == null || idMatcher == null || !idMatcher.find() || dateElement == null) {
                    throw new IllegalStateException("招商基金额度公告链接结构发生变化");
                }
                CMFFundPurchaseLimit result = new CMFFundPurchaseLimit();
                result.setAnnouncementId(idMatcher.group(1));
                result.setAnnouncementDate(LocalDate.parse(dateElement.text().trim()));
                result.setTitle(title);
                result.setDetailUrl(detailUrl.toString());
                return result;
            }
        }
        throw new IllegalStateException("招商基金产品页未找到纳指100直销额度公告");
    }

    CMFFundPurchaseLimit parsePurchaseLimit(byte[] response, CMFFundPurchaseLimit result) {
        Document document = Jsoup.parse(new String(response, StandardCharsets.UTF_8), result.getDetailUrl());
        Element contentElement = document.selectFirst(".article_detail_box");
        if (contentElement == null) {
            throw new IllegalStateException("招商基金额度公告正文结构发生变化");
        }
        String text = contentElement.text().replaceAll("\\s+", "");
        String fullText = result.getTitle().replaceAll("\\s+", "") + text;
        if (!fullText.contains("招商纳斯达克100") || !fullText.contains("直销")
                || !text.contains("019547") || !text.contains("019548")) {
            throw new IllegalStateException("招商基金额度公告与目标基金或直销渠道不匹配");
        }

        result.setEffectiveDate(parseEffectiveDate(text));
        if (fullText.contains("暂停申购") && !fullText.contains("暂停大额申购")) {
            result.setPurchaseStatus(FundPurchaseLimitConstant.STATUS_SUSPENDED);
        } else if (fullText.contains("大额申购")) {
            result.setPurchaseStatus(FundPurchaseLimitConstant.STATUS_LIMITED);
            result.setPurchaseLimitAmount(parseAmount(text, PURCHASE_LIMIT_PATTERN, "申购"));
        } else if (fullText.contains("恢复申购") || fullText.contains("取消大额申购限制")) {
            result.setPurchaseStatus(FundPurchaseLimitConstant.STATUS_OPEN);
        } else {
            throw new IllegalStateException("无法识别招商基金申购状态");
        }

        if (fullText.contains("定期定额") || fullText.contains("定投")) {
            result.setRecurringStatus(result.getPurchaseStatus());
            if (FundPurchaseLimitConstant.STATUS_LIMITED.equals(result.getRecurringStatus())) {
                Matcher recurringMatcher = RECURRING_LIMIT_PATTERN.matcher(text);
                result.setRecurringLimitAmount(recurringMatcher.find()
                        ? new BigDecimal(recurringMatcher.group(1).replace(",", ""))
                        : result.getPurchaseLimitAmount());
            }
        }
        return result;
    }

    private LocalDate parseEffectiveDate(String text) {
        Matcher matcher = EFFECTIVE_DATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            matcher = FALLBACK_EFFECTIVE_DATE_PATTERN.matcher(text);
            if (!matcher.find()) {
                throw new IllegalStateException("招商基金额度公告未解析出生效日期");
            }
        }
        return LocalDate.of(Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

    private BigDecimal parseAmount(String text, Pattern pattern, String businessName) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new IllegalStateException("招商基金额度公告未解析出" + businessName + "限额");
        }
        return new BigDecimal(matcher.group(1).replace(",", ""));
    }

    private byte[] execute(HttpUrl url) {
        HttpUrl officialUrl = HttpUrl.get(cmfFundAddress);
        if (!"https".equals(url.scheme()) || !officialUrl.host().equals(url.host())) {
            throw new IllegalArgumentException("招商基金官网地址不合法");
        }
        try {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(500, 1201));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("招商基金请求等待被中断", e);
        }
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; AQuant/1.0)")
                .get()
                .build();
        OkHttpClient client = okHttpClient.newBuilder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(25, TimeUnit.SECONDS)
                .build();
        try (Response response = client.newCall(request).execute()) {
            HttpUrl responseUrl = response.request().url();
            if (!url.host().equals(responseUrl.host()) || !url.scheme().equals(responseUrl.scheme())) {
                throw new IllegalStateException("招商基金官网响应跳转到非配置域名");
            }
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("招商基金官网请求失败，status=" + response.code());
            }
            if (body.contentLength() > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("招商基金官网响应超过大小限制");
            }
            byte[] bytes = body.bytes();
            if (bytes.length > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("招商基金官网响应超过大小限制");
            }
            return bytes;
        } catch (Exception e) {
            throw new IllegalStateException("招商基金官网请求异常，url=" + url, e);
        }
    }

}
