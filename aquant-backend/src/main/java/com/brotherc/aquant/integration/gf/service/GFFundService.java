package com.brotherc.aquant.integration.gf.service;

import com.brotherc.aquant.integration.gf.model.GFFundPurchaseLimit;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 广发基金官网公开接口。
 */
@Service
@RequiredArgsConstructor
public class GFFundService {

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    @Value("${gffund-address}")
    private String gfFundAddress;

    /**
     * 查询广发基金官网产品页展示的个人客户当前申购上下限。
     */
    public GFFundPurchaseLimit getPersonalPurchaseLimit(String fundCode) {
        HttpUrl url = HttpUrl.get(gfFundAddress).newBuilder()
                .addPathSegments("api/v1/funds/fund-person-limit.shtml")
                .addQueryParameter("fundcode", fundCode)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "AQuant/1.0")
                .get()
                .build();
        OkHttpClient client = okHttpClient.newBuilder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("广发基金申购额度请求失败，fundCode=" + fundCode
                        + ", status=" + response.code());
            }
            JsonNode json = objectMapper.readTree(body.bytes());
            if (json.hasNonNull("ERROR")) {
                throw new IllegalStateException("广发基金申购额度接口返回失败，fundCode=" + fundCode
                        + ", message=" + json.path("ERROR").asText());
            }
            String responseFundCode = json.path("FUNDCODE").asText();
            if (!fundCode.equals(responseFundCode)) {
                throw new IllegalStateException("广发基金申购额度响应基金代码不匹配，fundCode=" + fundCode
                        + ", responseFundCode=" + responseFundCode);
            }

            GFFundPurchaseLimit result = new GFFundPurchaseLimit();
            result.setFundCode(responseFundCode);
            String maximum = json.path("MAX_ALLOT_BALA").asText();
            String minimum = json.path("MIN_ALLOT_BALA").asText();
            if (StringUtils.isNotBlank(maximum)) {
                result.setMaximumPurchaseAmount(new BigDecimal(maximum));
            }
            if (StringUtils.isNotBlank(minimum)) {
                result.setMinimumPurchaseAmount(new BigDecimal(minimum));
            }
            return result;
        } catch (IOException | NumberFormatException e) {
            throw new IllegalStateException("广发基金申购额度响应解析失败，fundCode=" + fundCode, e);
        }
    }

}
