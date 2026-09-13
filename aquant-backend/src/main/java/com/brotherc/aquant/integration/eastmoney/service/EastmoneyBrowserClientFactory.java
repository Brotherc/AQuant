package com.brotherc.aquant.integration.eastmoney.service;

import com.github.zhkl0228.impersonator.ImpersonatorApi;
import com.github.zhkl0228.impersonator.ImpersonatorFactory;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;

/**
 * 东财专用浏览器客户端工厂：BCJSSE 模拟 macOS Chrome 的 TLS/JA3/JA4 指纹。
 * 东财 WAF 通过 TLS 指纹识别非浏览器客户端，JDK 默认 JSSE 的握手特征（密码套件顺序、扩展顺序、
 * GREASE 缺失）与 Chrome 差异显著，仅伪造请求头不足以伪装。
 * 注意：仅模拟 TLS 指纹（impersonator-bctls）；HTTP/2 指纹需要完整替换 okhttp（impersonator-okhttp
 * 为 okhttp 5.5 fork，与项目 okhttp 4.11.0 同包名冲突），权衡后不引入。
 */
public final class EastmoneyBrowserClientFactory {

    private EastmoneyBrowserClientFactory() {
    }

    public static OkHttpClient create(OkHttpClient baseClient) {
        return create(baseClient, true);
    }

    /**
     * @param impersonate 是否启用 TLS 指纹模拟。关闭后回落到 OkHttp/JDK 默认 SSLContext，
     *                    用于定位 WAF 拦截是否由 TLS 指纹（尤其是 GREASE ECH 扩展）触发：
     *                    部分 CDN/中间盒对未识别的 ECH 扩展会直接断连，表现为空响应。
     *                    2026-09-10 实测：开启与关闭在 push2 / push2his 上失败形态一致，
     *                    故 TLS 指纹不是当前故障主因，此开关保留作排查手段。
     * @param cookie      会话 Cookie（完整 Cookie 请求头字符串），可为空。quote.eastmoney.com
     *                    滑块验证通过后浏览器持有的 Cookie 是 push2 接口的通行证
     */
    public static OkHttpClient create(OkHttpClient baseClient, boolean impersonate) {
        return create(baseClient, impersonate, () -> null);
    }

    public static OkHttpClient create(OkHttpClient baseClient, boolean impersonate, String cookie) {
        return create(baseClient, impersonate, () -> cookie);
    }

    /**
     * @param cookieSource 会话 Cookie 来源，每次请求实时取值（支持运行期热更新），可为空
     */
    public static OkHttpClient create(OkHttpClient baseClient, boolean impersonate,
                                      java.util.function.Supplier<String> cookieSource) {
        OkHttpClient.Builder builder = baseClient.newBuilder()
                .addInterceptor(new EastmoneyBrowserHeadersInterceptor(cookieSource));
        if (!impersonate) {
            return builder.build();
        }
        ImpersonatorApi api = ImpersonatorFactory.macChrome();
        // 关闭 ECH 的 DNS-over-HTTPS 查询（默认 resolver 1.1.1.1 国内不可达，首次建连会等待超时），
        // 保留 GREASE ECH 扩展，与真实 Chrome 无 ECHConfig 时的行为一致
        api.setEchConfigProvider(null);
        SSLContext sslContext = api.newSSLContext(null, null);
        return builder
                .sslSocketFactory(sslContext.getSocketFactory(), ImpersonatorFactory.DEFAULT_TRUST_MANAGER)
                .build();
    }
}
