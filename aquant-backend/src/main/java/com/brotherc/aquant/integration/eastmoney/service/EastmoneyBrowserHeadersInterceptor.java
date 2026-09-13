package com.brotherc.aquant.integration.eastmoney.service;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 东财请求浏览器指纹模拟：完整 Chrome 请求头，降低被 WAF 按固定指纹识别的风险。
 * UA 与平台固定为 macOS Chrome 152，须与 EastmoneyBrowserClientFactory 的 MacChrome TLS 指纹预设
 * 保持一致（TLS 层暴露操作系统特征，UA 声称 Windows 而指纹是 macOS 会形成矛盾信号）。
 * sec-ch-ua 的 "Not A Brand" 品牌/顺序 Chrome 每次安装随机化，此处按真实分布做小池轮换。
 * quote.eastmoney.com 首次访问会触发滑块验证，通过后浏览器持有的会话 Cookie 是 push2 接口的
 * 通行证：可把通过验证的完整 Cookie 配置进来，让 Java 客户端复用该会话。
 */
public class EastmoneyBrowserHeadersInterceptor implements Interceptor {

    /** 与 ImpersonatorFactory.macChrome() 预设（Chrome 152）一致 */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36";
    private static final String PLATFORM = "\"macOS\"";

    /** Chrome 各安装随机的品牌列表变体（版本号一致，仅 GREASE 品牌与顺序不同） */
    private static final List<String> BRAND_POOL = List.of(
            "\"Chromium\";v=\"152\", \"Not?A_Brand\";v=\"24\", \"Google Chrome\";v=\"152\"",
            "\"Google Chrome\";v=\"152\", \"Not)A(Brand\";v=\"99\", \"Chromium\";v=\"152\"",
            "\"Chromium\";v=\"152\", \"Not;A=Brand\";v=\"8\", \"Google Chrome\";v=\"152\""
    );

    /** 会话 Cookie 来源（每次请求实时取值，支持运行期热更新），可为空 */
    private final java.util.function.Supplier<String> cookieSource;

    public EastmoneyBrowserHeadersInterceptor() {
        this(() -> null);
    }

    public EastmoneyBrowserHeadersInterceptor(String configuredCookie) {
        this(() -> configuredCookie);
    }

    public EastmoneyBrowserHeadersInterceptor(java.util.function.Supplier<String> cookieSource) {
        this.cookieSource = cookieSource;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String brands = BRAND_POOL.get(ThreadLocalRandom.current().nextInt(BRAND_POOL.size()));
        Request.Builder builder = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("sec-ch-ua", brands)
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", PLATFORM)
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-site")
                .header("Referer", "https://quote.eastmoney.com/");
        String cookie = cookieSource.get();
        if (cookie != null && !cookie.isBlank()) {
            builder.header("Cookie", cookie.trim());
        }
        return chain.proceed(builder.build());
    }
}
