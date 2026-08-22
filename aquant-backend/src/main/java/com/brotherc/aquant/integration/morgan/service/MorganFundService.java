package com.brotherc.aquant.integration.morgan.service;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitAnnouncement;
import com.brotherc.aquant.integration.morgan.model.MorganFundAnnouncementPage;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 摩根基金官网公开公告接口。
 */
@Service
@RequiredArgsConstructor
public class MorganFundService {

    private static final int MAX_RESPONSE_SIZE = 15 * 1024 * 1024;
    private static final DateTimeFormatter ANNOUNCEMENT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter ANNOUNCEMENT_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final OkHttpClient okHttpClient;

    @Value("${morganfund-address}")
    private String morganFundAddress;

    /**
     * 查询摩根纳斯达克100指数基金公告。官网首页使用无后缀 XML，后续页使用 _页码 后缀。
     */
    public MorganFundAnnouncementPage getNasdaq100Announcements(int page) {
        String fileName = page <= 1 ? "data_2723.xml" : "data_2723_" + page + ".xml";
        HttpUrl url = HttpUrl.get(morganFundAddress).newBuilder()
                .addPathSegments("fund/019172/announce")
                .addPathSegment(fileName)
                .build();
        byte[] response = execute(url);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(new InputSource(
                    new StringReader(new String(response, StandardCharsets.UTF_8))
            ));
            MorganFundAnnouncementPage result = new MorganFundAnnouncementPage();
            result.setTotalPages(Integer.parseInt(document.getElementsByTagName("pageCount").item(0).getTextContent()));
            NodeList items = document.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String attachmentUrl = item.getAttribute("url");
                FundPurchaseLimitAnnouncement announcement = new FundPurchaseLimitAnnouncement();
                String attachmentName = StringUtils.substringAfterLast(attachmentUrl, "/");
                announcement.setAnnouncementId(StringUtils.substringBeforeLast(attachmentName, "."));
                announcement.setAnnouncementDate(LocalDate.parse(item.getAttribute("time"),
                        ANNOUNCEMENT_DATE_FORMATTER));
                announcement.setTitle(item.getAttribute("title"));
                result.getContent().add(announcement);
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("摩根基金公告列表解析失败，page=" + page, e);
        }
    }

    public byte[] downloadAnnouncement(String announcementUrl) {
        HttpUrl url = HttpUrl.parse(announcementUrl);
        if (url == null || !"https".equals(url.scheme())
                || !HttpUrl.get(morganFundAddress).host().equals(url.host())
                || !url.encodedPath().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("摩根基金公告附件地址不合法");
        }
        return execute(url);
    }

    public String getAnnouncementUrl(String announcementId, LocalDate announcementDate) {
        return HttpUrl.get(morganFundAddress).newBuilder()
                .addPathSegments("fund/019172/announce")
                .addPathSegment(announcementDate.format(ANNOUNCEMENT_MONTH_FORMATTER))
                .addPathSegment(announcementId + ".pdf")
                .build().toString();
    }

    private byte[] execute(HttpUrl url) {
        Request request = new Request.Builder().url(url).header("User-Agent", "AQuant/1.0").get().build();
        OkHttpClient client = okHttpClient.newBuilder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(25, TimeUnit.SECONDS)
                .build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("摩根基金官网请求失败，status=" + response.code());
            }
            if (body.contentLength() > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("摩根基金官网响应超过大小限制");
            }
            byte[] bytes = body.bytes();
            if (bytes.length > MAX_RESPONSE_SIZE) {
                throw new IllegalStateException("摩根基金官网响应超过大小限制");
            }
            return bytes;
        } catch (Exception e) {
            throw new IllegalStateException("摩根基金官网请求异常，url=" + url, e);
        }
    }

}
