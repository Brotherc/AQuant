package com.brotherc.aquant.integration.efund.service;

import com.brotherc.aquant.integration.efund.model.EFundAnnouncement;
import com.brotherc.aquant.integration.efund.model.EFundAnnouncementPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 易方达基金官网公开公告接口。
 */
@Service
@RequiredArgsConstructor
public class EFundService {

    private static final int PAGE_SIZE = 10;
    private static final int MAX_RESPONSE_SIZE = 15 * 1024 * 1024;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @Value("${efund-address}")
    private String eFundAddress;

    @Value("${efund-api-address}")
    private String eFundApiAddress;

    @Value("${efund-cdn-address}")
    private String eFundCdnAddress;

    /**
     * 查询易方达纳斯达克100指数基金公告，官网页码从 0 开始。
     */
    public EFundAnnouncementPage getNasdaq100Announcements(int page) {
        HttpUrl url = HttpUrl.get(eFundApiAddress).newBuilder()
                .addPathSegments("xcowch/front/contents")
                .build();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("siteID", "1");
        params.put("catalogAlias", "xxplflwj,xxpldqgg,xxpllsgg");
        params.put("title", "申购");
        params.put("fundCode", "161130");
        params.put("isIncludeTransFund", "Y");
        params.put("pageSize", PAGE_SIZE);
        params.put("pageIndex", Math.max(page - 1, 0));
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "AQuant/1.0")
                    .post(RequestBody.create(objectMapper.writeValueAsBytes(params), JSON))
                    .build();
            JsonNode root = objectMapper.readTree(execute(request));
            if (root.path("status").asInt() != 1 || !root.path("data").isObject()) {
                throw new IllegalStateException("易方达基金公告接口返回失败，message="
                        + root.path("message").asText());
            }
            JsonNode data = root.path("data");
            EFundAnnouncementPage result = new EFundAnnouncementPage();
            int total = data.path("total").asInt();
            result.setTotalPages(Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE));
            for (JsonNode item : data.path("data")) {
                String attachmentUrl = item.path("path").asText();
                if (item.path("id").isMissingNode() || attachmentUrl.isBlank()) {
                    continue;
                }
                EFundAnnouncement announcement = new EFundAnnouncement();
                announcement.setAnnouncementId(item.path("id").asText());
                announcement.setAnnouncementDate(parseDate(item));
                announcement.setTitle(item.path("title").asText());
                String link = item.path("link").asText();
                HttpUrl detailUrl = link.isBlank() ? null : HttpUrl.get(eFundAddress).resolve(link);
                announcement.setDetailUrl(detailUrl == null ? attachmentUrl : detailUrl.toString());
                announcement.setAttachmentUrl(attachmentUrl);
                result.getContent().add(announcement);
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("获取易方达基金公告列表失败，page=" + page, e);
        }
    }

    public byte[] downloadAnnouncement(String attachmentUrl) {
        HttpUrl url = HttpUrl.parse(attachmentUrl);
        HttpUrl cdnUrl = HttpUrl.get(eFundCdnAddress);
        if (url == null || !"https".equals(url.scheme()) || !cdnUrl.host().equals(url.host())
                || !url.encodedPath().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("易方达基金公告附件地址不合法");
        }
        return execute(new Request.Builder().url(url).header("User-Agent", "AQuant/1.0").get().build());
    }

    private LocalDate parseDate(JsonNode item) {
        String date = item.path("prop1").asText();
        if (date.length() < 10) {
            date = item.path("publishDate").asText();
        }
        if (date.length() < 10) {
            throw new IllegalStateException("易方达基金公告缺少发布日期，id=" + item.path("id").asText());
        }
        return LocalDate.parse(date.substring(0, 10));
    }

    private byte[] execute(Request request) {
        try {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(500, 1201));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("易方达基金请求等待被中断", e);
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
                throw new IllegalStateException("易方达基金官网响应跳转到非配置域名");
            }
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("易方达基金官网请求失败，status=" + response.code());
            }
            if (body.contentLength() > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("易方达基金官网响应超过大小限制");
            }
            byte[] bytes = body.bytes();
            if (bytes.length > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("易方达基金官网响应超过大小限制");
            }
            return bytes;
        } catch (Exception e) {
            throw new IllegalStateException("易方达基金官网请求异常，url=" + request.url(), e);
        }
    }

}
