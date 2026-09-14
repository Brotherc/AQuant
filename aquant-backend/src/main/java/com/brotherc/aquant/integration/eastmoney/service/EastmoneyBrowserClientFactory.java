package com.brotherc.aquant.integration.eastmoney.service;

import com.github.zhkl0228.impersonator.ImpersonatorApi;
import com.github.zhkl0228.impersonator.ImpersonatorFactory;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;

/**
 * 东财专用浏览器客户端工厂：
 */
public final class EastmoneyBrowserClientFactory {

    private EastmoneyBrowserClientFactory() {
    }

    public static OkHttpClient create(OkHttpClient baseClient) {
        return create(baseClient, true);
    }

    /**
     * @param impersonate
     *
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
