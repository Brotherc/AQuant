package com.brotherc.aquant.integration.nf.service;

import com.brotherc.aquant.integration.nf.model.NFFundAnnouncement;
import com.brotherc.aquant.integration.nf.model.NFFundAnnouncementPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 南方基金官网公开公告接口。
 */
@Service
@RequiredArgsConstructor
public class NFFundService {

    private static final int PAGE_SIZE = 15;
    private static final int MAX_RESPONSE_SIZE = 15 * 1024 * 1024;

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @Value("${nffund-address}")
    private String nfFundAddress;

    /**
     * 按基金代码查询南方纳斯达克100指数基金公告，官网页码从 1 开始。
     */
    public NFFundAnnouncementPage getNasdaq100Announcements(int page) {
        HttpUrl url = HttpUrl.get(nfFundAddress).newBuilder()
                .addPathSegments("nfwebApi/notice/disclosureInfo")
                .build();
        FormBody formBody = new FormBody.Builder()
                .add("jjtype", "")
                .add("jjcode", "016452")
                .add("jjtitle", "申购")
                .add("tabsid", "newgg")
                .add("type", "0")
                .add("curPage", String.valueOf(Math.max(page, 1)))
                .add("pageSize", String.valueOf(PAGE_SIZE))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "AQuant/1.0")
                .post(formBody)
                .build();
        try {
            JsonNode root = objectMapper.readTree(execute(request));
            if (!"ETS-5BP00000".equals(root.path("code").asText())) {
                throw new IllegalStateException("南方基金公告接口返回失败，message="
                        + root.path("message").asText());
            }
            JsonNode pageNode = root.path("data").path("nfggList");
            if (!pageNode.isObject()) {
                throw new IllegalStateException("南方基金公告接口缺少分页数据");
            }
            NFFundAnnouncementPage result = new NFFundAnnouncementPage();
            result.setTotalPages(Math.max(pageNode.path("pages").asInt(1), 1));
            for (JsonNode item : pageNode.path("list")) {
                String attachmentPath = item.path("url").asText();
                if (item.path("id").isMissingNode() || attachmentPath.isBlank()) {
                    continue;
                }
                HttpUrl attachmentUrl = HttpUrl.get(nfFundAddress).resolve(attachmentPath);
                if (attachmentUrl == null) {
                    throw new IllegalStateException("南方基金公告附件地址无法解析，id="
                            + item.path("id").asText());
                }
                NFFundAnnouncement announcement = new NFFundAnnouncement();
                announcement.setAnnouncementId(item.path("id").asText());
                announcement.setAnnouncementDate(parseDate(item));
                announcement.setTitle(item.path("title").asText());
                announcement.setDetailUrl(attachmentUrl.toString());
                announcement.setAttachmentUrl(attachmentUrl.toString());
                result.getContent().add(announcement);
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("获取南方基金公告列表失败，page=" + page, e);
        }
    }

    public byte[] downloadAnnouncement(String attachmentUrl) {
        HttpUrl url = HttpUrl.parse(attachmentUrl);
        HttpUrl officialUrl = HttpUrl.get(nfFundAddress);
        if (url == null || !"https".equals(url.scheme()) || !officialUrl.host().equals(url.host())
                || !url.encodedPath().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("南方基金公告附件地址不合法");
        }
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "AQuant/1.0")
                .get()
                .build();
        return execute(request);
    }

    private LocalDate parseDate(JsonNode item) {
        String date = item.path("creatortime").asText();
        if (date.length() < 10) {
            date = item.path("publishtime").asText();
        }
        if (date.length() < 10) {
            throw new IllegalStateException("南方基金公告缺少发布日期，id=" + item.path("id").asText());
        }
        return LocalDate.parse(date.substring(0, 10));
    }

    private byte[] execute(Request request) {
        try {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(500, 1201));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("南方基金请求等待被中断", e);
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
                throw new IllegalStateException("南方基金官网响应跳转到非配置域名");
            }
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("南方基金官网请求失败，status=" + response.code());
            }
            if (body.contentLength() > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("南方基金官网响应超过大小限制");
            }
            byte[] bytes = body.bytes();
            if (bytes.length > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("南方基金官网响应超过大小限制");
            }
            return bytes;
        } catch (Exception e) {
            throw new IllegalStateException("南方基金官网请求异常，url=" + request.url(), e);
        }
    }

}
