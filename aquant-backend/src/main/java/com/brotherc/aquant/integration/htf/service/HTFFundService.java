package com.brotherc.aquant.integration.htf.service;

import com.brotherc.aquant.integration.htf.model.HTFFundAnnouncement;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 汇添富基金官网纳斯达克100联接基金公告接入服务。
 */
@Service
@RequiredArgsConstructor
public class HTFFundService {

    private static final int MAX_RESPONSE_SIZE = 2 * 1024 * 1024;
    private static final Charset WEBSITE_CHARSET = Charset.forName("GB18030");
    private static final Pattern ANNOUNCEMENT_ID_PATTERN = Pattern.compile("/(\\d+)\\.shtml$");

    private final OkHttpClient okHttpClient;

    @Value("${htffund-address}")
    private String htfFundAddress;

    /**
     * 从人民币 A 份额产品公告页读取最新的纳斯达克100申购额度公告。
     */
    public HTFFundAnnouncement getLatestNasdaq100LimitAnnouncement() {
        HttpUrl listUrl = HttpUrl.get(htfFundAddress).newBuilder()
                .addPathSegments("main/products/pofund/018966/fundgg.shtml")
                .build();
        try {
            return parseLatestAnnouncement(execute(listUrl), listUrl);
        } catch (Exception e) {
            throw new IllegalStateException("获取汇添富基金官方额度公告失败", e);
        }
    }

    public byte[] downloadAnnouncement(String attachmentUrl) {
        HttpUrl url = HttpUrl.parse(attachmentUrl);
        if (url == null || !url.encodedPath().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("汇添富基金公告附件地址不合法");
        }
        return execute(url);
    }

    HTFFundAnnouncement parseLatestAnnouncement(byte[] response, HttpUrl listUrl) {
        Document document = Jsoup.parse(new String(response, WEBSITE_CHARSET), listUrl.toString());
        for (Element row : document.select("table.sharetable tr")) {
            Elements cells = row.select("td");
            Element detailLink = cells.isEmpty() ? null : cells.get(0).selectFirst("a[href]");
            String title = detailLink == null ? "" : detailLink.text().trim();
            String normalizedTitle = title.replace(" ", "");
            boolean target = normalizedTitle.contains("汇添富纳斯达克100")
                    && (normalizedTitle.contains("大额申购") || normalizedTitle.contains("暂停申购")
                    || normalizedTitle.contains("恢复申购"))
                    && !normalizedTitle.contains("节假日");
            if (target) {
                Element attachmentLink = row.selectFirst("a.yellowline[href]");
                HttpUrl detailUrl = listUrl.resolve(detailLink.attr("href"));
                HttpUrl attachmentUrl = attachmentLink == null ? null : listUrl.resolve(attachmentLink.attr("href"));
                Matcher idMatcher = detailUrl == null ? null
                        : ANNOUNCEMENT_ID_PATTERN.matcher(detailUrl.encodedPath());
                if (cells.size() < 2 || detailUrl == null || attachmentUrl == null
                        || idMatcher == null || !idMatcher.find()) {
                    throw new IllegalStateException("汇添富基金额度公告链接结构发生变化");
                }
                HTFFundAnnouncement result = new HTFFundAnnouncement();
                result.setAnnouncementId(idMatcher.group(1));
                result.setAnnouncementDate(LocalDate.parse(cells.get(1).text().trim()));
                result.setTitle(title);
                result.setDetailUrl(detailUrl.toString());
                result.setAttachmentUrl(attachmentUrl.toString());
                return result;
            }
        }
        throw new IllegalStateException("汇添富基金产品页未找到纳指100额度公告");
    }

    private byte[] execute(HttpUrl url) {
        HttpUrl officialUrl = HttpUrl.get(htfFundAddress);
        if (!"https".equals(url.scheme()) || !officialUrl.host().equals(url.host())) {
            throw new IllegalArgumentException("汇添富基金官网地址不合法");
        }
        try {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(500, 1201));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("汇添富基金请求等待被中断", e);
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
                throw new IllegalStateException("汇添富基金官网响应跳转到非配置域名");
            }
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("汇添富基金官网请求失败，status=" + response.code());
            }
            if (body.contentLength() > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("汇添富基金官网响应超过大小限制");
            }
            byte[] bytes = body.bytes();
            if (bytes.length > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("汇添富基金官网响应超过大小限制");
            }
            return bytes;
        } catch (Exception e) {
            throw new IllegalStateException("汇添富基金官网请求异常，url=" + url, e);
        }
    }

}
