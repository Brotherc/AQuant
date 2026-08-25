package com.brotherc.aquant.integration.th.service;

import com.brotherc.aquant.integration.th.model.THFundAnnouncement;
import com.brotherc.aquant.integration.th.model.THFundAnnouncementPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
 * 天弘基金公告接入服务。公告目录使用公开基金披露接口，额度内容以基金公司发布的正式 PDF 为准。
 */
@Service
@RequiredArgsConstructor
public class THFundService {

    private static final int PAGE_SIZE = 20;
    private static final int MAX_RESPONSE_SIZE = 15 * 1024 * 1024;
    private static final String TARGET_FUND_CODE = "018043";

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @Value("${thfund-address}")
    private String thFundAddress;

    @Value("${thfund-disclosure-api-address}")
    private String thFundDisclosureApiAddress;

    @Value("${thfund-disclosure-cdn-address}")
    private String thFundDisclosureCdnAddress;

    /**
     * 查询天弘纳斯达克100指数基金公告。A/C/D 份额共用公告，使用 A 类代码可避免重复扫描。
     */
    public THFundAnnouncementPage getNasdaq100Announcements(int page) {
        int pageIndex = Math.max(page, 1);
        HttpUrl url = HttpUrl.get(thFundDisclosureApiAddress).newBuilder()
                .addPathSegments("f10/JJGG")
                .addQueryParameter("fundcode", TARGET_FUND_CODE)
                .addQueryParameter("pageIndex", String.valueOf(pageIndex))
                .addQueryParameter("pageSize", String.valueOf(PAGE_SIZE))
                .addQueryParameter("type", "0")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; AQuant/1.0)")
                .header("Referer", "https://fundf10.eastmoney.com/")
                .get()
                .build();
        try {
            return parseAnnouncementPage(execute(request));
        } catch (Exception e) {
            throw new IllegalStateException("获取天弘基金公告列表失败，page=" + pageIndex, e);
        }
    }

    public byte[] downloadAnnouncement(String attachmentUrl) {
        HttpUrl url = HttpUrl.parse(attachmentUrl);
        HttpUrl cdnUrl = HttpUrl.get(thFundDisclosureCdnAddress);
        if (url == null || !"https".equals(url.scheme()) || !cdnUrl.host().equals(url.host())
                || !url.encodedPath().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("天弘基金公告附件地址不合法");
        }
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; AQuant/1.0)")
                .get()
                .build();
        return execute(request);
    }

    THFundAnnouncementPage parseAnnouncementPage(byte[] response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        if (root.path("ErrCode").asInt(-1) != 0 || !root.path("Data").isArray()) {
            throw new IllegalStateException("基金公告披露接口返回失败，message=" + root.path("ErrMsg").asText());
        }
        THFundAnnouncementPage result = new THFundAnnouncementPage();
        int totalCount = root.path("TotalCount").asInt();
        int pageSize = root.path("PageSize").asInt(PAGE_SIZE);
        result.setTotalPages(Math.max(1, (totalCount + pageSize - 1) / pageSize));
        HttpUrl officialNoticeUrl = HttpUrl.get(thFundAddress).newBuilder()
                .addPathSegment("notice_list")
                .addQueryParameter("title", "天弘纳斯达克100")
                .build();
        for (JsonNode item : root.path("Data")) {
            String announcementId = item.path("ID").asText();
            String title = item.path("TITLE").asText();
            String publishDate = item.path("PUBLISHDATEDesc").asText();
            if (!announcementId.isBlank() && !title.isBlank() && publishDate.length() >= 10) {
                HttpUrl attachmentUrl = HttpUrl.get(thFundDisclosureCdnAddress).newBuilder()
                        .addPathSegment("pdf")
                        .addPathSegment("H2_" + announcementId + "_1.pdf")
                        .build();
                THFundAnnouncement announcement = new THFundAnnouncement();
                announcement.setAnnouncementId(announcementId);
                announcement.setAnnouncementDate(LocalDate.parse(publishDate.substring(0, 10)));
                announcement.setTitle(title);
                announcement.setDetailUrl(officialNoticeUrl.toString());
                announcement.setAttachmentUrl(attachmentUrl.toString());
                result.getContent().add(announcement);
            }
        }
        return result;
    }

    private byte[] execute(Request request) {
        try {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(500, 1201));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("天弘基金请求等待被中断", e);
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
                throw new IllegalStateException("天弘基金公告请求跳转到非配置域名");
            }
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("天弘基金公告请求失败，status=" + response.code());
            }
            if (body.contentLength() > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("天弘基金公告响应超过大小限制");
            }
            byte[] bytes = body.bytes();
            if (bytes.length > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("天弘基金公告响应超过大小限制");
            }
            return bytes;
        } catch (Exception e) {
            throw new IllegalStateException("天弘基金公告请求异常，url=" + request.url(), e);
        }
    }

}
