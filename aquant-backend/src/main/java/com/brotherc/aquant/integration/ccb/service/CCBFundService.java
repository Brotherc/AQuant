package com.brotherc.aquant.integration.ccb.service;

import com.brotherc.aquant.integration.ccb.model.CCBFundAnnouncement;
import com.brotherc.aquant.integration.ccb.model.CCBFundAnnouncementDetail;
import com.brotherc.aquant.integration.ccb.model.CCBFundAnnouncementPage;
import com.brotherc.aquant.integration.ccb.model.CCBFundInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 建信基金官网公开接口
 */
@Service
@RequiredArgsConstructor
public class CCBFundService {

    private static final int MAX_JSON_SIZE = 5 * 1024 * 1024;
    private static final int MAX_HTML_SIZE = 1024 * 1024;
    private static final Pattern ATTACHMENT_PATTERN = Pattern.compile(
            "<a[^>]+href=[\"']([^\"']+\\.docx?)[\"']", Pattern.CASE_INSENSITIVE
    );

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    @Value("${ccbfund-address}")
    private String ccbFundAddress;

    /**
     * 获取建信官网“海外基金”分类下的纳斯达克100指数基金份额，用作官方额度同步的目标基金池。
     */
    public List<CCBFundInfo> getNasdaq100IndexFunds() {
        HttpUrl url = HttpUrl.get(ccbFundAddress).newBuilder().addPathSegments("website/v1/api/fundList").build();
        JsonNode data = getJson(url).path("data");

        List<CCBFundInfo> result = new ArrayList<>();
        if (!data.isArray()) {
            return result;
        }

        for (JsonNode category : data) {
            if ("海外基金".equals(category.path("name").asText())) {
                for (JsonNode fund : category.path("list")) {
                    String fundName = fund.path("fundName").asText();
                    String fundCode = fund.path("fundCode").asText();
                    if (StringUtils.isNotBlank(fundCode)
                            && StringUtils.contains(StringUtils.deleteWhitespace(fundName), "纳斯达克100")) {
                        CCBFundInfo item = new CCBFundInfo();
                        item.setFundCode(fundCode);
                        item.setFundName(StringUtils.trim(fundName));
                        result.add(item);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 分页查询指定基金的建信官网申购类公告，关键词使用“申购”以覆盖限额、暂停和恢复申购等公告。
     */
    public CCBFundAnnouncementPage getPurchaseLimitAnnouncements(
            String fundCode, LocalDate startDate, LocalDate endDate, int page
    ) {
        HttpUrl url = HttpUrl.get(ccbFundAddress).newBuilder()
                .addPathSegments("website/v1/api/fund/notice")
                .addQueryParameter("fundCode", fundCode)
                .addQueryParameter("categoryId", "876")
                // 不能只查“大额申购”，否则会漏掉解除限额的“恢复申购”公告。
                .addQueryParameter("keyword", "申购")
                .addQueryParameter("start", startDate == null ? "" : startDate.toString())
                // 建信接口只有同时提供开始和结束日期时才会应用日期过滤。
                .addQueryParameter("end", endDate == null ? "" : endDate.toString())
                .addQueryParameter("page", String.valueOf(page))
                .build();

        JsonNode data = getJson(url).path("data");
        CCBFundAnnouncementPage result = new CCBFundAnnouncementPage();
        result.setTotalPages(data.path("totalPages").asInt(0));
        for (JsonNode item : data.path("content")) {
            if (!item.hasNonNull("cntId")) {
                continue;
            }
            CCBFundAnnouncement announcement = new CCBFundAnnouncement();
            announcement.setAnnouncementId(item.path("cntId").asText());
            announcement.setTitle(item.path("title").asText());
            String date = item.path("date").asText();
            if (StringUtils.isNotBlank(date)) {
                announcement.setAnnouncementDate(LocalDate.parse(date));
            }
            result.getContent().add(announcement);
        }
        return result;
    }

    public CCBFundAnnouncementDetail getAnnouncementDetail(String announcementId) {
        HttpUrl detailUrl = HttpUrl.get(ccbFundAddress).newBuilder()
                .addPathSegments("resource/static/content/" + announcementId + ".html")
                .build();
        String html = new String(execute(detailUrl, MAX_HTML_SIZE), StandardCharsets.UTF_8);
        Matcher matcher = ATTACHMENT_PATTERN.matcher(html);
        if (!matcher.find()) {
            throw new IllegalStateException("公告未找到 Word 附件，announcementId=" + announcementId);
        }
        String attachmentPath = matcher.group(1).replace("&amp;", "&");
        HttpUrl attachmentUrl = HttpUrl.get(ccbFundAddress).resolve(attachmentPath);
        if (attachmentUrl == null) {
            throw new IllegalStateException("公告附件地址不合法，announcementId=" + announcementId);
        }
        HttpUrl officialBase = HttpUrl.get(ccbFundAddress);
        if (!officialBase.host().equals(attachmentUrl.host()) || !"https".equals(attachmentUrl.scheme())) {
            throw new IllegalArgumentException("仅允许访问配置的建信基金 HTTPS 域名");
        }

        CCBFundAnnouncementDetail detail = new CCBFundAnnouncementDetail();
        detail.setDetailUrl(detailUrl.toString());
        detail.setAttachmentUrl(attachmentUrl.toString());
        detail.setAttachmentName(attachmentUrl.pathSegments().get(attachmentUrl.pathSize() - 1));
        return detail;
    }

    private JsonNode getJson(HttpUrl url) {
        try {
            JsonNode response = objectMapper.readTree(execute(url, MAX_JSON_SIZE));
            if (response.path("errcode").asInt(-1) != 0) {
                throw new IllegalStateException("建信基金接口返回失败: " + response.path("msg").asText());
            }
            return response;
        } catch (IOException e) {
            throw new IllegalStateException("建信基金接口响应解析失败", e);
        }
    }

    public byte[] execute(HttpUrl url, int maxSize) {
        HttpUrl officialBase = HttpUrl.get(ccbFundAddress);
        try {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(500, 1201));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("建信基金请求等待被中断", e);
        }
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "AQuant/1.0")
                .get()
                .build();
        OkHttpClient client = okHttpClient.newBuilder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .build();
        try (Response response = client.newCall(request).execute()) {
            HttpUrl responseUrl = response.request().url();
            if (!officialBase.host().equals(responseUrl.host()) || !"https".equals(responseUrl.scheme())) {
                throw new IllegalArgumentException("仅允许访问配置的建信基金 HTTPS 域名");
            }
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("建信基金请求失败，status=" + response.code());
            }
            if (body.contentLength() > maxSize) {
                throw new IllegalStateException("建信基金响应超过大小限制");
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            InputStream inputStream = body.byteStream();
            int total = 0;
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                total += length;
                if (total > maxSize) {
                    throw new IllegalStateException("建信基金响应超过大小限制");
                }
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("建信基金请求异常", e);
        }
    }

}
